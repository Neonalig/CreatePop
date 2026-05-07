package org.neonalig.createpop;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CreatePopConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_JEI_POTION_HINTS = BUILDER
            .comment("Enable JEI potion reaction hint recipes (Requires JEI).")
            .define("enableJeiPotionHints", true);

    public static final ModConfigSpec CLIENT_SPEC = BUILDER.build();

    private CreatePopConfig() {
    }

    public static boolean enableJeiPotionHints() {
        return ENABLE_JEI_POTION_HINTS.get();
    }
}

