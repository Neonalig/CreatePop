package org.neonalig.createpop.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;

public record SodaData(List<MobEffectInstance> effects, int color, float instability) {
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;
    public static final SodaData EMPTY = new SodaData(List.of(), DEFAULT_COLOR, 0.0f);

    public static final Codec<SodaData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MobEffectInstance.CODEC.listOf().fieldOf("effects").forGetter(SodaData::effects),
            Codec.INT.fieldOf("color").forGetter(SodaData::color),
            Codec.FLOAT.fieldOf("instability").forGetter(SodaData::instability)
    ).apply(instance, SodaData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SodaData> STREAM_CODEC = StreamCodec.composite(
            MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()), SodaData::effects,
            ByteBufCodecs.INT, SodaData::color,
            ByteBufCodecs.FLOAT, SodaData::instability,
            SodaData::new
    );

    public SodaData {
        effects = List.copyOf(effects);
        color = withAlpha(color);
        instability = Math.max(0.0f, instability);
    }

    public static SodaData ofPotion(List<MobEffectInstance> effects, int color) {
        return new SodaData(effects, color, 0.1f);
    }

    public int rgbColor() {
        return color & 0x00FFFFFF;
    }

    public static int colorFromEffects(List<MobEffectInstance> effects) {
        return withAlpha(PotionContents.getColor(effects));
    }

    public static int averageColor(int first, int second) {
        int r = (((first >> 16) & 0xFF) + ((second >> 16) & 0xFF)) / 2;
        int g = (((first >> 8) & 0xFF) + ((second >> 8) & 0xFF)) / 2;
        int b = ((first & 0xFF) + (second & 0xFF)) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int color) {
        return (color & 0xFF000000) == 0 ? 0xFF000000 | color : color;
    }
}

