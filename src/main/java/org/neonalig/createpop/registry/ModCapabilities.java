package org.neonalig.createpop.registry;

import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.neonalig.createpop.soda.SodaBucketFluidHandler;
import org.neonalig.createpop.soda.SodaBottleFluidHandler;
import org.neonalig.createpop.soda.CarbonatedWaterBottleFluidHandler;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new SodaBucketFluidHandler(stack), ModFluids.SODA_BUCKET.get());
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new SodaBottleFluidHandler(stack),
                Items.GLASS_BOTTLE,
                ModFluids.SODA_BOTTLE.get()
        );
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new CarbonatedWaterBottleFluidHandler(stack),
                ModFluids.CARBONATED_WATER_BOTTLE.get()
        );
    }
}



