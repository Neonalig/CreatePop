package org.neonalig.createpop.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;
import org.neonalig.createpop.soda.SodaEffectReducer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BrewersNotebookData(List<Entry> entries) {
    public static final BrewersNotebookData EMPTY = new BrewersNotebookData(List.of());

    public static final Codec<BrewersNotebookData> CODEC = Entry.CODEC.listOf()
            .xmap(BrewersNotebookData::new, BrewersNotebookData::entries);

    public static final StreamCodec<RegistryFriendlyByteBuf, BrewersNotebookData> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(BrewersNotebookData::new, BrewersNotebookData::entries);

    public BrewersNotebookData {
        entries = normalize(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public BrewersNotebookData withEntry(SodaData data) {
        Map<String, SodaData> merged = new LinkedHashMap<>(asMap());
        merged.putIfAbsent(keyFor(data), data);
        return fromMap(merged);
    }

    public BrewersNotebookData merge(BrewersNotebookData other) {
        Map<String, SodaData> merged = new LinkedHashMap<>(asMap());
        merged.putAll(other.asMap());
        return fromMap(merged);
    }

    public Map<String, SodaData> asMap() {
        Map<String, SodaData> byKey = new LinkedHashMap<>();
        for (Entry entry : entries) {
            byKey.put(entry.key(), entry.data());
        }
        return byKey;
    }

    public static BrewersNotebookData fromMap(Map<String, SodaData> byKey) {
        List<Entry> merged = new ArrayList<>();
        byKey.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> merged.add(new Entry(entry.getKey(), entry.getValue())));
        return new BrewersNotebookData(merged);
    }

    public static String keyFor(SodaData data) {
        StringBuilder key = new StringBuilder();
        key.append("c=").append(data.color())
                .append(";i=").append(Float.floatToIntBits(data.instability()))
                .append(";e=");

        List<MobEffectInstance> effects = SodaEffectReducer.copyEffects(data.effects());
        effects.sort(java.util.Comparator.comparing(SodaEffectReducer::effectId));
        for (MobEffectInstance effect : effects) {
            key.append(SodaEffectReducer.effectId(effect))
                    .append('@').append(effect.getAmplifier())
                    .append('@').append(effect.getDuration())
                    .append('|');
        }
        return key.toString();
    }

    private static List<Entry> normalize(List<Entry> input) {
        if (input.isEmpty()) {
            return List.of();
        }
        Map<String, SodaData> merged = new LinkedHashMap<>();
        for (Entry entry : input) {
            merged.put(entry.key(), entry.data());
        }
        List<Entry> normalized = new ArrayList<>();
        merged.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> normalized.add(new Entry(entry.getKey(), entry.getValue())));
        return List.copyOf(normalized);
    }

    public record Entry(String key, SodaData data) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("key").forGetter(Entry::key),
                SodaData.CODEC.fieldOf("data").forGetter(Entry::data)
        ).apply(instance, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                Entry::key,
                SodaData.STREAM_CODEC,
                Entry::data,
                Entry::new
        );
    }
}


