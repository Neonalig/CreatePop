package org.neonalig.createpop.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.component.SodaData;

public final class ModDataComponents {
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreatePop.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SodaData>> SODA_DATA = DATA_COMPONENTS.registerComponentType(
            "soda_data",
            builder -> builder.persistent(SodaData.CODEC).networkSynchronized(SodaData.STREAM_CODEC).cacheEncoding()
    );

    private ModDataComponents() {
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}

