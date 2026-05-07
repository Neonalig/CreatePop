package org.neonalig.createpop.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.neonalig.createpop.CreatePop;

public record RemoveNotebookEntryPayload(boolean mainHand, String entryKey) implements CustomPacketPayload {
    public static final Type<RemoveNotebookEntryPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "remove_notebook_entry")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveNotebookEntryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RemoveNotebookEntryPayload::mainHand,
            ByteBufCodecs.STRING_UTF8,
            RemoveNotebookEntryPayload::entryKey,
            RemoveNotebookEntryPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

