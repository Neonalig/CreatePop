package org.neonalig.createpop.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import org.neonalig.createpop.CreatePopConfig;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.compat.create.DynamicSodaMixing;
import org.neonalig.createpop.registry.ModDataComponents;
import org.neonalig.createpop.registry.ModFluids;
import org.neonalig.createpop.soda.SodaEffectReducer;
import org.neonalig.createpop.soda.SodaFluidStackHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
    private final List<Object> runtimeHintSodaIngredients = new ArrayList<>();
    private Boolean lastHintsEnabled;

    public CreatePopJeiPlugin() {
        NeoForge.EVENT_BUS.addListener(this::onClientLogin);
        NeoForge.EVENT_BUS.addListener(this::onClientLogout);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public <T> void registerFluidSubtypes(ISubtypeRegistration registration, IPlatformFluidHelper<T> helper) {
        this.platformFluidHelper = helper;
        registration.registerSubtypeInterpreter(ModFluids.SODA_BOTTLE.get(), sodaItemSubtypeInterpreter());
        registration.registerSubtypeInterpreter(ModFluids.SODA_BUCKET.get(), sodaItemSubtypeInterpreter());
        // Intentionally do not subtype the soda fluid ingredient in JEI.
        // This keeps hint recipes discoverable from the main soda focus instead of
        // filtering by exact instability/effect component payload.
    }

    private static ISubtypeInterpreter<ItemStack> sodaItemSubtypeInterpreter() {
        return new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return sodaSubtypeKey(SodaFluidStackHelper.getSodaData(stack));
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return sodaSubtypeKey(SodaFluidStackHelper.getSodaData(stack));
            }
        };
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        if (platformFluidHelper != null) {
            addFluidAliases(registration, platformFluidHelper);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MIXING_TYPE, buildStabilisationHintRecipes());
        registration.addRecipes(MIXING_TYPE, buildPotionBaseRecipes(collectPotionBases()));
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        if (platformFluidHelper != null) {
            addBaseSodaIngredients(registration, platformFluidHelper);
            addStabilisationDemoIngredients(registration, platformFluidHelper);
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.jeiRuntime = jeiRuntime;
        refreshRuntimeHintSodaIngredients();
        refreshRuntimeRecipes();
    }

    @Override
    public void onRuntimeUnavailable() {
        clearRuntimeHintSodaIngredients();
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
                List.of("Soda", "soft drink", "pop", "fizzy drink", "alchemical soda"));
    }

    private static <T> void addBaseSodaIngredients(IExtraIngredientRegistration registration,
                                                    IPlatformFluidHelper<T> helper) {
        List<PotionBaseData> potionBases = collectPotionBases();
        Map<String, SodaData> unique = new LinkedHashMap<>();

        addSodaVariant(unique, SodaData.EMPTY);
        for (PotionBaseData base : potionBases) {
            addSodaVariant(unique, base.sodaData());
        }

        List<T> extras = new ArrayList<>();
        long listVolume = helper.bucketVolume();
        for (SodaData data : unique.values()) {
            DataComponentPatch patch = DataComponentPatch.builder()
                    .set(ModDataComponents.SODA_DATA.get(), data)
                    .build();
            extras.add(helper.create(
                    BuiltInRegistries.FLUID.wrapAsHolder(ModFluids.SODA.get()),
                    listVolume,
                    patch
            ));
        }

        registration.addExtraIngredients(helper.getFluidIngredientType(), extras);
    }

    private static void addSodaVariant(Map<String, SodaData> unique, SodaData data) {
        unique.putIfAbsent(sodaSubtypeKey(data), data);
    }

    private void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        refreshRuntimeRecipes();
    }

    private void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearRuntimeHintSodaIngredients();
        clearInjectedRecipes();
    }

    private void onClientTick(ClientTickEvent.Post event) {
        boolean hintsEnabled = isHintsEnabledSafe();
        if (lastHintsEnabled != null && hintsEnabled == lastHintsEnabled) {
            return;
        }
        lastHintsEnabled = hintsEnabled;
        refreshRuntimeHintSodaIngredients();
        refreshRuntimeRecipes();
    }


    private void refreshRuntimeRecipes() {
        if (jeiRuntime == null) {
            return;
        }

        clearInjectedRecipes();

        if (!isHintsEnabledSafe()) {
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

    private static boolean isHintsEnabledSafe() {
        try {
            return CreatePopConfig.enableJeiPotionHints();
        } catch (IllegalStateException ignored) {
            // JEI/plugin setup can run before config load; defer toggling until config is ready.
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshRuntimeHintSodaIngredients() {
        if (jeiRuntime == null || platformFluidHelper == null) {
            return;
        }

        IPlatformFluidHelper<Object> helper = (IPlatformFluidHelper<Object>) platformFluidHelper;
        clearRuntimeHintSodaIngredients();

        if (!isHintsEnabledSafe()) {
            return;
        }

        long seed = resolveClientSeed();
        List<Object> extras = new ArrayList<>(buildHintSodaIngredients(helper, seed));
        if (extras.isEmpty()) {
            return;
        }

        jeiRuntime.getIngredientManager().addIngredientsAtRuntime(helper.getFluidIngredientType(), extras);
        runtimeHintSodaIngredients.addAll(extras);
    }

    @SuppressWarnings("unchecked")
    private void clearRuntimeHintSodaIngredients() {
        if (jeiRuntime == null || platformFluidHelper == null || runtimeHintSodaIngredients.isEmpty()) {
            return;
        }

        IPlatformFluidHelper<Object> helper = (IPlatformFluidHelper<Object>) platformFluidHelper;
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(helper.getFluidIngredientType(), runtimeHintSodaIngredients);
        runtimeHintSodaIngredients.clear();
    }

    private static <T> List<T> buildHintSodaIngredients(IPlatformFluidHelper<T> helper, long worldSeed) {
        List<PotionBaseData> potionBases = collectPotionBases();
        Map<String, SodaData> unique = new LinkedHashMap<>();

        for (int i = 0; i < potionBases.size(); i++) {
            for (int j = i + 1; j < potionBases.size(); j++) {
                SodaData mixed = SodaEffectReducer.mix(
                        potionBases.get(i).sodaData(),
                        potionBases.get(j).sodaData(),
                        DynamicSodaMixing.DRINK_AMOUNT,
                        DynamicSodaMixing.DRINK_AMOUNT,
                        worldSeed
                );
                addSodaVariant(unique, mixed);
            }
        }

        List<T> extras = new ArrayList<>();
        long listVolume = helper.bucketVolume();
        for (SodaData data : unique.values()) {
            DataComponentPatch patch = DataComponentPatch.builder()
                    .set(ModDataComponents.SODA_DATA.get(), data)
                    .build();
            extras.add(helper.create(
                    BuiltInRegistries.FLUID.wrapAsHolder(ModFluids.SODA.get()),
                    listVolume,
                    patch
            ));
        }
        return extras;
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
            FluidStack output = sodaForJei(base.sodaData());
            SizedFluidIngredient carbonated = exactFluid(SodaFluidStackHelper.carbonatedWater(DynamicSodaMixing.DRINK_AMOUNT));

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
            FluidStack firstSoda = sodaForJei(first.sodaData());

            for (int j = i + 1; j < potionBases.size(); j++) {
                PotionBaseData second = potionBases.get(j);
                SodaData outputData = SodaEffectReducer.mix(first.sodaData(), second.sodaData(), DynamicSodaMixing.DRINK_AMOUNT, DynamicSodaMixing.DRINK_AMOUNT, worldSeed);
                FluidStack output = sodaForJei(outputData);

                // Show as: soda(A) + potion_item(B) → soda(A+B).
                // The first input is always a soda — you can't mix two raw potions; you need an existing soda base.
                String key = first.id().getPath() + "__" + second.id().getPath();
                recipes.add(recipeHolder(
                        ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "jei/hint/" + key),
                        output,
                        List.of(exactFluid(firstSoda)),
                        Ingredient.of(second.potionItem())
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

    private static RecipeHolder<BasinRecipe> recipeHolder(ResourceLocation id, FluidStack output, List<SizedFluidIngredient> fluids, HeatCondition heat, Ingredient... items) {
        NonNullList<SizedFluidIngredient> ingredients = NonNullList.create();
        ingredients.addAll(fluids);

        StandardProcessingRecipe.Builder<MixingRecipe> builder = new StandardProcessingRecipe.Builder<>(MixingRecipe::new, id)
                .withFluidIngredients(ingredients)
                .withFluidOutputs(output)
                .duration(100);

        builder.requiresHeat(heat);

        for (Ingredient item : items) {
            builder.require(item);
        }

        return new RecipeHolder<>(id, builder.build());
    }

    private static List<RecipeHolder<BasinRecipe>> buildStabilisationHintRecipes() {
        float demoInstability = 0.40f;
        SodaData demoInput = new SodaData(List.of(), SodaData.DEFAULT_COLOR, demoInstability);
        FluidStack unstable = sodaForJei(demoInput);

        List<RecipeHolder<BasinRecipe>> recipes = new ArrayList<>();

        double acaciaReduction = CreatePopConfig.acaciaLogInstabilityReduction();
        if (acaciaReduction > 0.0) {
            SodaData output = new SodaData(List.of(), SodaData.DEFAULT_COLOR, demoInstability * (1f - (float) acaciaReduction));
            recipes.add(recipeHolder(
                    ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "jei/00_stabilise/acacia_log"),
                    sodaForJei(output),
                    List.of(exactFluid(unstable)),
                    HeatCondition.HEATED,
                    Ingredient.of(Items.STRIPPED_ACACIA_LOG)
            ));
        }

        double magmaReduction = CreatePopConfig.magmaCreamInstabilityReduction();
        if (magmaReduction > 0.0) {
            SodaData output = new SodaData(List.of(), SodaData.DEFAULT_COLOR, demoInstability * (1f - (float) magmaReduction));
            recipes.add(recipeHolder(
                    ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "jei/00_stabilise/magma_cream"),
                    sodaForJei(output),
                    List.of(exactFluid(unstable)),
                    HeatCondition.HEATED,
                    Ingredient.of(Items.MAGMA_CREAM)
            ));
        }

        double amethystReduction = CreatePopConfig.amethystShardInstabilityReduction();
        if (amethystReduction > 0.0) {
            SodaData output = new SodaData(List.of(), SodaData.DEFAULT_COLOR, demoInstability * (1f - (float) amethystReduction));
            recipes.add(recipeHolder(
                    ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, "jei/00_stabilise/amethyst_shard"),
                    sodaForJei(output),
                    List.of(exactFluid(unstable)),
                    HeatCondition.SUPERHEATED,
                    Ingredient.of(Items.AMETHYST_SHARD)
            ));
        }

        return recipes;
    }

    private static <T> void addStabilisationDemoIngredients(IExtraIngredientRegistration registration,
                                                             IPlatformFluidHelper<T> helper) {
        float demoInstability = 0.40f;
        Map<String, SodaData> unique = new LinkedHashMap<>();

        SodaData inputDemo = new SodaData(List.of(), SodaData.DEFAULT_COLOR, demoInstability);
        addSodaVariant(unique, inputDemo);

        for (double reduction : List.of(
                CreatePopConfig.acaciaLogInstabilityReduction(),
                CreatePopConfig.magmaCreamInstabilityReduction(),
                CreatePopConfig.amethystShardInstabilityReduction()
        )) {
            if (reduction > 0.0) {
                addSodaVariant(unique, new SodaData(List.of(), SodaData.DEFAULT_COLOR, demoInstability * (1f - (float) reduction)));
            }
        }

        long listVolume = helper.bucketVolume();
        List<T> extras = new ArrayList<>();
        for (SodaData data : unique.values()) {
            DataComponentPatch patch = DataComponentPatch.builder()
                    .set(ModDataComponents.SODA_DATA.get(), data)
                    .build();
            extras.add(helper.create(
                    BuiltInRegistries.FLUID.wrapAsHolder(ModFluids.SODA.get()),
                    listVolume,
                    patch
            ));
        }
        registration.addExtraIngredients(helper.getFluidIngredientType(), extras);
    }

    private static String sodaSubtypeKey(SodaData data) {
        StringBuilder key = new StringBuilder();
        key.append("c=").append(data.color())
                .append(";i=").append(data.instability())
                .append(";e=");
        for (MobEffectInstance effect : SodaEffectReducer.copyEffects(data.effects())) {
            key.append(SodaEffectReducer.effectId(effect))
                    .append('@').append(effect.getAmplifier())
                    .append('@').append(effect.getDuration())
                    .append('|');
        }
        return key.toString();
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

    private static FluidStack sodaForJei(SodaData data) {
        FluidStack soda = SodaFluidStackHelper.soda(DynamicSodaMixing.DRINK_AMOUNT, data);
        Component effectsSummary = buildEffectSummary(data.effects());
        Component instabilityText = Component.translatable(
                "createpop.soda.tooltip.instability",
                String.format(Locale.ROOT, "%.2f", data.instability())
        ).withColor(0xFFB347); // amber/orange

        MutableComponent customName = Component.translatable("fluid_type.createpop.soda")
                .append(Component.literal(" ("))
                .append(effectsSummary)
                .append(Component.literal(", "))
                .append(instabilityText)
                .append(Component.literal(")"));
        soda.set(DataComponents.CUSTOM_NAME, customName);
        // Attach potion-like tooltip data so JEI fluid tooltips expose effects for preview stacks.
        PotionContents tooltipContents = new PotionContents(Optional.empty(), Optional.of(data.color() & 0x00FFFFFF), SodaEffectReducer.copyEffects(data.effects()));
        soda.set(DataComponents.POTION_CONTENTS, tooltipContents);
        return soda;
    }

    private static Component buildEffectSummary(List<MobEffectInstance> effects) {
        if (effects.isEmpty()) {
            return Component.translatable("createpop.soda.tooltip.no_effects");
        }

        List<MobEffectInstance> sorted = SodaEffectReducer.copyEffects(effects);
        sorted.sort(java.util.Comparator.comparing(SodaEffectReducer::effectId));

        MutableComponent summary = Component.empty();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                summary.append(Component.literal(", "));
            }
            summary.append(formatEffectName(sorted.get(i)));
        }
        return summary;
    }

    private static Component formatEffectName(MobEffectInstance effect) {
        MutableComponent effectText = Component.translatable(effect.getDescriptionId());
        if (effect.getAmplifier() > 0) {
            effectText = Component.translatable(
                    "potion.withAmplifier",
                    effectText,
                    Component.translatable("potion.potency." + effect.getAmplifier())
            );
        }
        MobEffectCategory category = effect.getEffect().value().getCategory();
        return effectText.withStyle(category.getTooltipFormatting());
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
