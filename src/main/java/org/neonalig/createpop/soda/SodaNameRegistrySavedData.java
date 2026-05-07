package org.neonalig.createpop.soda;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public final class SodaNameRegistrySavedData extends SavedData {
    private static final String DATA_NAME = "createpop_soda_names";
    private static final String NAMES_TAG = "names";

    private final Map<String, String> names = new HashMap<>();

    public static SodaNameRegistrySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                SodaNameRegistrySavedData::new,
                SodaNameRegistrySavedData::load
        ), DATA_NAME);
    }

    private static SodaNameRegistrySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        SodaNameRegistrySavedData data = new SodaNameRegistrySavedData();
        if (tag.contains(NAMES_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag namesTag = tag.getCompound(NAMES_TAG);
            for (String key : namesTag.getAllKeys()) {
                String value = namesTag.getString(key).trim();
                if (!value.isEmpty()) {
                    data.names.put(key, value);
                }
            }
        }
        return data;
    }

    @Override
    @Nonnull
    public CompoundTag save(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider provider) {
        CompoundTag namesTag = new CompoundTag();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            namesTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put(NAMES_TAG, namesTag);
        return tag;
    }

    public String getName(String key) {
        return names.getOrDefault(key, "");
    }

    public void putName(String key, String name) {
        names.put(key, name == null ? "" : name.trim());
        setDirty();
    }
}

