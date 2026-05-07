package org.neonalig.createpop.event;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.neonalig.createpop.advancement.CreatePopAdvancementGrants;
import org.neonalig.createpop.advancement.ModTriggers;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.registry.ModDataComponents;
import org.neonalig.createpop.registry.ModFluids;
import org.neonalig.createpop.registry.ModItems;
import org.neonalig.createpop.soda.BrewingDiscoveryManager;
import org.neonalig.createpop.soda.SodaEffectReducer;
import org.neonalig.createpop.soda.SodaFluidStackHelper;

public final class ModCommonEvents {
    private ModCommonEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ModCommonEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ModCommonEvents::onItemPickup);
        NeoForge.EVENT_BUS.addListener(ModCommonEvents::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(ModCommonEvents::onRightClickBlock);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer) || serverPlayer.tickCount % 40 != 0) {
            return;
        }

        BrewingDiscoveryManager.autoUnlockJeiHintsIfEnabled(serverPlayer);

        for (ItemStack stack : serverPlayer.getInventory().items) {
            BrewingDiscoveryManager.learnFromStack(serverPlayer, stack);
        }
        for (ItemStack stack : serverPlayer.getInventory().offhand) {
            BrewingDiscoveryManager.learnFromStack(serverPlayer, stack);
        }

        CreatePopAdvancementGrants.grantInventoryAdvancements(serverPlayer);
        checkSodaAdvancements(serverPlayer);
    }

    private static void checkSodaAdvancements(ServerPlayer player) {
        boolean foundSoda = false;
        boolean foundCompound = false;
        boolean foundNegative = false;
        boolean foundPerfect = false;
        boolean foundAmethyst = false;
        boolean foundAcacia = false;
        boolean foundMagma = false;

        for (java.util.List<ItemStack> itemList : java.util.List.of(player.getInventory().items, player.getInventory().offhand)) {
            for (ItemStack stack : itemList) {
                if (!stack.is(ModFluids.SODA_BOTTLE.get()) && !stack.is(ModFluids.SODA_BUCKET.get())) {
                    continue;
                }

                foundSoda = true;

                SodaData data = SodaFluidStackHelper.getSodaData(stack);
                if (data.equals(SodaData.EMPTY)) {
                    continue;
                }

                if (data.effects().size() >= 2) {
                    foundCompound = true;
                }

                if (data.effects().stream().anyMatch(e -> !SodaEffectReducer.isPositive(e))) {
                    foundNegative = true;
                }

                // Amethyst purification sets instability to exactly 0.0f.
                // Since base instability is always >= 0.1f, only amethyst yields 0.
                if (data.instability() <= 0.0001f) {
                    foundPerfect = true;
                    foundAmethyst = true;
                }

                String stabiliser = resolveStabiliser(stack);
                if ("acacia".equals(stabiliser)) {
                    foundAcacia = true;
                }
                if ("magma_cream".equals(stabiliser)) {
                    foundMagma = true;
                }
            }
        }

        if (foundSoda) {
            CreatePopAdvancementGrants.grantObtainedSoda(player);
            ModTriggers.OBTAINED_SODA.trigger(player);
        }
        if (foundCompound) {
            CreatePopAdvancementGrants.grantCompoundSoda(player);
            ModTriggers.OBTAINED_COMPOUND_SODA.trigger(player);
        }
        if (foundNegative) {
            CreatePopAdvancementGrants.grantNegativeSoda(player);
            ModTriggers.OBTAINED_NEGATIVE_SODA.trigger(player);
        }
        if (foundPerfect) {
            CreatePopAdvancementGrants.grantPerfectSoda(player);
            ModTriggers.OBTAINED_PERFECT_SODA.trigger(player);
        }
        if (foundAmethyst) {
            CreatePopAdvancementGrants.grantStabilisedWithAmethyst(player);
            ModTriggers.STABILISED_WITH_AMETHYST.trigger(player);
        }
        if (foundAcacia) {
            CreatePopAdvancementGrants.grantStabilisedWithAcacia(player);
            ModTriggers.STABILISED_WITH_ACACIA.trigger(player);
        }
        if (foundMagma) {
            CreatePopAdvancementGrants.grantStabilisedWithMagmaCream(player);
            ModTriggers.STABILISED_WITH_MAGMA_CREAM.trigger(player);
        }
    }

    private static String resolveStabiliser(ItemStack stack) {
        String fromItem = stack.get(ModDataComponents.SODA_STABILISER.get());
        if (fromItem != null && !fromItem.isBlank()) {
            return fromItem;
        }

        FluidStack contained = FluidStack.EMPTY;
        if (stack.is(ModFluids.SODA_BOTTLE.get())) {
            contained = SodaFluidStackHelper.sodaBottleFluid(stack);
        } else if (stack.is(ModFluids.SODA_BUCKET.get())) {
            contained = SodaFluidStackHelper.sodaBucketFluid(stack);
        }
        return contained.isEmpty() ? null : contained.get(ModDataComponents.SODA_STABILISER.get());
    }

    private static void onItemPickup(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        BrewingDiscoveryManager.learnFromStack(player, event.getOriginalStack());

        if (player instanceof ServerPlayer serverPlayer) {
            BrewingDiscoveryManager.syncKnownNamesInInventory(serverPlayer);
            CreatePopAdvancementGrants.grantInventoryAdvancements(serverPlayer);
            checkSodaAdvancements(serverPlayer);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer) || !serverPlayer.isShiftKeyDown()) {
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();

        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.BREWERS_NOTEBOOK.get())) {
            return;
        }

        java.util.List<BrewingDiscoveryManager.LearnedBlockRecipe> learnedRecipes = BrewingDiscoveryManager.learnResultsFromBlock(serverPlayer, level, event.getPos(), event.getFace());

        // Add only the newly learned recipes to the notebook
        for (BrewingDiscoveryManager.LearnedBlockRecipe learnedRecipe : learnedRecipes) {
            BrewingDiscoveryManager.addLearnedRecipeToNotebook(serverPlayer, stack, learnedRecipe.data());
        }

        if (learnedRecipes.size() == 1) {
            serverPlayer.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.learned_named", learnedRecipes.getFirst().name()), true);
        } else if (!learnedRecipes.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.scanned_added", learnedRecipes.size()), true);
        } else {
            serverPlayer.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.scanned_none"), true);
        }

        if (!learnedRecipes.isEmpty()) {
            level.playSound(null, serverPlayer.blockPosition(), SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace("entity.villager.work_cartographer")), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    private static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafted = event.getCrafting();
        if (!crafted.is(ModItems.BREWERS_NOTEBOOK.get())) {
            return;
        }

        ItemStack firstNotebook = ItemStack.EMPTY;
        ItemStack secondNotebook = ItemStack.EMPTY;
        for (int slot = 0; slot < event.getInventory().getContainerSize(); slot++) {
            ItemStack ingredient = event.getInventory().getItem(slot);
            if (!ingredient.is(ModItems.BREWERS_NOTEBOOK.get())) {
                continue;
            }
            if (firstNotebook.isEmpty()) {
                firstNotebook = ingredient;
            } else {
                secondNotebook = ingredient;
                break;
            }
        }

        if (!firstNotebook.isEmpty() && !secondNotebook.isEmpty()) {
            BrewingDiscoveryManager.mergeNotebookStacks(crafted, firstNotebook, secondNotebook);
        } else if (!firstNotebook.isEmpty()) {
            BrewingDiscoveryManager.copyNotebookToOutput(crafted, firstNotebook);
        }
    }
}
