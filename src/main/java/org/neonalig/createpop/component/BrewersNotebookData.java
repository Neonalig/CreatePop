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
        return withEntry(data, "Unnamed Soda", List.of());
    }

    public BrewersNotebookData withEntry(SodaData data, String name, List<String> ingredients) {
        String key = keyFor(data);
        Map<String, Entry> merged = new LinkedHashMap<>(entryMap());
        merged.putIfAbsent(key, new Entry(key, data, name, ingredients));
        return fromEntryMap(merged);
    }

    public BrewersNotebookData merge(BrewersNotebookData other) {
        Map<String, Entry> merged = new LinkedHashMap<>(entryMap());
        for (Entry incoming : other.entries) {
            merged.merge(incoming.key(), incoming, (current, next) -> {
                String mergedName = current.name().isBlank() && !next.name().isBlank() ? next.name() : current.name();
                String mergedNote = current.note().isEmpty() && !next.note().isEmpty() ? next.note() : current.note();
                if (current.ingredients().isEmpty() && !next.ingredients().isEmpty()) {
                    return new Entry(current.key(), current.data(), mergedName, next.ingredients(), mergedNote);
                }
                return new Entry(current.key(), current.data(), mergedName, current.ingredients(), mergedNote);
            });
        }
        return fromEntryMap(merged);
    }

    public Map<String, SodaData> asMap() {
        Map<String, SodaData> byKey = new LinkedHashMap<>();
        for (Entry entry : entries) {
            byKey.put(entry.key(), entry.data());
        }
        return byKey;
    }

    public Map<String, Entry> entryMap() {
        Map<String, Entry> byKey = new LinkedHashMap<>();
        for (Entry entry : entries) {
            byKey.put(entry.key(), entry);
        }
        return byKey;
    }

    public boolean containsKey(String key) {
        return entryMap().containsKey(key);
    }

    public BrewersNotebookData withoutEntry(String key) {
        Map<String, Entry> updated = new LinkedHashMap<>(entryMap());
        updated.remove(key);
        return fromEntryMap(updated);
    }

    public static BrewersNotebookData fromMap(Map<String, SodaData> byKey) {
        List<Entry> merged = new ArrayList<>();
        byKey.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> merged.add(new Entry(entry.getKey(), entry.getValue(), "Unnamed Soda", List.of())));
        return new BrewersNotebookData(merged);
    }

    public static BrewersNotebookData fromEntryMap(Map<String, Entry> byKey) {
        List<Entry> merged = new ArrayList<>();
        byKey.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> merged.add(entry.getValue()));
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
        Map<String, Entry> merged = new LinkedHashMap<>();
        for (Entry entry : input) {
            merged.merge(entry.key(), entry, (current, next) -> {
                String mergedName = current.name().isBlank() && !next.name().isBlank() ? next.name() : current.name();
                String mergedNote = current.note().isEmpty() && !next.note().isEmpty() ? next.note() : current.note();
                if (current.ingredients().isEmpty() && !next.ingredients().isEmpty()) {
                    return new Entry(current.key(), current.data(), mergedName, next.ingredients(), mergedNote);
                }
                return new Entry(current.key(), current.data(), mergedName, current.ingredients(), mergedNote);
            });
        }
        List<Entry> normalized = new ArrayList<>();
        merged.values().stream()
                .sorted((a, b) -> {
                    int byName = a.name().compareToIgnoreCase(b.name());
                    return byName != 0 ? byName : a.key().compareTo(b.key());
                })
                .forEach(normalized::add);
        return List.copyOf(normalized);
    }

    public record Entry(String key, SodaData data, String name, List<String> ingredients, String note) {
        public Entry {
            name = name == null || name.isBlank() ? "Unnamed Soda" : name;
            ingredients = List.copyOf(ingredients);
            note = note == null ? "" : note;
        }

        public Entry(String key, SodaData data, String name, List<String> ingredients) {
            this(key, data, name, ingredients, "");
        }

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("key").forGetter(Entry::key),
                SodaData.CODEC.fieldOf("data").forGetter(Entry::data),
                Codec.STRING.optionalFieldOf("name", "Unnamed Soda").forGetter(Entry::name),
                Codec.STRING.listOf().optionalFieldOf("ingredients", List.of()).forGetter(Entry::ingredients),
                Codec.STRING.optionalFieldOf("note", "").forGetter(Entry::note)
        ).apply(instance, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                Entry::key,
                SodaData.STREAM_CODEC,
                Entry::data,
                ByteBufCodecs.STRING_UTF8,
                Entry::name,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                Entry::ingredients,
                ByteBufCodecs.STRING_UTF8,
                Entry::note,
                Entry::new
        );
    }
}


