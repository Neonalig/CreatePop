package org.neonalig.createpop.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import org.neonalig.createpop.CreatePopConfig;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.compat.create.DynamicSodaMixing;
import org.neonalig.createpop.registry.ModFluids;
import org.neonalig.createpop.soda.SodaEffectReducer;
import org.neonalig.createpop.soda.SodaFluidStackHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI plugin for CreatePop.
 * <p>
 * The carbonated-water mixing recipe is a static {@code create:mixing} recipe
 * that Create's own JEI integration displays automatically.
 * This plugin improves search by registering aliases for our custom fluids.
 */
@JeiPlugin
public class CreatePopJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "jei_plugin");
    private static final RecipeType<RecipeHolder<BasinRecipe>> MIXING_TYPE =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath("create", "mixing"));
    private static final ResourceLocation CREATE_POTION_FLUID_ID = ResourceLocation.fromNamespaceAndPath("create", "potion");

    /** Captured during {@link #registerFluidSubtypes} for use in alias registration. */
    private IPlatformFluidHelper<?> platformFluidHelper;
    private IJeiRuntime jeiRuntime;
    private final List<RecipeHolder<BasinRecipe>> injectedRecipes = new ArrayList<>();

    public CreatePopJeiPlugin() {
        NeoForge.EVENT_BUS.addListener(this::onClientLogin);
        NeoForge.EVENT_BUS.addListener(this::onClientLogout);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public <T> void registerFluidSubtypes(ISubtypeRegistration registration, IPlatformFluidHelper<T> helper) {
        this.platformFluidHelper = helper;
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        if (platformFluidHelper != null) {
            addFluidAliases(registration, platformFluidHelper);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MIXING_TYPE, buildPotionBaseRecipes(collectPotionBases()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.jeiRuntime = jeiRuntime;
        refreshRuntimeRecipes();
    }

    @Override
    public void onRuntimeUnavailable() {
        clearInjectedRecipes();
        this.jeiRuntime = null;
    }

    /**
     * Helper that captures the wildcard {@code T} so the generic {@link IIngredientAliasRegistration}
     * calls remain type-safe at the call site.
     */
    private static <T> void addFluidAliases(IIngredientAliasRegistration registration,
                                             IPlatformFluidHelper<T> helper) {
        IIngredientType<T> fluidType = helper.getFluidIngredientType();

        T carbonatedWater = helper.create(
                BuiltInRegistries.FLUID.wrapAsHolder(ModFluids.CARBONATED_WATER.get()), 1000L,
                DataComponentPatch.EMPTY);
        registration.addAliases(fluidType, carbonatedWater,
                List.of("Carbonated Water", "sparkling water", "fizzy water", "soda water"));

        T soda = helper.create(
                BuiltInRegistries.FLUID.wrapAsHolder(ModFluids.SODA.get()), 1000L,
                DataComponentPatch.EMPTY);
        registration.addAliases(fluidType, soda,
                List.of("Soda", "pop", "fizzy drink", "alchemical soda"));
    }

    private void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        refreshRuntimeRecipes();
    }

    private void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearInjectedRecipes();
    }


    private void refreshRuntimeRecipes() {
        if (jeiRuntime == null) {
            return;
        }

        clearInjectedRecipes();

        if (!CreatePopConfig.enableJeiPotionHints()) {
            return;
        }

        long worldSeed = resolveClientSeed();
        List<PotionBaseData> potionBases = collectPotionBases();
        List<RecipeHolder<BasinRecipe>> recipes = buildReactionHintRecipes(potionBases, worldSeed);

        if (recipes.isEmpty()) {
            return;
        }

        jeiRuntime.getRecipeManager().addRecipes(MIXING_TYPE, recipes);
        jeiRuntime.getRecipeManager().unhideRecipes(MIXING_TYPE, recipes);
        injectedRecipes.addAll(recipes);
    }

    private void clearInjectedRecipes() {
        if (jeiRuntime == null || injectedRecipes.isEmpty()) {
            return;
        }
        jeiRuntime.getRecipeManager().hideRecipes(MIXING_TYPE, injectedRecipes);
        injectedRecipes.clear();
    }

    private static List<RecipeHolder<BasinRecipe>> buildPotionBaseRecipes(List<PotionBaseData> potionBases) {
        List<RecipeHolder<BasinRecipe>> recipes = new ArrayList<>();
        for (PotionBaseData base : potionBases) {
            FluidStack output = SodaFluidStackHelper.soda(DynamicSodaMixing.DRINK_AMOUNT, base.sodaData());
            SizedFluidIngredient carbonated = exactFluid(SodaFluidStackHelper.carbonatedWater(DynamicSodaMixing.DRINK_AMOUNT));

            recipes.add(recipeHolder(
                    ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "jei/potion_base/" + base.id().getPath() + "_item"),
                    output,
                    List.of(carbonated),
                    Ingredient.of(base.potionItem())
            ));

            FluidStack potionFluid = potionFluid(base.contents());
            if (!potionFluid.isEmpty()) {
                recipes.add(recipeHolder(
                        ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "jei/potion_base/" + base.id().getPath() + "_fluid"),
                        output,
                        List.of(carbonated, exactFluid(potionFluid))
                ));
            }
        }
        return recipes;
    }

    private static List<RecipeHolder<BasinRecipe>> buildReactionHintRecipes(List<PotionBaseData> potionBases, long worldSeed) {
        List<RecipeHolder<BasinRecipe>> recipes = new ArrayList<>();
        for (int i = 0; i < potionBases.size(); i++) {
            PotionBaseData first = potionBases.get(i);
            FluidStack firstSoda = SodaFluidStackHelper.soda(DynamicSodaMixing.DRINK_AMOUNT, first.sodaData());

            for (int j = i + 1; j < potionBases.size(); j++) {
                PotionBaseData second = potionBases.get(j);
                SodaData outputData = SodaEffectReducer.mix(first.sodaData(), second.sodaData(), DynamicSodaMixing.DRINK_AMOUNT, DynamicSodaMixing.DRINK_AMOUNT, worldSeed);
                FluidStack output = SodaFluidStackHelper.soda(DynamicSodaMixing.DRINK_AMOUNT, outputData);

                String key = first.id().getPath() + "__" + second.id().getPath();
                recipes.add(recipeHolder(
                        ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "jei/hint/" + key + "_potion_item"),
                        output,
                        List.of(exactFluid(firstSoda)),
                        Ingredient.of(second.potionItem())
                ));

                FluidStack potionFluid = potionFluid(second.contents());
                if (!potionFluid.isEmpty()) {
                    recipes.add(recipeHolder(
                            ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "jei/hint/" + key + "_potion_fluid"),
                            output,
                            List.of(exactFluid(firstSoda), exactFluid(potionFluid))
                    ));
                }

                FluidStack secondSoda = SodaFluidStackHelper.soda(DynamicSodaMixing.DRINK_AMOUNT, second.sodaData());
                recipes.add(recipeHolder(
                        ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "jei/hint/" + key + "_soda"),
                        output,
                        List.of(exactFluid(firstSoda), exactFluid(secondSoda))
                ));
            }
        }
        return recipes;
    }

    private static List<PotionBaseData> collectPotionBases() {
        List<PotionBaseData> bases = new ArrayList<>();
        for (Potion potion : BuiltInRegistries.POTION) {
            ResourceLocation id = BuiltInRegistries.POTION.getKey(potion);
            if (id == null || "empty".equals(id.getPath())) {
                continue;
            }

            Holder<Potion> holder = BuiltInRegistries.POTION.wrapAsHolder(potion);
            ItemStack potionItem = PotionContents.createItemStack(Items.POTION, holder);
            PotionContents contents = potionItem.get(DataComponents.POTION_CONTENTS);
            if (contents == null) {
                continue;
            }

            List<MobEffectInstance> tierOneBeneficial = new ArrayList<>();
            for (MobEffectInstance effect : contents.getAllEffects()) {
                if (effect.getAmplifier() == 0 && effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                    tierOneBeneficial.add(new MobEffectInstance(effect));
                }
            }

            if (tierOneBeneficial.isEmpty()) {
                continue;
            }

            SodaData data = SodaEffectReducer.baseFromPotion(tierOneBeneficial, contents.getColor());
            bases.add(new PotionBaseData(id, potionItem, contents, data));
        }
        return bases;
    }

    private static RecipeHolder<BasinRecipe> recipeHolder(ResourceLocation id, FluidStack output, List<SizedFluidIngredient> fluids, Ingredient... items) {
        NonNullList<SizedFluidIngredient> ingredients = NonNullList.create();
        ingredients.addAll(fluids);

        StandardProcessingRecipe.Builder<MixingRecipe> builder = new StandardProcessingRecipe.Builder<>(MixingRecipe::new, id)
                .withFluidIngredients(ingredients)
                .withFluidOutputs(output)
                .duration(100);

        for (Ingredient item : items) {
            builder.require(item);
        }

        return new RecipeHolder<>(id, builder.build());
    }

    private static SizedFluidIngredient exactFluid(FluidStack stack) {
        return new SizedFluidIngredient(DataComponentFluidIngredient.of(true, stack), stack.getAmount());
    }

    private static FluidStack potionFluid(PotionContents contents) {
        var fluid = BuiltInRegistries.FLUID.get(CREATE_POTION_FLUID_ID);
        if (fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        FluidStack stack = new FluidStack(fluid, DynamicSodaMixing.DRINK_AMOUNT);
        stack.set(DataComponents.POTION_CONTENTS, contents);
        return stack;
    }

    private static long resolveClientSeed() {
        Minecraft minecraft = Minecraft.getInstance();
        MinecraftServer integratedServer = minecraft.getSingleplayerServer();
        if (integratedServer != null) {
            return integratedServer.overworld().getSeed();
        }
        return 0L;
    }

    private record PotionBaseData(ResourceLocation id, ItemStack potionItem, PotionContents contents, SodaData sodaData) {
    }
}
