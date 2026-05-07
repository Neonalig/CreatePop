package org.neonalig.createpop.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.neonalig.createpop.CreatePop;

public record SubmitSodaNamePayload(String sodaKey, String chosenName) implements CustomPacketPayload {
    public static final Type<SubmitSodaNamePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "submit_soda_name")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitSodaNamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SubmitSodaNamePayload::sodaKey,
            ByteBufCodecs.STRING_UTF8,
            SubmitSodaNamePayload::chosenName,
            SubmitSodaNamePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

