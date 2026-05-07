package org.neonalig.createpop.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.component.BrewersNotebookData;
import org.neonalig.createpop.component.SodaData;

public final class ModDataComponents {
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreatePop.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SodaData>> SODA_DATA = DATA_COMPONENTS.registerComponentType(
            "soda_data",
            builder -> builder.persistent(SodaData.CODEC).networkSynchronized(SodaData.STREAM_CODEC).cacheEncoding()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BrewersNotebookData>> BREWERS_NOTEBOOK_DATA = DATA_COMPONENTS.registerComponentType(
            "brewers_notebook_data",
            builder -> builder.persistent(BrewersNotebookData.CODEC).networkSynchronized(BrewersNotebookData.STREAM_CODEC).cacheEncoding()
    );

    /**
     * Tracks which stabiliser ingredient was last applied to this soda.
     * Values: "acacia", "magma_cream". Amethyst is detected by instability == 0.
     * Stored on both FluidStack and ItemStack so it survives bottling/bucketing.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SODA_STABILISER = DATA_COMPONENTS.registerComponentType(
            "soda_stabiliser",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
    );

    private ModDataComponents() {
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}

