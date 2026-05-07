package org.neonalig.createpop.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.neoforged.neoforge.fluids.FluidStack;
import org.neonalig.createpop.network.ModPayloads;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.registry.ModFluids;
import org.neonalig.createpop.soda.SodaFluidStackHelper;

import javax.annotation.Nonnull;

public final class ModClientEvents {
    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");

    private ModClientEvents() {
    }

    public static void register(IEventBus modEventBus) {
        ModPayloads.registerClientScreens(new ModPayloads.ClientScreens() {
            @Override
            public void openPrompt(org.neonalig.createpop.network.OpenSodaNamePromptPayload payload) {
                ModPayloadsClient.openPrompt(payload);
            }

            @Override
            public void openNotebook(org.neonalig.createpop.network.OpenBrewersNotebookPayload payload) {
                ModPayloadsClient.openNotebook(payload);
            }

            @Override
            public void openGuide() {
                ModPayloadsClient.openGuide();
            }
        });
        modEventBus.addListener(ModClientEvents::registerClientExtensions);
        modEventBus.addListener(ModClientEvents::registerItemColors);
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new StaticFluidExtensions(0xFFB8F7FF), ModFluids.CARBONATED_WATER_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            @Nonnull
            public ResourceLocation getStillTexture() {
                return WATER_STILL;
            }

            @Override
            @Nonnull
            public ResourceLocation getFlowingTexture() {
                return WATER_FLOW;
            }

            @Override
            @Nonnull
            public ResourceLocation getOverlayTexture() {
                return WATER_OVERLAY;
            }

            @Override
            public int getTintColor() {
                return SodaData.DEFAULT_COLOR;
            }

            @Override
            public int getTintColor(@Nonnull FluidStack stack) {
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
        @Nonnull
        public ResourceLocation getStillTexture() {
            return WATER_STILL;
        }

        @Override
        @Nonnull
        public ResourceLocation getFlowingTexture() {
            return WATER_FLOW;
        }

        @Override
        @Nonnull
        public ResourceLocation getOverlayTexture() {
            return WATER_OVERLAY;
        }

        @Override
        public int getTintColor() {
            return tint;
        }
    }
}

