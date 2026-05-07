package org.neonalig.createpop.soda;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.neonalig.createpop.compat.create.DynamicSodaMixing;
import org.neonalig.createpop.registry.ModFluids;

public class SodaBottleFluidHandler implements IFluidHandlerItem {
    private ItemStack container;

    public SodaBottleFluidHandler(ItemStack container) {
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
        if (container.is(ModFluids.SODA_BOTTLE.get())) {
            return SodaFluidStackHelper.sodaBottleFluid(container);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return DynamicSodaMixing.DRINK_AMOUNT;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return SodaFluidStackHelper.isSoda(stack) || SodaFluidStackHelper.isCarbonatedWater(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!container.is(Items.GLASS_BOTTLE)) {
            return 0;
        }
        if (resource.getAmount() < DynamicSodaMixing.DRINK_AMOUNT) {
            return 0;
        }

        // Create checks fillability with SIMULATE on the original stack.
        // Accept stacked bottles there; EXECUTE still requires a single split item.
        if (!action.execute()) {
            return container.getCount() > 0 ? DynamicSodaMixing.DRINK_AMOUNT : 0;
        }
        if (container.getCount() != 1) {
            return 0;
        }

        if (SodaFluidStackHelper.isCarbonatedWater(resource)) {
            if (action.execute()) {
                container = new ItemStack(ModFluids.CARBONATED_WATER_BOTTLE.get());
            }
            return DynamicSodaMixing.DRINK_AMOUNT;
        }

        if (SodaFluidStackHelper.isSoda(resource)) {
            if (action.execute()) {
                container = SodaFluidStackHelper.sodaBottle(resource);
            }
            return DynamicSodaMixing.DRINK_AMOUNT;
        }

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
            return contained;
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
        return contained;
    }
}

