package dev.sphereworld.worldgen;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.jspecify.annotations.Nullable;

public final class CarverBiomeCache {
    private static final int SIZE = 1 << 14;

    private record Entry(long pos, BiomeGenerationSettings settings) {
    }

    private record Table(RandomState owner, Entry[] entries) {
    }

    private volatile @Nullable Table table;

    public @Nullable BiomeGenerationSettings get(RandomState randomState, ChunkPos pos) {
        Table current = table;
        if (current == null || current.owner() != randomState) return null;
        long key = pos.pack();
        Entry entry = current.entries()[slot(key)];
        return entry != null && entry.pos() == key ? entry.settings() : null;
    }

    public void put(RandomState randomState, ChunkPos pos, BiomeGenerationSettings settings) {
        Table current = table;
        if (current == null || current.owner() != randomState) table = current = new Table(randomState, new Entry[SIZE]);
        long key = pos.pack();
        current.entries()[slot(key)] = new Entry(key, settings);
    }

    private static int slot(long key) {
        return (int) HashCommon.mix(key) & (SIZE - 1);
    }
}
