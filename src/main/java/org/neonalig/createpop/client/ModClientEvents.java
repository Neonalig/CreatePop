package org.neonalig.createpop.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.neoforged.neoforge.fluids.FluidStack;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.registry.ModFluids;
import org.neonalig.createpop.soda.SodaFluidStackHelper;

public final class ModClientEvents {
    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");

    private ModClientEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModClientEvents::registerClientExtensions);
        modEventBus.addListener(ModClientEvents::registerItemColors);
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new StaticFluidExtensions(0xFFB8F7FF), ModFluids.CARBONATED_WATER_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return WATER_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return WATER_FLOW;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return WATER_OVERLAY;
            }

            @Override
            public int getTintColor() {
                return SodaData.DEFAULT_COLOR;
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return SodaFluidStackHelper.getSodaData(stack).color();
            }
        }, ModFluids.SODA_TYPE.get());
    }

    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        DynamicFluidContainerModel.Colors colors = new DynamicFluidContainerModel.Colors();
        event.register(colors, ModFluids.CARBONATED_WATER_BUCKET.get(), ModFluids.SODA_BUCKET.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? SodaFluidStackHelper.getSodaData(stack).color() : 0xFFFFFFFF, ModFluids.SODA_BOTTLE.get());
    }

    private record StaticFluidExtensions(int tint) implements IClientFluidTypeExtensions {
        @Override
        public ResourceLocation getStillTexture() {
            return WATER_STILL;
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return WATER_FLOW;
        }

        @Override
        public ResourceLocation getOverlayTexture() {
            return WATER_OVERLAY;
        }

        @Override
        public int getTintColor() {
            return tint;
        }
    }
}

