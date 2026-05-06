package org.neonalig.createpop.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.neonalig.createpop.soda.SodaBucketFluidHandler;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new SodaBucketFluidHandler(stack), ModFluids.SODA_BUCKET.get());
    }
}



