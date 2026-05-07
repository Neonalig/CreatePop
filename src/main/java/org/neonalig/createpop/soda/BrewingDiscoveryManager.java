package org.neonalig.createpop.soda;

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.neonalig.createpop.CreatePopConfig;
import org.neonalig.createpop.advancement.CreatePopAdvancementGrants;
import org.neonalig.createpop.advancement.ModTriggers;
import org.neonalig.createpop.network.OpenSodaNamePromptPayload;
import org.neonalig.createpop.component.BrewersNotebookData;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.compat.create.DynamicSodaMixing;
import org.neonalig.createpop.registry.ModItems;
import org.neonalig.createpop.registry.ModFluids;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BrewingDiscoveryManager {
    private static final String PLAYER_RECIPES_TAG = "createpop_brewers_recipes";
    private static final String NOTEBOOK_RECIPES_TAG = "createpop_notebook_recipes";
    private static final String ENTRY_KEY = "key";
    private static final String ENTRY_DATA = "data";
    private static final String ENTRY_NAME = "name";
    private static final String ENTRY_INGREDIENTS = "ingredients";
    private static final String ENTRY_NOTE = "note";

    private BrewingDiscoveryManager() {
    }

    public static BrewersNotebookData notebookData(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(NOTEBOOK_RECIPES_TAG, Tag.TAG_LIST)) {
            return BrewersNotebookData.EMPTY;
        }
        return readData(root.getList(NOTEBOOK_RECIPES_TAG, Tag.TAG_COMPOUND));
    }

    public static void setNotebookData(ItemStack stack, BrewersNotebookData data) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(NOTEBOOK_RECIPES_TAG, writeData(data)));
    }

    public static boolean notebookHasEntries(ItemStack stack) {
        return !notebookData(stack).isEmpty();
    }

    public static int notebookEntryCount(ItemStack stack) {
        return notebookData(stack).size();
    }

    public static void learnFromStack(Player player, ItemStack stack) {
        if (stack.is(ModFluids.SODA_BOTTLE.get()) || stack.is(ModFluids.SODA_BUCKET.get())) {
            syncKnownNameOnStack(player, stack);
            learn(player, SodaFluidStackHelper.getSodaData(stack), List.of());
        }
    }

    public static void syncKnownNamesInInventory(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            syncKnownNameOnStack(player, stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            syncKnownNameOnStack(player, stack);
        }
    }

    public static List<LearnedBlockRecipe> learnResultsFromBlock(Player player, Level level, BlockPos pos, @Nullable Direction side) {
        java.util.LinkedHashMap<String, ScanCandidate> candidates = new java.util.LinkedHashMap<>();

        var fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        if (fluidHandler == null && side != null) {
            fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        }
        if (fluidHandler != null) {
            for (int i = 0; i < fluidHandler.getTanks(); i++) {
                collectScanCandidate(candidates, SodaFluidStackHelper.getSodaData(fluidHandler.getFluidInTank(i)), fluidHandler.getFluidInTank(i).getAmount(), true);
            }
        }

        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (itemHandler == null && side != null) {
            itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        }
        if (itemHandler != null) {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!stack.is(ModFluids.SODA_BOTTLE.get()) && !stack.is(ModFluids.SODA_BUCKET.get())) {
                    continue;
                }
                collectScanCandidate(candidates, SodaFluidStackHelper.getSodaData(stack), stack.getCount(), false);
            }
        }

        java.util.ArrayList<LearnedBlockRecipe> learned = new java.util.ArrayList<>();
        ScanCandidate candidate = chooseScanCandidate(candidates);
        if (candidate == null) {
            return List.of();
        }

        LearnResult result = learnDetailed(player, candidate.data(), List.of());
        if (result.learned()) {
            learned.add(new LearnedBlockRecipe(candidate.data(), result.name()));
        }
        return List.copyOf(learned);
    }

    public static void learn(Player player, SodaData data, List<String> ingredients) {
        learnDetailed(player, data, ingredients);
    }

    public static LearnResult learnDetailed(Player player, SodaData data, List<String> ingredients) {
        if (data.equals(SodaData.EMPTY)) {
            return LearnResult.none();
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return LearnResult.none();
        }

        BrewersNotebookData known = playerData(serverPlayer);
        String key = BrewersNotebookData.keyFor(data);
        if (known.containsKey(key)) {
            String existingName = nameForEntry(key, known, serverPlayer.serverLevel());
            return new LearnResult(false, existingName);
        }

        SodaNameRegistrySavedData names = SodaNameRegistrySavedData.get(serverPlayer.serverLevel());
        String potionBaseLabel = singlePotionBaseLabel(data);
        String knownName = potionBaseLabel != null ? potionBaseLabel : names.getName(key);
        boolean brandNew = potionBaseLabel == null && knownName.isBlank();
        if (brandNew) {
            knownName = SodaNameGenerator.randomName(serverPlayer.getRandom());
            names.putName(key, knownName);
        }

        List<String> normalizedIngredients = ingredients.isEmpty() ? List.of(
                "Carbonated Water",
                "Potion/Soda reactants"
        ) : ingredients;
        if (normalizedIngredients.size() == 2
                && "Carbonated Water".equals(normalizedIngredients.get(0))
                && "Potion/Soda reactants".equals(normalizedIngredients.get(1))) {
            normalizedIngredients = inferLikelyIngredients(data, serverPlayer.serverLevel());
        }
        setPlayerData(serverPlayer, known.withEntry(data, knownName, normalizedIngredients));

        if (brandNew) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenSodaNamePromptPayload(key, knownName));
        }

        return new LearnResult(true, knownName);
    }

    public static int learnFromNotebook(Player player, ItemStack notebook) {
        BrewersNotebookData fromBook = notebookData(notebook);
        if (fromBook.isEmpty()) {
            return 0;
        }

        BrewersNotebookData known = playerData(player);
        int before = known.size();
        BrewersNotebookData merged = known.merge(fromBook);
        setPlayerData(player, merged);
        return merged.size() - before;
    }

    public static int writePlayerRecipesToNotebook(Player player, ItemStack notebook) {
        BrewersNotebookData existing = notebookData(notebook);
        BrewersNotebookData merged = existing.merge(playerData(player));
        int added = merged.size() - existing.size();
        setNotebookData(notebook, merged);
        return added;
    }

    public static void addLearnedRecipeToNotebook(Player player, ItemStack notebook, SodaData data) {
        BrewersNotebookData existing = notebookData(notebook);
        String key = BrewersNotebookData.keyFor(data);
        if (existing.containsKey(key)) {
            return;
        }

        BrewersNotebookData.Entry playerEntry = knownEntry(player, data);
        BrewersNotebookData updated = existing.withEntry(data, playerEntry.name(), playerEntry.ingredients());
        setNotebookData(notebook, updated);
    }

    public static void removeNotebookEntry(ItemStack notebook, String key) {
        BrewersNotebookData data = notebookData(notebook);
        setNotebookData(notebook, data.withoutEntry(key));
    }

    public static void updateNotebookEntryNote(ItemStack notebook, String key, String note) {
        BrewersNotebookData data = notebookData(notebook);
        BrewersNotebookData.Entry entry = data.entryMap().get(key);
        if (entry != null) {
            Map<String, BrewersNotebookData.Entry> updated = new LinkedHashMap<>(data.entryMap());
            updated.put(key, new BrewersNotebookData.Entry(entry.key(), entry.data(), entry.name(), entry.ingredients(), note));
            setNotebookData(notebook, BrewersNotebookData.fromEntryMap(updated));
        }
    }

    public static void mergeNotebookStacks(ItemStack output, ItemStack first, ItemStack second) {
        BrewersNotebookData merged = notebookData(first).merge(notebookData(second));
        setNotebookData(output, merged);
    }

    public static void copyNotebookToOutput(ItemStack output, ItemStack source) {
        setNotebookData(output, notebookData(source));
    }

    public static Set<String> knownRecipeKeys(Player player) {
        return playerData(player).asMap().keySet();
    }

    public static int unlockAllJeiHintRecipes(ServerPlayer player) {
        BrewersNotebookData known = playerData(player);
        Map<String, BrewersNotebookData.Entry> updated = new LinkedHashMap<>(known.entryMap());

        List<PotionBase> bases = collectPotionBases();
        long seed = player.serverLevel().getSeed();

        for (int i = 0; i < bases.size(); i++) {
            PotionBase first = bases.get(i);
            for (int j = i + 1; j < bases.size(); j++) {
                PotionBase second = bases.get(j);
                SodaData mixed = SodaEffectReducer.mix(
                        first.data(),
                        second.data(),
                        DynamicSodaMixing.DRINK_AMOUNT,
                        DynamicSodaMixing.DRINK_AMOUNT,
                        seed
                );
                String key = BrewersNotebookData.keyFor(mixed);
                if (updated.containsKey(key)) {
                    continue;
                }

                String name = "Hint: " + first.label() + " + " + second.label();
                List<String> ingredients = List.of(
                        "Input Soda: " + first.label(),
                        "Input Soda/Potion: " + second.label()
                );
                updated.put(key, new BrewersNotebookData.Entry(key, mixed, name, ingredients));
            }
        }

        int added = updated.size() - known.size();
        if (added > 0) {
            setPlayerData(player, BrewersNotebookData.fromEntryMap(updated));
        }
        return added;
    }

    public static void autoUnlockJeiHintsIfEnabled(ServerPlayer player) {
        if (!CreatePopConfig.autoUnlockAllJeiHints()) {
            return;
        }
        unlockAllJeiHintRecipes(player);
    }

    public static BrewersNotebookData.Entry knownEntry(Player player, SodaData data) {
        return playerData(player).entryMap().get(BrewersNotebookData.keyFor(data));
    }

    public static void renameRecipe(ServerPlayer player, String key, String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        SodaNameRegistrySavedData.get(player.serverLevel()).putName(key, trimmed);
        BrewersNotebookData known = playerData(player);
        BrewersNotebookData.Entry existing = known.entryMap().get(key);
        if (existing != null) {
            Map<String, BrewersNotebookData.Entry> updated = new LinkedHashMap<>(known.entryMap());
            updated.put(key, new BrewersNotebookData.Entry(existing.key(), existing.data(), trimmed, existing.ingredients(), existing.note()));
            setPlayerData(player, BrewersNotebookData.fromEntryMap(updated));
        }

        updateInventoryNotebooksForKey(player, key, trimmed);
        updateInventorySodaStacksForKey(player, key, trimmed);
        CreatePopAdvancementGrants.grantNamedSoda(player);
        ModTriggers.NAMED_SODA.trigger(player);
    }

    public static BrewersNotebookData playerData(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(PLAYER_RECIPES_TAG, Tag.TAG_LIST)) {
            return BrewersNotebookData.EMPTY;
        }

        ListTag list = root.getList(PLAYER_RECIPES_TAG, Tag.TAG_COMPOUND);
        return readData(list);
    }

    private static BrewersNotebookData readData(ListTag list) {
        Map<String, BrewersNotebookData.Entry> byKey = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String key = entry.getString(ENTRY_KEY);
            if (key.isEmpty() || !entry.contains(ENTRY_DATA, Tag.TAG_COMPOUND)) {
                continue;
            }
            DataResult<SodaData> parsed = SodaData.CODEC.parse(NbtOps.INSTANCE, entry.getCompound(ENTRY_DATA));
            List<String> ingredients = List.of();
            if (entry.contains(ENTRY_INGREDIENTS, Tag.TAG_LIST)) {
                ListTag ingredientList = entry.getList(ENTRY_INGREDIENTS, Tag.TAG_STRING);
                java.util.ArrayList<String> loaded = new java.util.ArrayList<>();
                for (int index = 0; index < ingredientList.size(); index++) {
                    loaded.add(ingredientList.getString(index));
                }
                ingredients = List.copyOf(loaded);
            }
            final String finalName = entry.contains(ENTRY_NAME, Tag.TAG_STRING) ? entry.getString(ENTRY_NAME) : "Unnamed Soda";
            final String finalNote = entry.contains(ENTRY_NOTE, Tag.TAG_STRING) ? entry.getString(ENTRY_NOTE) : "";
            final List<String> finalIngredients = ingredients;
            parsed.result().ifPresent(data -> byKey.put(key, new BrewersNotebookData.Entry(key, data, finalName, finalIngredients, finalNote)));
        }
        return BrewersNotebookData.fromEntryMap(byKey);
    }

    private static void setPlayerData(Player player, BrewersNotebookData data) {
        player.getPersistentData().put(PLAYER_RECIPES_TAG, writeData(data));
    }

    private static ListTag writeData(BrewersNotebookData data) {
        ListTag list = new ListTag();
        for (BrewersNotebookData.Entry entry : data.entries()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(ENTRY_KEY, entry.key());
            entryTag.putString(ENTRY_NAME, entry.name());
            entryTag.putString(ENTRY_NOTE, entry.note());
            SodaData.CODEC.encodeStart(NbtOps.INSTANCE, entry.data()).result().ifPresent(encoded -> {
                if (encoded instanceof CompoundTag compound) {
                    entryTag.put(ENTRY_DATA, compound);
                }
            });
            ListTag ingredients = new ListTag();
            for (String ingredient : entry.ingredients()) {
                ingredients.add(net.minecraft.nbt.StringTag.valueOf(ingredient));
            }
            entryTag.put(ENTRY_INGREDIENTS, ingredients);
            list.add(entryTag);
        }
        return list;
    }

    private static String nameForEntry(String key, BrewersNotebookData known, net.minecraft.server.level.ServerLevel level) {
        BrewersNotebookData.Entry entry = known.entryMap().get(key);
        if (entry != null && !entry.name().isBlank()) {
            return entry.name();
        }
        String stored = SodaNameRegistrySavedData.get(level).getName(key);
        return stored.isBlank() ? "Unnamed Soda" : stored;
    }

    private static void updateInventoryNotebooksForKey(ServerPlayer player, String key, String name) {
        for (ItemStack stack : player.getInventory().items) {
            renameNotebookEntry(stack, key, name);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            renameNotebookEntry(stack, key, name);
        }
    }

    private static void updateInventorySodaStacksForKey(ServerPlayer player, String key, String name) {
        for (ItemStack stack : player.getInventory().items) {
            renameSodaStack(stack, key, name);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            renameSodaStack(stack, key, name);
        }
    }

    private static void renameSodaStack(ItemStack stack, String key, String name) {
        if (!stack.is(ModFluids.SODA_BOTTLE.get()) && !stack.is(ModFluids.SODA_BUCKET.get())) {
            return;
        }

        SodaData data = SodaFluidStackHelper.getSodaData(stack);
        if (!BrewersNotebookData.keyFor(data).equals(key)) {
            return;
        }

        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(style -> style.withItalic(false)));
    }

    private static void syncKnownNameOnStack(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!stack.is(ModFluids.SODA_BOTTLE.get()) && !stack.is(ModFluids.SODA_BUCKET.get())) {
            return;
        }

        SodaData data = SodaFluidStackHelper.getSodaData(stack);
        if (data.equals(SodaData.EMPTY)) {
            return;
        }

        String key = BrewersNotebookData.keyFor(data);
        String knownName = SodaNameRegistrySavedData.get(serverPlayer.serverLevel()).getName(key);
        if (knownName.isBlank()) {
            return;
        }

        stack.set(DataComponents.CUSTOM_NAME, Component.literal(knownName).withStyle(style -> style.withItalic(false)));
    }

    private static void renameNotebookEntry(ItemStack stack, String key, String name) {
        if (!stack.is(ModItems.BREWERS_NOTEBOOK.get())) {
            return;
        }
        BrewersNotebookData data = notebookData(stack);
        BrewersNotebookData.Entry entry = data.entryMap().get(key);
        if (entry == null) {
            return;
        }
        Map<String, BrewersNotebookData.Entry> updated = new LinkedHashMap<>(data.entryMap());
        updated.put(key, new BrewersNotebookData.Entry(entry.key(), entry.data(), name, entry.ingredients(), entry.note()));
        setNotebookData(stack, BrewersNotebookData.fromEntryMap(updated));
    }

    private static List<String> inferLikelyIngredients(SodaData data, net.minecraft.server.level.ServerLevel level) {
        List<PotionBase> bases = collectPotionBases();
        SodaNameRegistrySavedData names = SodaNameRegistrySavedData.get(level);
        String targetKey = BrewersNotebookData.keyFor(data);

        for (PotionBase base : bases) {
            if (base.key().equals(targetKey)) {
                return List.of("Carbonated Water", "Potion: " + base.label());
            }
        }

        long seed = level.getSeed();
        for (int i = 0; i < bases.size(); i++) {
            PotionBase first = bases.get(i);
            for (int j = i + 1; j < bases.size(); j++) {
                PotionBase second = bases.get(j);
                SodaData mixed = SodaEffectReducer.mix(
                        first.data(),
                        second.data(),
                        DynamicSodaMixing.DRINK_AMOUNT,
                        DynamicSodaMixing.DRINK_AMOUNT,
                        seed
                );
                if (!BrewersNotebookData.keyFor(mixed).equals(targetKey)) {
                    continue;
                }

                String firstName = names.getName(first.key());
                String secondName = names.getName(second.key());
                String firstLabel = firstName.isBlank() ? first.label() : firstName;
                String secondLabel = secondName.isBlank() ? second.label() : secondName;

                return List.of(
                        "Input Soda: " + firstLabel,
                        "Input Soda/Potion: " + secondLabel
                );
            }
        }

        return List.of("Carbonated Water", "Potion/Soda reactants");
    }

    @Nullable
    private static ScanCandidate chooseScanCandidate(Map<String, ScanCandidate> candidates) {
        ScanCandidate best = null;
        for (ScanCandidate candidate : candidates.values()) {
            if (best == null || compareScanCandidates(candidate, best) > 0) {
                best = candidate;
            }
        }
        return best;
    }

    private static int compareScanCandidates(ScanCandidate first, ScanCandidate second) {
        int compare = Boolean.compare(isSinglePotionBase(second.data()), isSinglePotionBase(first.data()));
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(first.data().effects().size(), second.data().effects().size());
        if (compare != 0) {
            return compare;
        }

        compare = Boolean.compare(first.fromFluid(), second.fromFluid());
        if (compare != 0) {
            return compare;
        }

        return Integer.compare(first.amount(), second.amount());
    }

    private static void collectScanCandidate(Map<String, ScanCandidate> candidates, SodaData data, int amount, boolean fromFluid) {
        if (data.equals(SodaData.EMPTY)) {
            return;
        }

        String key = BrewersNotebookData.keyFor(data);
        ScanCandidate incoming = new ScanCandidate(data, amount, fromFluid);
        ScanCandidate existing = candidates.get(key);
        if (existing == null || compareScanCandidates(incoming, existing) > 0) {
            candidates.put(key, incoming);
        }
    }

    private static boolean isSinglePotionBase(SodaData data) {
        return singlePotionBaseLabel(data) != null;
    }

    @Nullable
    private static String singlePotionBaseLabel(SodaData data) {
        String key = BrewersNotebookData.keyFor(data);
        for (PotionBase base : collectPotionBases()) {
            if (base.key().equals(key)) {
                return base.label();
            }
        }
        return null;
    }

    private static List<PotionBase> collectPotionBases() {
        java.util.ArrayList<PotionBase> bases = new java.util.ArrayList<>();
        for (Potion potion : BuiltInRegistries.POTION) {
            net.minecraft.resources.ResourceLocation id = BuiltInRegistries.POTION.getKey(potion);
            if (id == null || "empty".equals(id.getPath())) {
                continue;
            }

            ItemStack potionItem = PotionContents.createItemStack(Items.POTION, BuiltInRegistries.POTION.wrapAsHolder(potion));
            PotionContents contents = potionItem.get(DataComponents.POTION_CONTENTS);
            if (contents == null || !contents.hasEffects()) {
                continue;
            }

            java.util.List<MobEffectInstance> baseEffects = SodaEffectReducer.acceptedPotionEffects(contents);
            if (baseEffects.isEmpty()) {
                continue;
            }

            SodaData base = SodaEffectReducer.baseFromPotion(baseEffects, contents.getColor());
            bases.add(new PotionBase(id.getPath().replace('_', ' '), base, BrewersNotebookData.keyFor(base)));
        }
        return List.copyOf(bases);
    }

    private record PotionBase(String label, SodaData data, String key) {
    }

    private record ScanCandidate(SodaData data, int amount, boolean fromFluid) {
    }

    public record LearnedBlockRecipe(SodaData data, String name) {
    }

    public record LearnResult(boolean learned, String name) {
        public static LearnResult none() {
            return new LearnResult(false, "Unnamed Soda");
        }
    }
}

