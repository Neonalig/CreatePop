package org.neonalig.createpop.soda;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.neonalig.createpop.compat.create.DynamicSodaMixing;

public class CarbonatedWaterBottleFluidHandler implements IFluidHandlerItem {
    private ItemStack container;

    public CarbonatedWaterBottleFluidHandler(ItemStack container) {
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return SodaFluidStackHelper.carbonatedWater(DynamicSodaMixing.DRINK_AMOUNT);
    }

    @Override
    public int getTankCapacity(int tank) {
        return DynamicSodaMixing.DRINK_AMOUNT;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (container.getCount() != 1 || resource.getAmount() < DynamicSodaMixing.DRINK_AMOUNT) {
            return FluidStack.EMPTY;
        }
        FluidStack contained = getFluidInTank(0);
        if (!contained.isEmpty() && FluidStack.isSameFluidSameComponents(contained, resource)) {
            if (action.execute()) {
                container = new ItemStack(Items.GLASS_BOTTLE);
            }
            return contained.copyWithAmount(DynamicSodaMixing.DRINK_AMOUNT);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (container.getCount() != 1 || maxDrain < DynamicSodaMixing.DRINK_AMOUNT) {
            return FluidStack.EMPTY;
        }
        FluidStack contained = getFluidInTank(0);
        if (contained.isEmpty()) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            container = new ItemStack(Items.GLASS_BOTTLE);
        }
        return contained.copyWithAmount(DynamicSodaMixing.DRINK_AMOUNT);
    }
}

