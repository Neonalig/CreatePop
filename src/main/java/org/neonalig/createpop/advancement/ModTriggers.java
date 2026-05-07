package org.neonalig.createpop.advancement;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.neonalig.createpop.CreatePop;

public final class ModTriggers {
    /** Fires when a player has any soda bottle or soda bucket in their inventory. */
    public static final SimpleFiringTrigger OBTAINED_SODA = new SimpleFiringTrigger();

    /** Fires when a player drinks a soda bottle. */
    public static final SimpleFiringTrigger DRANK_SODA = new SimpleFiringTrigger();

    /** Fires when a player holds a soda with two or more distinct effects (compound mix). */
    public static final SimpleFiringTrigger OBTAINED_COMPOUND_SODA = new SimpleFiringTrigger();

    /** Fires when a player holds a soda that contains at least one negative effect. */
    public static final SimpleFiringTrigger OBTAINED_NEGATIVE_SODA = new SimpleFiringTrigger();

    /** Fires when a player pours a soda bucket onto the ground (destabilising it). */
    public static final SimpleFiringTrigger POURED_SODA_BUCKET = new SimpleFiringTrigger();

    /** Fires when a player explicitly names (or renames) a soda recipe. */
    public static final SimpleFiringTrigger NAMED_SODA = new SimpleFiringTrigger();

    /** Fires when a player holds a soda with exactly 0% instability (perfect stabilisation). */
    public static final SimpleFiringTrigger OBTAINED_PERFECT_SODA = new SimpleFiringTrigger();

    /**
     * Fires when a player holds a soda that was stabilised using stripped acacia log
     * (gum arabic – the original real-world stabiliser reference).
     */
    public static final SimpleFiringTrigger STABILISED_WITH_ACACIA = new SimpleFiringTrigger();

    /**
     * Fires when a player holds a soda that was fully purified using an amethyst shard
     * (industrial crystalline lattice vitrification).
     */
    public static final SimpleFiringTrigger STABILISED_WITH_AMETHYST = new SimpleFiringTrigger();

    /** Fires when a player holds a soda that was stabilised using magma cream (emulsifier). */
    public static final SimpleFiringTrigger STABILISED_WITH_MAGMA_CREAM = new SimpleFiringTrigger();

    private ModTriggers() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ModTriggers::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(Registries.TRIGGER_TYPE, helper -> {
            id(helper, "obtained_soda", OBTAINED_SODA);
            id(helper, "drank_soda", DRANK_SODA);
            id(helper, "obtained_compound_soda", OBTAINED_COMPOUND_SODA);
            id(helper, "obtained_negative_soda", OBTAINED_NEGATIVE_SODA);
            id(helper, "poured_soda_bucket", POURED_SODA_BUCKET);
            id(helper, "named_soda", NAMED_SODA);
            id(helper, "obtained_perfect_soda", OBTAINED_PERFECT_SODA);
            id(helper, "stabilised_with_acacia", STABILISED_WITH_ACACIA);
            id(helper, "stabilised_with_amethyst", STABILISED_WITH_AMETHYST);
            id(helper, "stabilised_with_magma_cream", STABILISED_WITH_MAGMA_CREAM);
        });
    }

    private static void id(RegisterEvent.RegisterHelper<CriterionTrigger<?>> helper, String path, SimpleFiringTrigger trigger) {
        helper.register(ResourceLocation.fromNamespaceAndPath(CreatePop.MODID, path), trigger);
    }
}
