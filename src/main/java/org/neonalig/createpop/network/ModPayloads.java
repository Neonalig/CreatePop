package org.neonalig.createpop.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.neonalig.createpop.soda.BrewingDiscoveryManager;
import org.neonalig.createpop.soda.SodaNameGenerator;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenSodaNamePromptPayload.TYPE, OpenSodaNamePromptPayload.STREAM_CODEC, ModPayloads::handleOpenPrompt);
        registrar.playToServer(SubmitSodaNamePayload.TYPE, SubmitSodaNamePayload.STREAM_CODEC, ModPayloads::handleSubmitName);
        registrar.playToServer(RemoveNotebookEntryPayload.TYPE, RemoveNotebookEntryPayload.STREAM_CODEC, ModPayloads::handleRemoveEntry);
        registrar.playToServer(UpdateNotebookNotePayload.TYPE, UpdateNotebookNotePayload.STREAM_CODEC, ModPayloads::handleUpdateNote);
    }

    private static void handleOpenPrompt(OpenSodaNamePromptPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Class<?> screenClass = Class.forName("org.neonalig.createpop.client.SodaNamePromptScreen");
                Object screen = screenClass.getConstructor(String.class, String.class, java.util.function.Supplier.class)
                        .newInstance(
                                payload.sodaKey(),
                                payload.suggestedName(),
                                (java.util.function.Supplier<String>) () -> SodaNameGenerator.randomName(net.minecraft.util.RandomSource.create())
                        );
                if (screen instanceof Screen gui) {
                    Minecraft.getInstance().setScreen(gui);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private static void handleSubmitName(SubmitSodaNamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                BrewingDiscoveryManager.renameRecipe(serverPlayer, payload.sodaKey(), payload.chosenName());
            }
        });
    }

    private static void handleRemoveEntry(RemoveNotebookEntryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // The player is holding a notebook and wants to remove an entry
                // We need to find the notebook in their inventory
                for (net.minecraft.world.item.ItemStack stack : serverPlayer.getInventory().items) {
                    if (stack.is(org.neonalig.createpop.registry.ModItems.BREWERS_NOTEBOOK.get())) {
                        BrewingDiscoveryManager.removeNotebookEntry(stack, payload.entryKey());
                        return;
                    }
                }
                for (net.minecraft.world.item.ItemStack stack : serverPlayer.getInventory().offhand) {
                    if (stack.is(org.neonalig.createpop.registry.ModItems.BREWERS_NOTEBOOK.get())) {
                        BrewingDiscoveryManager.removeNotebookEntry(stack, payload.entryKey());
                        return;
                    }
                }
            }
        });
    }

    private static void handleUpdateNote(UpdateNotebookNotePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // The player is holding a notebook and wants to update a note
                // We need to find the notebook in their inventory
                for (net.minecraft.world.item.ItemStack stack : serverPlayer.getInventory().items) {
                    if (stack.is(org.neonalig.createpop.registry.ModItems.BREWERS_NOTEBOOK.get())) {
                        BrewingDiscoveryManager.updateNotebookEntryNote(stack, payload.entryKey(), payload.note());
                        return;
                    }
                }
                for (net.minecraft.world.item.ItemStack stack : serverPlayer.getInventory().offhand) {
                    if (stack.is(org.neonalig.createpop.registry.ModItems.BREWERS_NOTEBOOK.get())) {
                        BrewingDiscoveryManager.updateNotebookEntryNote(stack, payload.entryKey(), payload.note());
                        return;
                    }
                }
            }
        });
    }
}

