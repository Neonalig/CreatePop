package org.neonalig.createpop.soda;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

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
}

