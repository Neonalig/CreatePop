package org.neonalig.createpop.soda;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import org.neonalig.createpop.component.SodaData;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SodaTextHelper {
    private SodaTextHelper() {
    }

    public static void appendSodaTooltip(List<Component> tooltip, SodaData data) {
        List<MobEffectInstance> effects = SodaEffectReducer.copyEffects(data.effects());
        effects.sort(Comparator.comparing(SodaEffectReducer::effectId));

        if (effects.isEmpty()) {
            tooltip.add(Component.translatable("createpop.soda.tooltip.no_effects").withStyle(ChatFormatting.GRAY));
        } else {
            for (MobEffectInstance effect : effects) {
                tooltip.add(formatEffect(effect));
            }
        }

        tooltip.add(Component.translatable("createpop.soda.tooltip.instability", String.format(Locale.ROOT, "%.2f", data.instability()))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    public static Component formatEffect(MobEffectInstance effect) {
        MutableComponent effectText = Component.translatable(effect.getDescriptionId());
        if (effect.getAmplifier() > 0) {
            effectText = Component.translatable("potion.withAmplifier", effectText, Component.translatable("potion.potency." + effect.getAmplifier()));
        }
        if (effect.getDuration() > 20) {
            effectText = Component.translatable("potion.withDuration", effectText, MobEffectUtil.formatDuration(effect, 1.0F, 1.0F));
        }

        MobEffectCategory category = effect.getEffect().value().getCategory();
        return effectText.withStyle(category.getTooltipFormatting());
    }

    public static String formatColorHex(int color) {
        return String.format(Locale.ROOT, "#%08X", color);
    }
}

