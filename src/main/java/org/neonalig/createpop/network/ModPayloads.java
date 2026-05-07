package org.neonalig.createpop.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.neonalig.createpop.registry.ModItems;
import org.neonalig.createpop.soda.BrewingDiscoveryManager;

public final class ModPayloads {
    private static ClientScreens clientScreens = ClientScreens.NOOP;

    private ModPayloads() {
    }

    public static void registerClientScreens(ClientScreens clientScreens) {
        ModPayloads.clientScreens = clientScreens;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenSodaNamePromptPayload.TYPE, OpenSodaNamePromptPayload.STREAM_CODEC, ModPayloads::handleOpenPrompt);
        registrar.playToClient(OpenBrewersNotebookPayload.TYPE, OpenBrewersNotebookPayload.STREAM_CODEC, ModPayloads::handleOpenNotebook);
        registrar.playToClient(OpenBrewersGuidePayload.TYPE, OpenBrewersGuidePayload.STREAM_CODEC, ModPayloads::handleOpenGuide);
        registrar.playToServer(SubmitSodaNamePayload.TYPE, SubmitSodaNamePayload.STREAM_CODEC, ModPayloads::handleSubmitName);
        registrar.playToServer(RemoveNotebookEntryPayload.TYPE, RemoveNotebookEntryPayload.STREAM_CODEC, ModPayloads::handleRemoveEntry);
        registrar.playToServer(UpdateNotebookNotePayload.TYPE, UpdateNotebookNotePayload.STREAM_CODEC, ModPayloads::handleUpdateNote);
    }

    private static void handleOpenPrompt(OpenSodaNamePromptPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientScreens.openPrompt(payload));
    }

    private static void handleOpenNotebook(OpenBrewersNotebookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientScreens.openNotebook(payload));
    }

    private static void handleOpenGuide(OpenBrewersGuidePayload payload, IPayloadContext context) {
        context.enqueueWork(clientScreens::openGuide);
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
                ItemStack stack = notebookInHand(serverPlayer, payload.mainHand());
                if (!stack.isEmpty()) {
                    BrewingDiscoveryManager.removeNotebookEntry(stack, payload.entryKey());
                }
            }
        });
    }

    private static void handleUpdateNote(UpdateNotebookNotePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ItemStack stack = notebookInHand(serverPlayer, payload.mainHand());
                if (!stack.isEmpty()) {
                    BrewingDiscoveryManager.updateNotebookEntryNote(stack, payload.entryKey(), payload.note());
                }
            }
        });
    }

    private static ItemStack notebookInHand(ServerPlayer player, boolean mainHand) {
        InteractionHand hand = mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);
        return stack.is(ModItems.BREWERS_NOTEBOOK.get()) ? stack : ItemStack.EMPTY;
    }

    public interface ClientScreens {
        ClientScreens NOOP = new ClientScreens() {
            @Override
            public void openPrompt(OpenSodaNamePromptPayload payload) {
            }

            @Override
            public void openNotebook(OpenBrewersNotebookPayload payload) {
            }

            @Override
            public void openGuide() {
            }
        };

        void openPrompt(OpenSodaNamePromptPayload payload);

        void openNotebook(OpenBrewersNotebookPayload payload);

        void openGuide();
    }
}

