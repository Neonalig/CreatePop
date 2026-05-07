package org.neonalig.createpop.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.neonalig.createpop.CreatePop;

public record OpenSodaNamePromptPayload(String sodaKey, String suggestedName) implements CustomPacketPayload {
    public static final Type<OpenSodaNamePromptPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "open_soda_name_prompt")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSodaNamePromptPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenSodaNamePromptPayload::sodaKey,
            ByteBufCodecs.STRING_UTF8,
            OpenSodaNamePromptPayload::suggestedName,
            OpenSodaNamePromptPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

