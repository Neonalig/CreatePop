package org.neonalig.createpop.soda;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.PotionContents;
import org.neonalig.createpop.CreatePopConfig;
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
    public static final float DEFAULT_BASE_INSTABILITY_GAIN = 0.24f;
    public static final float DEFAULT_MIX_REACTION_INSTABILITY_GAIN = 0.45f;
    public static final float DEFAULT_MIX_FLAT_INSTABILITY_GAIN = 0.12f;
    public static final float DEFAULT_INSTABILITY_THRESHOLD = 0.70f;
    public static final float DEFAULT_SAFE_INSTABILITY_AFTER_BACKFIRE = 0.45f;
    public static final float DEFAULT_REACTION_AFFINITY_THRESHOLD = 0.45f;
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
            MobEffects.DIG_SLOWDOWN,
            MobEffects.HARM,
            MobEffects.WITHER,
            MobEffects.HUNGER,
            MobEffects.BLINDNESS,
            MobEffects.UNLUCK,
            MobEffects.DARKNESS
    );

    private static final List<Holder<MobEffect>> POSITIVE_PURIFICATION_POOL = List.of(
            MobEffects.MOVEMENT_SPEED,
            MobEffects.DIG_SPEED,
            MobEffects.DAMAGE_BOOST,
            MobEffects.JUMP,
            MobEffects.REGENERATION,
            MobEffects.HEAL,
            MobEffects.SATURATION,
            MobEffects.NIGHT_VISION,
            MobEffects.LUCK,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.ABSORPTION,
            MobEffects.SLOW_FALLING,
            MobEffects.WATER_BREATHING,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.CONDUIT_POWER,
            MobEffects.DOLPHINS_GRACE,
            MobEffects.HEALTH_BOOST,
            MobEffects.INVISIBILITY
    );

    private static final Map<Holder<MobEffect>, Holder<MobEffect>> NEGATIVE_TO_POSITIVE = Map.ofEntries(
            Map.entry(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.MOVEMENT_SPEED),
            Map.entry(MobEffects.DIG_SLOWDOWN, MobEffects.DIG_SPEED),
            Map.entry(MobEffects.WEAKNESS, MobEffects.DAMAGE_BOOST),
            Map.entry(MobEffects.HARM, MobEffects.HEAL),
            Map.entry(MobEffects.POISON, MobEffects.REGENERATION),
            Map.entry(MobEffects.WITHER, MobEffects.REGENERATION),
            Map.entry(MobEffects.HUNGER, MobEffects.SATURATION),
            Map.entry(MobEffects.CONFUSION, MobEffects.DAMAGE_RESISTANCE),
            Map.entry(MobEffects.BLINDNESS, MobEffects.NIGHT_VISION),
            Map.entry(MobEffects.DARKNESS, MobEffects.NIGHT_VISION),
            Map.entry(MobEffects.UNLUCK, MobEffects.LUCK)
    );

    private SodaEffectReducer() {
    }

    public static SodaData baseFromPotion(List<MobEffectInstance> effects, int color) {
        return new SodaData(copyEffects(effects), color, baseInstabilityGain());
    }

    public static List<MobEffectInstance> acceptedPotionEffects(PotionContents potion) {
        List<MobEffectInstance> acceptedEffects = new ArrayList<>();
        for (MobEffectInstance effect : potion.getAllEffects()) {
            acceptedEffects.add(new MobEffectInstance(
                    effect.getEffect(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }
        return acceptedEffects;
    }

    public static SodaData purifyWithAmethyst(SodaData data) {
        List<MobEffectInstance> purified = new ArrayList<>();
        for (MobEffectInstance effect : data.effects()) {
            purified.add(purifyNegativeEffect(effect));
        }
        purified = coalesce(purified);
        purified.sort(Comparator.comparing(SodaEffectReducer::effectId));
        return new SodaData(purified, data.color(), 0.0f);
    }


    public static SodaData mix(SodaData first, SodaData second, int firstAmount, int secondAmount, long worldSeed) {
        List<MobEffectInstance> combined = mergeWithDilution(first.effects(), second.effects(), firstAmount, secondAmount);

        Reduction reduction = resolveMix(combined, worldSeed);
        float instability = first.instability()
                + second.instability()
                + mixFlatInstabilityGain()
                + reduction.positiveReductions() * mixReactionInstabilityGain();
        List<MobEffectInstance> resolved = reduction.effects();

        if (instability > instabilityThreshold()) {
            resolved = new ArrayList<>(resolved);
            resolved.add(deterministicNegativeEffect(resolved, worldSeed));
            instability = safeInstabilityAfterBackfire();
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

                for (int j = i + 1; j < effects.size(); j++) {
                    MobEffectInstance second = effects.get(j);
                    if (effectId(first).equals(effectId(second))) {
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

            if (candidate.getDuration() >= MIN_EFFECT_DURATION) {
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

        if (affinity > reactionAffinityThreshold()) {
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
        List<Holder<MobEffect>> pool = new ArrayList<>(HIGHER_TIER_POOL);
        pool.addAll(NEGATIVE_POOL);
        pool.addAll(POSITIVE_PURIFICATION_POOL);
        return pool.stream()
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
        int duration = Math.max(MIN_EFFECT_DURATION, Math.round(source.getDuration() * ratio));
        int amplifier = source.getAmplifier();

        if (ratio < 0.5f && amplifier > 0) {
            amplifier--;
        }
        if (ratio < 0.25f && amplifier > 0) {
            amplifier--;
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

    private static MobEffectInstance purifyNegativeEffect(MobEffectInstance effect) {
        if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
            return new MobEffectInstance(effect);
        }

        Holder<MobEffect> mapped = NEGATIVE_TO_POSITIVE.get(effect.getEffect());
        if (mapped == null) {
            String id = effectId(effect);
            int index = Math.floorMod(id.hashCode(), POSITIVE_PURIFICATION_POOL.size());
            mapped = POSITIVE_PURIFICATION_POOL.get(index);
        }

        return new MobEffectInstance(
                mapped,
                effect.getDuration(),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon()
        );
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

    private static float baseInstabilityGain() {
        try {
            return (float) CreatePopConfig.basePotionInstabilityGain();
        } catch (IllegalStateException ignored) {
            return DEFAULT_BASE_INSTABILITY_GAIN;
        }
    }

    private static float mixReactionInstabilityGain() {
        try {
            return (float) CreatePopConfig.mixReactionInstabilityGain();
        } catch (IllegalStateException ignored) {
            return DEFAULT_MIX_REACTION_INSTABILITY_GAIN;
        }
    }

    private static float mixFlatInstabilityGain() {
        try {
            return CreatePopConfig.MIX_FLAT_INSTABILITY_GAIN.get().floatValue();
        } catch (IllegalStateException ignored) {
            return DEFAULT_MIX_FLAT_INSTABILITY_GAIN;
        }
    }

    private static float instabilityThreshold() {
        try {
            return CreatePopConfig.INSTABILITY_THRESHOLD.get().floatValue();
        } catch (IllegalStateException ignored) {
            return DEFAULT_INSTABILITY_THRESHOLD;
        }
    }

    private static float safeInstabilityAfterBackfire() {
        try {
            return CreatePopConfig.SAFE_INSTABILITY_AFTER_BACKFIRE.get().floatValue();
        } catch (IllegalStateException ignored) {
            return DEFAULT_SAFE_INSTABILITY_AFTER_BACKFIRE;
        }
    }

    private static float reactionAffinityThreshold() {
        try {
            return CreatePopConfig.REACTION_AFFINITY_THRESHOLD.get().floatValue();
        } catch (IllegalStateException ignored) {
            return DEFAULT_REACTION_AFFINITY_THRESHOLD;
        }
    }

    private record Reduction(List<MobEffectInstance> effects, int positiveReductions) {
    }
}

