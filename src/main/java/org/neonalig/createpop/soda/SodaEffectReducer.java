package org.neonalig.createpop.soda;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.neonalig.createpop.component.SodaData;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SodaEffectReducer {
    public static final float BASE_INSTABILITY = 0.1f;
    public static final float POSITIVE_MIX_INSTABILITY = 0.18f;
    public static final float INSTABILITY_THRESHOLD = 0.8f;
    public static final float SAFE_INSTABILITY = 0.35f;
    public static final int DEFAULT_DURATION = 20 * 60 * 4;

    private static final List<Holder<MobEffect>> HIGHER_TIER_POOL = List.of(
            MobEffects.REGENERATION,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.WATER_BREATHING,
            MobEffects.NIGHT_VISION,
            MobEffects.HEALTH_BOOST,
            MobEffects.ABSORPTION,
            MobEffects.SLOW_FALLING,
            MobEffects.CONDUIT_POWER,
            MobEffects.DOLPHINS_GRACE
    );

    private static final List<Holder<MobEffect>> NEGATIVE_POOL = List.of(
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.WEAKNESS,
            MobEffects.CONFUSION,
            MobEffects.POISON,
            MobEffects.DIG_SLOWDOWN
    );

    private SodaEffectReducer() {
    }

    public static SodaData baseFromPotion(List<MobEffectInstance> effects, int color) {
        return new SodaData(copyEffects(effects), color, BASE_INSTABILITY);
    }

    public static SodaData mix(SodaData first, SodaData second, long worldSeed) {
        List<MobEffectInstance> combined = new ArrayList<>();
        combined.addAll(copyEffects(first.effects()));
        combined.addAll(copyEffects(second.effects()));

        Reduction reduction = reduce(combined, worldSeed);
        float instability = first.instability() + second.instability() + reduction.positiveReductions() * POSITIVE_MIX_INSTABILITY;
        List<MobEffectInstance> resolved = reduction.effects();

        if (instability > INSTABILITY_THRESHOLD) {
            resolved = new ArrayList<>(resolved);
            resolved.add(deterministicNegativeEffect(resolved, worldSeed));
            instability = SAFE_INSTABILITY;
        }

        resolved.sort(Comparator.comparing(SodaEffectReducer::effectId));
        return new SodaData(resolved, SodaData.averageColor(first.color(), second.color()), instability);
    }

    public static boolean isPositive(MobEffectInstance effect) {
        return effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
    }

    public static List<MobEffectInstance> copyEffects(Iterable<MobEffectInstance> effects) {
        List<MobEffectInstance> copy = new ArrayList<>();
        for (MobEffectInstance effect : effects) {
            copy.add(new MobEffectInstance(effect));
        }
        return copy;
    }

    private static Reduction reduce(List<MobEffectInstance> input, long worldSeed) {
        List<MobEffectInstance> effects = coalesce(input);
        int positiveReductions = 0;
        Set<String> seenPairs = new HashSet<>();

        boolean changed;
        do {
            changed = false;
            effects.sort(Comparator.comparing(SodaEffectReducer::effectId));

            for (int i = 0; i < effects.size() && !changed; i++) {
                MobEffectInstance first = effects.get(i);
                if (!isPositive(first)) {
                    continue;
                }

                for (int j = i + 1; j < effects.size(); j++) {
                    MobEffectInstance second = effects.get(j);
                    if (!isPositive(second) || effectId(first).equals(effectId(second))) {
                        continue;
                    }

                    String pairKey = pairKey(first, second);
                    if (!seenPairs.add(pairKey)) {
                        continue;
                    }

                    MobEffectInstance result = deterministicPositiveEffect(first, second, worldSeed);
                    effects.remove(j);
                    effects.remove(i);
                    effects.add(result);
                    effects = coalesce(effects);
                    positiveReductions++;
                    changed = true;
                    break;
                }
            }
        } while (changed);

        return new Reduction(effects, positiveReductions);
    }

    private static List<MobEffectInstance> coalesce(List<MobEffectInstance> effects) {
        List<MobEffectInstance> result = new ArrayList<>();
        for (MobEffectInstance next : effects) {
            Optional<MobEffectInstance> existing = result.stream()
                    .filter(effect -> effect.getEffect().equals(next.getEffect()))
                    .findFirst();
            if (existing.isPresent()) {
                existing.get().update(new MobEffectInstance(next));
            } else {
                result.add(new MobEffectInstance(next));
            }
        }
        return result;
    }

    private static MobEffectInstance deterministicPositiveEffect(MobEffectInstance first, MobEffectInstance second, long worldSeed) {
        RandomSource random = RandomSource.create(hashPair(first, second, worldSeed));
        Holder<MobEffect> effect = HIGHER_TIER_POOL.get(random.nextInt(HIGHER_TIER_POOL.size()));
        int duration = Math.max(Math.max(first.getDuration(), second.getDuration()), DEFAULT_DURATION);
        int amplifier = Math.min(2, Math.max(first.getAmplifier(), second.getAmplifier()) + (random.nextBoolean() ? 1 : 0));
        return new MobEffectInstance(effect, duration, amplifier);
    }

    private static MobEffectInstance deterministicNegativeEffect(List<MobEffectInstance> effects, long worldSeed) {
        long hash = worldSeed ^ 0x5DEECE66DL;
        for (MobEffectInstance effect : effects) {
            hash = mixHash(hash, effectId(effect));
        }
        RandomSource random = RandomSource.create(hash);
        Holder<MobEffect> effect = NEGATIVE_POOL.get(random.nextInt(NEGATIVE_POOL.size()));
        return new MobEffectInstance(effect, DEFAULT_DURATION / 2, random.nextInt(2));
    }

    private static long hashPair(MobEffectInstance first, MobEffectInstance second, long worldSeed) {
        return mixHash(mixHash(worldSeed, sortedId(first, second, 0)), sortedId(first, second, 1));
    }

    private static String pairKey(MobEffectInstance first, MobEffectInstance second) {
        return sortedId(first, second, 0) + "+" + sortedId(first, second, 1);
    }

    private static String sortedId(MobEffectInstance first, MobEffectInstance second, int index) {
        String firstId = effectId(first);
        String secondId = effectId(second);
        if (firstId.compareTo(secondId) <= 0) {
            return index == 0 ? firstId : secondId;
        }
        return index == 0 ? secondId : firstId;
    }

    public static String effectId(MobEffectInstance effect) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        return id == null ? "unknown" : id.toString();
    }

    private static long mixHash(long seed, String value) {
        long hash = seed;
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= b;
            hash *= 0x100000001B3L;
        }
        return hash;
    }

    private record Reduction(List<MobEffectInstance> effects, int positiveReductions) {
    }
}

