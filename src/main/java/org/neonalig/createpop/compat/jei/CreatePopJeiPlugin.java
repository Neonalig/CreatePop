package org.neonalig.createpop.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.registry.ModFluids;

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

    /** Captured during {@link #registerFluidSubtypes} for use in alias registration. */
    private IPlatformFluidHelper<?> platformFluidHelper;

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

    /**
     * Helper that captures the wildcard {@code T} so the generic {@link IIngredientAliasRegistration}
     * calls remain type-safe at the call site.
     */
    @SuppressWarnings("unchecked")
    private static <T> void addFluidAliases(IIngredientAliasRegistration registration,
                                             IPlatformFluidHelper<T> helper) {
        IIngredientType<T> fluidType = (IIngredientType<T>) helper.getFluidIngredientType();

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
}
