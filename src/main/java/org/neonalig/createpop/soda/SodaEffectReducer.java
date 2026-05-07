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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SodaEffectReducer {
    public static final float BASE_INSTABILITY = 0.1f;
    public static final float POSITIVE_MIX_INSTABILITY = 0.18f;
    public static final float INSTABILITY_THRESHOLD = 0.8f;
    public static final float SAFE_INSTABILITY = 0.35f;
    public static final float REACTION_AFFINITY_THRESHOLD = 0.30f;
    public static final int DEFAULT_DURATION = 20 * 60 * 4;
    private static final int MIN_EFFECT_DURATION = 20 * 8;

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
        return mix(first, second, 1, 1, worldSeed);
    }

    public static SodaData mix(SodaData first, SodaData second, int firstAmount, int secondAmount, long worldSeed) {
        List<MobEffectInstance> combined = mergeWithDilution(first.effects(), second.effects(), firstAmount, secondAmount);

        Reduction reduction = resolveMix(combined, worldSeed);
        float instability = first.instability() + second.instability() + reduction.positiveReductions() * POSITIVE_MIX_INSTABILITY;
        List<MobEffectInstance> resolved = reduction.effects();

        if (instability > INSTABILITY_THRESHOLD) {
            resolved = new ArrayList<>(resolved);
            resolved.add(deterministicNegativeEffect(resolved, worldSeed));
            instability = SAFE_INSTABILITY;
        }

        resolved.sort(Comparator.comparing(SodaEffectReducer::effectId));
        return new SodaData(resolved, weightedAverageColor(first.color(), second.color(), firstAmount, secondAmount), instability);
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

    private static Reduction resolveMix(List<MobEffectInstance> input, long worldSeed) {
        List<MobEffectInstance> effects = coalesce(input);
        int positiveReductions = 0;
        Set<String> resolvedPairs = new HashSet<>();

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
                    if (!resolvedPairs.add(pairKey)) {
                        continue;
                    }

                    Optional<List<MobEffectInstance>> reaction = resolveReaction(first, second, worldSeed);
                    if (reaction.isEmpty()) {
                        continue;
                    }

                    effects.remove(j);
                    effects.remove(i);
                    effects.addAll(copyEffects(reaction.get()));
                    effects = coalesce(effects);
                    positiveReductions++;
                    changed = true;
                    break;
                }
            }
        } while (changed);

        return new Reduction(effects, positiveReductions);
    }

    private static List<MobEffectInstance> mergeWithDilution(List<MobEffectInstance> firstEffects, List<MobEffectInstance> secondEffects, int firstAmount, int secondAmount) {
        int leftAmount = Math.max(1, firstAmount);
        int rightAmount = Math.max(1, secondAmount);
        int totalAmount = leftAmount + rightAmount;

        Map<String, MobEffectInstance> left = indexById(coalesce(copyEffects(firstEffects)));
        Map<String, MobEffectInstance> right = indexById(coalesce(copyEffects(secondEffects)));
        Set<String> allIds = new HashSet<>();
        allIds.addAll(left.keySet());
        allIds.addAll(right.keySet());

        List<MobEffectInstance> merged = new ArrayList<>();
        for (String id : allIds) {
            MobEffectInstance leftEffect = left.get(id);
            MobEffectInstance rightEffect = right.get(id);
            MobEffectInstance candidate;
            if (leftEffect != null && rightEffect != null) {
                candidate = combineMatchingEffects(leftEffect, rightEffect, leftAmount, rightAmount, totalAmount);
            } else {
                MobEffectInstance source = leftEffect != null ? leftEffect : rightEffect;
                int sourceAmount = leftEffect != null ? leftAmount : rightAmount;
                candidate = diluteEffect(source, sourceAmount, totalAmount);
            }

            if (candidate != null && candidate.getDuration() >= MIN_EFFECT_DURATION) {
                merged.add(candidate);
            }
        }

        return coalesce(merged);
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

    private static Optional<List<MobEffectInstance>> resolveReaction(MobEffectInstance first, MobEffectInstance second, long worldSeed) {
        long comboHash = hashPair(first, second, worldSeed);
        RandomSource random = RandomSource.create(comboHash);
        float affinity = random.nextFloat();

        if (affinity > REACTION_AFFINITY_THRESHOLD) {
            return Optional.empty();
        }

        List<Holder<MobEffect>> validOutcomes = validOutcomes(first, second);
        if (validOutcomes.isEmpty()) {
            return Optional.empty();
        }

        // Total "potion-time" available from both consumed effects.
        int totalDuration = first.getDuration() + second.getDuration();
        int baseAmplifier = Math.max(first.getAmplifier(), second.getAmplifier());

        int reactionType = random.nextInt(3);
        return switch (reactionType) {
            // Fusion: both effects consumed, all their time goes to one new effect.
            case 0 -> Optional.of(List.of(randomOutcome(random, validOutcomes, totalDuration, baseAmplifier)));
            // Fission: both effects consumed, total time split evenly between two new effects.
            case 1 -> Optional.of(fissionOutcomes(random, validOutcomes, totalDuration, baseAmplifier));
            // Catalysis: first effect kept with its own time, second is consumed and its time passes to the new effect.
            default -> Optional.of(List.of(new MobEffectInstance(first), randomOutcome(random, validOutcomes, second.getDuration(), baseAmplifier)));
        };
    }

    private static MobEffectInstance randomOutcome(RandomSource random, List<Holder<MobEffect>> outcomes, int duration, int baseAmplifier) {
        Holder<MobEffect> effect = outcomes.get(random.nextInt(outcomes.size()));
        int finalDuration = Math.max(MIN_EFFECT_DURATION, duration);
        int amplifier = Math.min(3, baseAmplifier + (random.nextFloat() < 0.35f ? 1 : 0));
        return new MobEffectInstance(effect, finalDuration, amplifier);
    }

    private static List<MobEffectInstance> fissionOutcomes(RandomSource random, List<Holder<MobEffect>> outcomes, int totalDuration, int baseAmplifier) {
        int splitDuration = Math.max(MIN_EFFECT_DURATION, totalDuration / 2);
        MobEffectInstance primary = randomOutcome(random, outcomes, splitDuration, baseAmplifier);
        List<Holder<MobEffect>> byproductPool = outcomes.stream()
                .filter(holder -> !holder.equals(primary.getEffect()))
                .toList();
        MobEffectInstance byproduct = randomOutcome(
                random,
                byproductPool.isEmpty() ? outcomes : byproductPool,
                splitDuration,
                baseAmplifier
        );
        return List.of(primary, byproduct);
    }

    private static List<Holder<MobEffect>> validOutcomes(MobEffectInstance first, MobEffectInstance second) {
        String firstId = effectId(first);
        String secondId = effectId(second);
        return HIGHER_TIER_POOL.stream()
                .filter(holder -> {
                    ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(holder.value());
                    if (id == null) {
                        return false;
                    }
                    String key = id.toString();
                    return !key.equals(firstId) && !key.equals(secondId);
                })
                .toList();
    }

    private static MobEffectInstance combineMatchingEffects(MobEffectInstance first, MobEffectInstance second, int firstAmount, int secondAmount, int totalAmount) {
        // Same effect: carry over both durations fully. Instability already handles the risk of repeated stacking.
        int duration = first.getDuration() + second.getDuration();

        int amplifier = weightedAverage(first.getAmplifier(), second.getAmplifier(), firstAmount, secondAmount, totalAmount);
        return new MobEffectInstance(
                first.getEffect(),
                duration,
                amplifier,
                first.isAmbient() || second.isAmbient(),
                first.isVisible() || second.isVisible(),
                first.showIcon() || second.showIcon()
        );
    }

    private static MobEffectInstance diluteEffect(MobEffectInstance source, int sourceAmount, int totalAmount) {
        float ratio = sourceAmount / (float) totalAmount;
        int duration = Math.round(source.getDuration() * ratio);
        int amplifier = source.getAmplifier();

        if (ratio < 0.5f && amplifier > 0) {
            amplifier--;
        }
        if (ratio < 0.25f && amplifier > 0) {
            amplifier--;
        }

        if (duration < MIN_EFFECT_DURATION) {
            return null;
        }

        return new MobEffectInstance(
                source.getEffect(),
                duration,
                amplifier,
                source.isAmbient(),
                source.isVisible(),
                source.showIcon()
        );
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
        return Objects.hash(worldSeed, sortedId(first, second, 0), sortedId(first, second, 1));
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

    private static Map<String, MobEffectInstance> indexById(List<MobEffectInstance> effects) {
        Map<String, MobEffectInstance> byId = new HashMap<>();
        for (MobEffectInstance effect : effects) {
            byId.put(effectId(effect), new MobEffectInstance(effect));
        }
        return byId;
    }

    private static int weightedAverage(int first, int second, int firstWeight, int secondWeight, int totalWeight) {
        if (totalWeight <= 0) {
            return Math.max(first, second);
        }
        return Math.round(((first * firstWeight) + (second * secondWeight)) / (float) totalWeight);
    }

    private static int weightedAverageColor(int first, int second, int firstWeight, int secondWeight) {
        int totalWeight = Math.max(1, firstWeight + secondWeight);
        int r = ((((first >> 16) & 0xFF) * firstWeight) + (((second >> 16) & 0xFF) * secondWeight)) / totalWeight;
        int g = ((((first >> 8) & 0xFF) * firstWeight) + (((second >> 8) & 0xFF) * secondWeight)) / totalWeight;
        int b = (((first & 0xFF) * firstWeight) + ((second & 0xFF) * secondWeight)) / totalWeight;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private record Reduction(List<MobEffectInstance> effects, int positiveReductions) {
    }
}

