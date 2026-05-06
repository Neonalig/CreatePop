package org.neonalig.createpop.soda;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import org.neonalig.createpop.registry.ModFluids;

public class SodaBucketFluidHandler extends FluidBucketWrapper {
    public SodaBucketFluidHandler(ItemStack container) {
        super(container);
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        return SodaFluidStackHelper.isSoda(fluid);
    }

    @Override
    public FluidStack getFluid() {
        if (!container.is(ModFluids.SODA_BUCKET.get())) {
            return FluidStack.EMPTY;
        }
        return SodaFluidStackHelper.sodaBucketFluid(container);
    }

    @Override
    protected void setFluid(FluidStack fluidStack) {
        if (fluidStack.isEmpty()) {
            container = new ItemStack(Items.BUCKET);
            return;
        }

        if (SodaFluidStackHelper.isSoda(fluidStack)) {
            container = SodaFluidStackHelper.sodaBucket(fluidStack);
            return;
        }

        container = FluidUtil.getFilledBucket(fluidStack);
    }
}

