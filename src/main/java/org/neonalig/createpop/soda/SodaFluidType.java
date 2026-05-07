package org.neonalig.createpop.soda;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.neonalig.createpop.component.SodaData;

import java.util.Comparator;
import java.util.List;

public class SodaFluidType extends FluidType {
    public SodaFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getBucket(FluidStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return SodaFluidStackHelper.sodaBucket(stack);
    }

    @Override
    public Component getDescription(FluidStack stack) {
        if (stack.isEmpty()) {
            return super.getDescription(stack);
        }

        SodaData data = SodaFluidStackHelper.getSodaData(stack);
        List<MobEffectInstance> effects = SodaEffectReducer.copyEffects(data.effects());
        effects.sort(Comparator.comparing(SodaEffectReducer::effectId));
        if (effects.isEmpty()) {
            return super.getDescription(stack);
        }

        MutableComponent description = super.getDescription(stack).copy().append(Component.literal(" ("));
        for (int i = 0; i < effects.size(); i++) {
            if (i > 0) {
                description.append(Component.literal(", "));
            }
            description.append(SodaTextHelper.formatEffect(effects.get(i)));
        }
        return description.append(Component.literal(")"));
    }
}

