package org.neonalig.createpop.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.neonalig.createpop.CreatePop;

public record OpenBrewersGuidePayload(boolean open) implements CustomPacketPayload {
    public static final Type<OpenBrewersGuidePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "open_brewers_guide")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBrewersGuidePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            OpenBrewersGuidePayload::open,
            OpenBrewersGuidePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

