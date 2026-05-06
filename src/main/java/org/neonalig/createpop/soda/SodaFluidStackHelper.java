package org.neonalig.createpop.soda;

import net.neoforged.neoforge.fluids.FluidStack;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.registry.ModDataComponents;
import org.neonalig.createpop.registry.ModFluids;

public final class SodaFluidStackHelper {
    private SodaFluidStackHelper() {
    }

    public static FluidStack carbonatedWater(int amount) {
        return new FluidStack(ModFluids.CARBONATED_WATER.get(), amount);
    }

    public static FluidStack soda(int amount, SodaData data) {
        FluidStack stack = new FluidStack(ModFluids.SODA.get(), amount);
        stack.set(ModDataComponents.SODA_DATA.get(), data);
        return stack;
    }

    public static boolean isSoda(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == ModFluids.SODA.get();
    }

    public static boolean isCarbonatedWater(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == ModFluids.CARBONATED_WATER.get();
    }

    public static SodaData getSodaData(FluidStack stack) {
        return stack.getOrDefault(ModDataComponents.SODA_DATA.get(), SodaData.EMPTY);
    }
}

