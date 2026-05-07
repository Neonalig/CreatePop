package org.neonalig.createpop.soda;

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.neonalig.createpop.component.BrewersNotebookData;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.registry.ModFluids;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class BrewingDiscoveryManager {
    private static final String PLAYER_RECIPES_TAG = "createpop_brewers_recipes";
    private static final String NOTEBOOK_RECIPES_TAG = "createpop_notebook_recipes";
    private static final String ENTRY_KEY = "key";
    private static final String ENTRY_DATA = "data";

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

    public static boolean learnFromStack(Player player, ItemStack stack) {
        if (stack.is(ModFluids.SODA_BOTTLE.get()) || stack.is(ModFluids.SODA_BUCKET.get())) {
            return learn(player, SodaFluidStackHelper.getSodaData(stack));
        }
        return false;
    }

    public static boolean learnFromFluid(Player player, FluidStack stack) {
        if (!SodaFluidStackHelper.isSoda(stack) || stack.getAmount() <= 0) {
            return false;
        }
        return learn(player, SodaFluidStackHelper.getSodaData(stack));
    }

    public static int learnFromBlock(Player player, Level level, BlockPos pos, @Nullable Direction side) {
        int learned = 0;

        var fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        if (fluidHandler == null && side != null) {
            fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        }
        if (fluidHandler != null) {
            for (int i = 0; i < fluidHandler.getTanks(); i++) {
                if (learnFromFluid(player, fluidHandler.getFluidInTank(i))) {
                    learned++;
                }
            }
        }

        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (itemHandler == null && side != null) {
            itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        }
        if (itemHandler != null) {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                if (learnFromStack(player, itemHandler.getStackInSlot(slot))) {
                    learned++;
                }
            }
        }

        return learned;
    }

    public static boolean learn(Player player, SodaData data) {
        if (data.equals(SodaData.EMPTY)) {
            return false;
        }

        BrewersNotebookData known = playerData(player);
        String key = BrewersNotebookData.keyFor(data);
        if (known.asMap().containsKey(key)) {
            return false;
        }

        setPlayerData(player, known.withEntry(data));
        return true;
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

    public static void writePlayerRecipesToNotebook(Player player, ItemStack notebook) {
        setNotebookData(notebook, playerData(player));
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

    private static BrewersNotebookData playerData(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(PLAYER_RECIPES_TAG, Tag.TAG_LIST)) {
            return BrewersNotebookData.EMPTY;
        }

        ListTag list = root.getList(PLAYER_RECIPES_TAG, Tag.TAG_COMPOUND);
        return readData(list);
    }

    private static BrewersNotebookData readData(ListTag list) {
        Map<String, SodaData> byKey = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String key = entry.getString(ENTRY_KEY);
            if (key.isEmpty() || !entry.contains(ENTRY_DATA, Tag.TAG_COMPOUND)) {
                continue;
            }
            DataResult<SodaData> parsed = SodaData.CODEC.parse(NbtOps.INSTANCE, entry.getCompound(ENTRY_DATA));
            parsed.result().ifPresent(data -> byKey.put(key, data));
        }
        return BrewersNotebookData.fromMap(byKey);
    }

    private static void setPlayerData(Player player, BrewersNotebookData data) {
        player.getPersistentData().put(PLAYER_RECIPES_TAG, writeData(data));
    }

    private static ListTag writeData(BrewersNotebookData data) {
        ListTag list = new ListTag();
        for (BrewersNotebookData.Entry entry : data.entries()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(ENTRY_KEY, entry.key());
            SodaData.CODEC.encodeStart(NbtOps.INSTANCE, entry.data()).result().ifPresent(encoded -> {
                if (encoded instanceof CompoundTag compound) {
                    entryTag.put(ENTRY_DATA, compound);
                }
            });
            list.add(entryTag);
        }
        return list;
    }
}

