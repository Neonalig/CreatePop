package org.neonalig.createpop.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.neonalig.createpop.component.BrewersNotebookData;
import org.neonalig.createpop.registry.ModItems;
import org.neonalig.createpop.soda.BrewingDiscoveryManager;

public final class ModCommonEvents {
    private ModCommonEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ModCommonEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ModCommonEvents::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(ModCommonEvents::onRightClickBlock);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 40 != 0) {
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            BrewingDiscoveryManager.learnFromStack(player, stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            BrewingDiscoveryManager.learnFromStack(player, stack);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.BREWERS_NOTEBOOK.get())) {
            return;
        }

        java.util.List<String> learnedNames = BrewingDiscoveryManager.learnNamesFromBlock(player, player.level(), event.getPos(), event.getFace());

        // Add only the newly learned recipes to the notebook
        for (String learnedName : learnedNames) {
            BrewersNotebookData.Entry entry = BrewingDiscoveryManager.playerData(player).entries().stream()
                    .filter(e -> learnedName.equals(e.name()))
                    .findFirst()
                    .orElse(null);
            if (entry != null) {
                BrewingDiscoveryManager.addLearnedRecipeToNotebook(player, stack, entry.data());
            }
        }

        if (learnedNames.size() == 1) {
            player.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.learned_named", learnedNames.get(0)), true);
        } else if (!learnedNames.isEmpty()) {
            player.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.scanned_added", learnedNames.size()), true);
        } else {
            player.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.scanned_none"), true);
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

