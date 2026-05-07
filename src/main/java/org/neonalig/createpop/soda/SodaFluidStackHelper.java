package org.neonalig.createpop.soda;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.neonalig.createpop.component.BrewersNotebookData;
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

    public static ItemStack sodaBucket(FluidStack stack) {
        ItemStack item = sodaBucket(getSodaData(stack));
        String stabiliser = stack.get(ModDataComponents.SODA_STABILISER.get());
        if (stabiliser != null) {
            item.set(ModDataComponents.SODA_STABILISER.get(), stabiliser);
        }
        return item;
    }

    public static ItemStack sodaBucket(SodaData data) {
        ItemStack stack = new ItemStack(ModFluids.SODA_BUCKET.get());
        stack.set(ModDataComponents.SODA_DATA.get(), data);
        applyKnownSodaName(stack, data);
        return stack;
    }

    public static FluidStack sodaBucketFluid(ItemStack stack) {
        FluidStack fluid = soda(FluidType.BUCKET_VOLUME, getSodaData(stack));
        copyStabiliser(stack, fluid);
        return fluid;
    }

    public static ItemStack sodaBottle(FluidStack stack) {
        ItemStack item = sodaBottle(getSodaData(stack));
        String stabiliser = stack.get(ModDataComponents.SODA_STABILISER.get());
        if (stabiliser != null) {
            item.set(ModDataComponents.SODA_STABILISER.get(), stabiliser);
        }
        return item;
    }

    public static ItemStack sodaBottle(SodaData data) {
        ItemStack stack = new ItemStack(ModFluids.SODA_BOTTLE.get());
        stack.set(ModDataComponents.SODA_DATA.get(), data);
        applyKnownSodaName(stack, data);
        return stack;
    }

    public static FluidStack sodaBottleFluid(ItemStack stack) {
        FluidStack fluid = soda(org.neonalig.createpop.compat.create.DynamicSodaMixing.DRINK_AMOUNT, getSodaData(stack));
        copyStabiliser(stack, fluid);
        return fluid;
    }

    public static SodaData getSodaData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SODA_DATA.get(), SodaData.EMPTY);
    }

    public static void applyKnownSodaName(ItemStack stack, SodaData data) {
        String name = resolveKnownSodaName(data);
        if (name != null && !name.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(style -> style.withItalic(false)));
        }
    }

    private static void copyStabiliser(ItemStack source, FluidStack target) {
        String stabiliser = source.get(ModDataComponents.SODA_STABILISER.get());
        if (stabiliser != null && !stabiliser.isBlank()) {
            target.set(ModDataComponents.SODA_STABILISER.get(), stabiliser);
        }
    }

    private static String resolveKnownSodaName(SodaData data) {
        if (data.equals(SodaData.EMPTY)) {
            return null;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        String key = BrewersNotebookData.keyFor(data);
        return SodaNameRegistrySavedData.get(server.overworld()).getName(key);
    }
}

