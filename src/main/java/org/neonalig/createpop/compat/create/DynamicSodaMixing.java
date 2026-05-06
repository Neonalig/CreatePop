package org.neonalig.createpop.compat.create;

import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.soda.SodaEffectReducer;
import org.neonalig.createpop.soda.SodaFluidStackHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DynamicSodaMixing {
    public static final int DRINK_AMOUNT = 250;

    private DynamicSodaMixing() {
    }

    public static Optional<MixingRecipe> findRecipe(BasinBlockEntity basin) {
        Level level = basin.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        List<FluidStack> fluids = availableFluids(basin);
        if (fluids.isEmpty()) {
            return Optional.empty();
        }

        List<SodaInput> inputs = fluids.stream()
                .filter(stack -> stack.getAmount() >= DRINK_AMOUNT)
                .map(DynamicSodaMixing::asSodaInput)
                .flatMap(Optional::stream)
                .toList();

        Optional<MixingRecipe> base = findCarbonatedPotionRecipe(inputs);
        if (base.isPresent()) {
            return base;
        }

        Optional<MixingRecipe> dye = findSodaDyeRecipe(inputs, findDyeInput(availableItems(basin)));
        if (dye.isPresent()) {
            return dye;
        }

        long seed = level instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L;
        return findSodaCombinationRecipe(inputs, seed);
    }

    private static Optional<MixingRecipe> findSodaDyeRecipe(List<SodaInput> inputs, Optional<DyeInput> dyeInput) {
        if (dyeInput.isEmpty()) {
            return Optional.empty();
        }

        DyeInput dye = dyeInput.get();
        for (SodaInput input : inputs) {
            if (!input.soda()) {
                continue;
            }

            SodaData recolored = recolor(input.data(), dye.color());
            FluidStack output = SodaFluidStackHelper.soda(DRINK_AMOUNT, recolored);
            return Optional.of(recipe(
                    "soda_dye",
                    output,
                    List.of(exactFluid(input.stack(), DRINK_AMOUNT)),
                    Ingredient.of(dye.item())
            ));
        }

        return Optional.empty();
    }

    private static Optional<MixingRecipe> findCarbonatedPotionRecipe(List<SodaInput> inputs) {
        for (SodaInput first : inputs) {
            if (!first.carbonatedWater()) {
                continue;
            }
            for (SodaInput second : inputs) {
                if (second == first || !second.potion()) {
                    continue;
                }
                SodaData data = SodaEffectReducer.baseFromPotion(second.data().effects(), second.data().color());
                FluidStack output = SodaFluidStackHelper.soda(DRINK_AMOUNT, data);
                return Optional.of(recipe("soda_base", output, List.of(exactFluid(first.stack(), DRINK_AMOUNT), exactFluid(second.stack(), DRINK_AMOUNT))));
            }
        }
        return Optional.empty();
    }

    private static Optional<MixingRecipe> findSodaCombinationRecipe(List<SodaInput> inputs, long seed) {
        for (int i = 0; i < inputs.size(); i++) {
            SodaInput first = inputs.get(i);
            if (!first.soda()) {
                continue;
            }
            for (int j = i + 1; j < inputs.size(); j++) {
                SodaInput second = inputs.get(j);
                if (!second.soda() && !second.potion()) {
                    continue;
                }
                // Prevent same-for-same mixes: if the two fluid stacks are the same fluid
                // with identical data components (e.g., output soda leaking back as an input,
                // or two tanks holding the same soda), skip to avoid draining the basin.
                if (FluidStack.isSameFluidSameComponents(first.stack(), second.stack())) {
                    continue;
                }
                SodaData mixed = SodaEffectReducer.mix(first.data(), second.data(), seed);
                FluidStack output = SodaFluidStackHelper.soda(DRINK_AMOUNT, mixed);
                return Optional.of(recipe("soda_mix", output, List.of(exactFluid(first.stack(), DRINK_AMOUNT), exactFluid(second.stack(), DRINK_AMOUNT))));
            }
        }
        return Optional.empty();
    }

    private static MixingRecipe recipe(String name, FluidStack output, List<SizedFluidIngredient> fluids, Ingredient... items) {
        StandardProcessingRecipe.Builder<MixingRecipe> builder = new StandardProcessingRecipe.Builder<>(
                MixingRecipe::new,
                ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "dynamic/" + name)
        ).withFluidIngredients(NonNullList.of(new SizedFluidIngredient(DataComponentFluidIngredient.of(true, output), output.getAmount()), fluids.toArray(SizedFluidIngredient[]::new)))
                .withFluidOutputs(output)
                .duration(100);

        for (Ingredient item : items) {
            builder.require(item);
        }

        return builder.build();
    }

    private static SizedFluidIngredient exactFluid(FluidStack stack, int amount) {
        FluidStack copy = stack.copyWithAmount(amount);
        return new SizedFluidIngredient(DataComponentFluidIngredient.of(true, copy), amount);
    }

    private static List<FluidStack> availableFluids(BasinBlockEntity basin) {
        var handler = basin.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, basin.getBlockPos(), null);
        if (handler == null) {
            return List.of();
        }

        List<FluidStack> fluids = new ArrayList<>();
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stack = handler.getFluidInTank(tank);
            if (!stack.isEmpty()) {
                fluids.add(stack.copy());
            }
        }
        return fluids;
    }

    private static List<ItemStack> availableItems(BasinBlockEntity basin) {
        IItemHandler handler = basin.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, basin.getBlockPos(), null);
        if (handler == null) {
            return List.of();
        }

        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        return items;
    }

    private static Optional<DyeInput> findDyeInput(List<ItemStack> items) {
        for (ItemStack stack : items) {
            if (stack.getItem() instanceof DyeItem dyeItem) {
                return Optional.of(new DyeInput(dyeItem, dyeItem.getDyeColor().getFireworkColor()));
            }
        }
        return Optional.empty();
    }

    private static SodaData recolor(SodaData data, int dyeColor) {
        int mixedColor = weightedAverageColor(data.color(), SodaData.withAlpha(dyeColor), 1, 2);
        return new SodaData(data.effects(), mixedColor, data.instability());
    }

    private static int weightedAverageColor(int first, int second, int firstWeight, int secondWeight) {
        int totalWeight = Math.max(1, firstWeight + secondWeight);
        int r = ((((first >> 16) & 0xFF) * firstWeight) + (((second >> 16) & 0xFF) * secondWeight)) / totalWeight;
        int g = ((((first >> 8) & 0xFF) * firstWeight) + (((second >> 8) & 0xFF) * secondWeight)) / totalWeight;
        int b = (((first & 0xFF) * firstWeight) + ((second & 0xFF) * secondWeight)) / totalWeight;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }


    private static Optional<SodaInput> asSodaInput(FluidStack stack) {
        if (SodaFluidStackHelper.isCarbonatedWater(stack)) {
            return Optional.of(new SodaInput(stack, SodaData.EMPTY, true, false, false));
        }
        if (SodaFluidStackHelper.isSoda(stack)) {
            return Optional.of(new SodaInput(stack, SodaFluidStackHelper.getSodaData(stack), false, true, false));
        }

        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion == null || !potion.hasEffects()) {
            return Optional.empty();
        }

        List<MobEffectInstance> tierOneEffects = new ArrayList<>();
        for (MobEffectInstance effect : potion.getAllEffects()) {
            if (effect.getAmplifier() == 0 && effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                tierOneEffects.add(new MobEffectInstance(effect));
            }
        }

        if (tierOneEffects.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SodaInput(stack, SodaData.ofPotion(tierOneEffects, potion.getColor()), false, false, true));
    }

    private record SodaInput(FluidStack stack, SodaData data, boolean carbonatedWater, boolean soda, boolean potion) {
    }

    private record DyeInput(Item item, int color) {
    }
}

