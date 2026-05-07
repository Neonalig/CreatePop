package org.neonalig.createpop.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.soda.SodaBottleItem;
import org.neonalig.createpop.soda.SodaBucketItem;
import org.neonalig.createpop.soda.SodaFluidType;

public final class ModFluids {
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, CreatePop.MODID);
    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CreatePop.MODID);

    public static final DeferredHolder<FluidType, FluidType> CARBONATED_WATER_TYPE = FLUID_TYPES.register(
            "carbonated_water",
            () -> new FluidType(FluidType.Properties.create().density(1000).viscosity(1000))
    );
    public static final DeferredHolder<FluidType, FluidType> SODA_TYPE = FLUID_TYPES.register(
            "soda",
            () -> new SodaFluidType(FluidType.Properties.create().density(1050).viscosity(1200))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CARBONATED_WATER = FLUIDS.register(
            "carbonated_water",
            () -> new BaseFlowingFluid.Source(carbonatedWaterProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_CARBONATED_WATER = FLUIDS.register(
            "flowing_carbonated_water",
            () -> new BaseFlowingFluid.Flowing(carbonatedWaterProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SODA = FLUIDS.register(
            "soda",
            () -> new BaseFlowingFluid.Source(sodaProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SODA = FLUIDS.register(
            "flowing_soda",
            () -> new BaseFlowingFluid.Flowing(sodaProperties())
    );

    public static final DeferredBlock<LiquidBlock> CARBONATED_WATER_BLOCK = CreatePop.BLOCKS.register(
            "carbonated_water",
            () -> new LiquidBlock(CARBONATED_WATER.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable())
    );
    public static final DeferredBlock<LiquidBlock> SODA_BLOCK = CreatePop.BLOCKS.register(
            "soda",
            () -> new LiquidBlock(SODA.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable())
    );

    public static final DeferredItem<BucketItem> CARBONATED_WATER_BUCKET = CreatePop.ITEMS.register(
            "carbonated_water_bucket",
            () -> new BucketItem(CARBONATED_WATER.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))
    );
    public static final DeferredItem<BucketItem> SODA_BUCKET = CreatePop.ITEMS.register(
            "soda_bucket",
            () -> new SodaBucketItem(SODA.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))
    );
    public static final DeferredItem<SodaBottleItem> SODA_BOTTLE = CreatePop.ITEMS.register(
            "soda_bottle",
            () -> new SodaBottleItem(new Item.Properties().stacksTo(1))
    );

    private ModFluids() {
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }

    private static BaseFlowingFluid.Properties carbonatedWaterProperties() {
        return new BaseFlowingFluid.Properties(CARBONATED_WATER_TYPE, CARBONATED_WATER, FLOWING_CARBONATED_WATER)
                .block(CARBONATED_WATER_BLOCK)
                .bucket(CARBONATED_WATER_BUCKET);
    }

    private static BaseFlowingFluid.Properties sodaProperties() {
        return new BaseFlowingFluid.Properties(SODA_TYPE, SODA, FLOWING_SODA)
                .block(SODA_BLOCK)
                .bucket(SODA_BUCKET)
                .tickRate(6);
    }
}

