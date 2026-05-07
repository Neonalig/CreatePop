package org.neonalig.createpop.advancement;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * A reusable simple criterion trigger that fires with no context payload.
 * Advancement JSON just needs the trigger ID; no extra conditions are evaluated.
 */
public class SimpleFiringTrigger extends SimpleCriterionTrigger<SimpleFiringTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** Fire the trigger for the given player. */
    public void trigger(ServerPlayer player) {
        this.trigger(player, inst -> true);
    }

    public static final class TriggerInstance implements SimpleCriterionTrigger.SimpleInstance {
        public static final TriggerInstance INSTANCE = new TriggerInstance();
        public static final Codec<TriggerInstance> CODEC = Codec.unit(INSTANCE);

        @Override
        public Optional<ContextAwarePredicate> player() {
            return Optional.empty();
        }
    }
}
