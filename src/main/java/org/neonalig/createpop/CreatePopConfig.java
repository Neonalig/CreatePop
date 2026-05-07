package org.neonalig.createpop;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CreatePopConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue FORCE_UNLOCK_ALL_JEI_SODA_RECIPES = BUILDER
            .comment("Force unlock all JEI soda reaction hint recipes (cheat/debug option, Requires JEI).")
            .define("forceUnlockAllJeiSodaRecipes", false);

    public static final ModConfigSpec CLIENT_SPEC = BUILDER.build();

    // --- Common (synced) config ---
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue ACACIA_LOG_INSTABILITY_REDUCTION = COMMON_BUILDER
            .comment("Absolute instability removed when mixing soda with a stripped acacia log in a heated mixer.",
                     "Range: 0.0 (no reduction) to 1.0 (full removal). Default: 0.05.")
            .defineInRange("acaciaLogInstabilityReduction", 0.05, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue MAGMA_CREAM_INSTABILITY_REDUCTION = COMMON_BUILDER
            .comment("Absolute instability removed when mixing soda with magma cream in a heated mixer.",
                     "Range: 0.0 (no reduction) to 1.0 (full removal). Default: 0.45.")
            .defineInRange("magmaCreamInstabilityReduction", 0.45, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue AMETHYST_SHARD_INSTABILITY_REDUCTION = COMMON_BUILDER
            .comment("Absolute instability removed when mixing soda with an amethyst shard in a superheated mixer.",
                     "Range: 0.0 (no reduction) to 1.0 (full removal). Default: 1.0.")
            .defineInRange("amethystShardInstabilityReduction", 1.0, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue BASE_POTION_INSTABILITY_GAIN = COMMON_BUILDER
            .comment("Instability assigned when creating a soda base from carbonated water + potion.",
                     "Higher values make instability ramp sooner across remix chains.",
                     "Range: 0.0 to 2.0. Default: 0.18.")
            .defineInRange("basePotionInstabilityGain", 0.18, 0.0, 2.0);

    public static final ModConfigSpec.DoubleValue MIX_REACTION_INSTABILITY_GAIN = COMMON_BUILDER
            .comment("Extra instability added per positive reaction resolved during soda remixing.",
                     "Higher values make negative side effects appear after fewer mixes.",
                     "Range: 0.0 to 2.0. Default: 0.30.")
            .defineInRange("mixReactionInstabilityGain", 0.30, 0.0, 2.0);

    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    private CreatePopConfig() {
    }

    public static boolean forceUnlockAllJeiSodaRecipes() {
        return FORCE_UNLOCK_ALL_JEI_SODA_RECIPES.get();
    }

    public static double acaciaLogInstabilityReduction() {
        return ACACIA_LOG_INSTABILITY_REDUCTION.get();
    }

    public static double magmaCreamInstabilityReduction() {
        return MAGMA_CREAM_INSTABILITY_REDUCTION.get();
    }

    public static double amethystShardInstabilityReduction() {
        return AMETHYST_SHARD_INSTABILITY_REDUCTION.get();
    }

    public static double basePotionInstabilityGain() {
        return BASE_POTION_INSTABILITY_GAIN.get();
    }

    public static double mixReactionInstabilityGain() {
        return MIX_REACTION_INSTABILITY_GAIN.get();
    }
}

