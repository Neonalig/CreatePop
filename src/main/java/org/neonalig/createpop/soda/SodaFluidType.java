package org.neonalig.createpop.soda;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.neonalig.createpop.component.SodaData;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SodaFluidType extends FluidType {
    public SodaFluidType(Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public ItemStack getBucket(@Nonnull FluidStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return SodaFluidStackHelper.sodaBucket(stack);
    }

    @Override
    @Nonnull
    public Component getDescription(@Nonnull FluidStack stack) {
        if (stack.isEmpty()) {
            return super.getDescription(stack);
        }

        SodaData data = SodaFluidStackHelper.getSodaData(stack);
        List<MobEffectInstance> effects = SodaEffectReducer.copyEffects(data.effects());
        effects.sort(Comparator.comparing(SodaEffectReducer::effectId));

        MutableComponent description = super.getDescription(stack).copy().append(Component.literal(" ("));
        if (effects.isEmpty()) {
            description.append(Component.translatable("createpop.soda.tooltip.no_effects"));
        } else {
            for (int i = 0; i < effects.size(); i++) {
                if (i > 0) {
                    description.append(Component.literal(", "));
                }
                description.append(SodaTextHelper.formatEffect(effects.get(i)));
            }
        }

        description.append(Component.literal(", "));
        description.append(Component.translatable(
                "createpop.soda.tooltip.instability",
                String.format(Locale.ROOT, "%.2f", data.instability())
        ).withColor(0xFFB347));
        return description.append(Component.literal(")"));
    }
}

