package org.neonalig.createpop.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.neonalig.createpop.CreatePop;

public record UpdateNotebookNotePayload(String entryKey, String note) implements CustomPacketPayload {
    public static final Type<UpdateNotebookNotePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "update_notebook_note")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateNotebookNotePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            UpdateNotebookNotePayload::entryKey,
            ByteBufCodecs.STRING_UTF8,
            UpdateNotebookNotePayload::note,
            UpdateNotebookNotePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

