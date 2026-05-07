package org.neonalig.createpop;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.neonalig.createpop.advancement.ModTriggers;
import org.neonalig.createpop.client.ModClientEvents;
import org.neonalig.createpop.event.ModCommonEvents;
import org.neonalig.createpop.network.ModPayloads;
import org.neonalig.createpop.registry.ModCapabilities;
import org.neonalig.createpop.registry.ModDataComponents;
import org.neonalig.createpop.registry.ModFluids;
import org.neonalig.createpop.registry.ModItems;

@Mod(CreatePop.MODID)
public class CreatePop {
    public static final String MODID = "createpop";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    static {
        CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.createpop"))
                .withTabsBefore(CreativeModeTabs.FOOD_AND_DRINKS)
                .icon(() -> ModFluids.SODA_BUCKET.get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(ModItems.BREWERS_GUIDE.get());
                    output.accept(ModItems.BREWERS_NOTEBOOK.get());
                    output.accept(ModFluids.CARBONATED_WATER_BUCKET.get());
                    output.accept(ModFluids.CARBONATED_WATER_BOTTLE.get());
                    output.accept(ModFluids.SODA_BUCKET.get());
                    output.accept(ModFluids.SODA_BOTTLE.get());
                })
                .build());
    }

    public CreatePop(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.init();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModFluids.register(modEventBus);
        ModTriggers.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, CreatePopConfig.COMMON_SPEC);
        modEventBus.addListener(ModCapabilities::registerCapabilities);
        modEventBus.addListener(ModPayloads::register);
        ModCommonEvents.register();
        NeoForge.EVENT_BUS.addListener(JeiHintsCommand::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {IConfigScreenFactory configScreenFactory = ConfigurationScreen::new;
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, configScreenFactory);
            ModClientEvents.register(modEventBus);
        }
    }
}
