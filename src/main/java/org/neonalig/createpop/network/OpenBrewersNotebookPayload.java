package org.neonalig.createpop.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.component.BrewersNotebookData;

public record OpenBrewersNotebookPayload(boolean mainHand, BrewersNotebookData notebookData) implements CustomPacketPayload {
    public static final Type<OpenBrewersNotebookPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "open_brewers_notebook")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBrewersNotebookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            OpenBrewersNotebookPayload::mainHand,
            BrewersNotebookData.STREAM_CODEC,
            OpenBrewersNotebookPayload::notebookData,
            OpenBrewersNotebookPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

