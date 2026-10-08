package dev.sphereworld.worldgen.stacked;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;

interface BandAccess {
    ProtoChunk view(ChunkAccess chunk);

    WorldGenLevel level();

    static BandAccess of(WorldGenLevel real, StackedChunkGenerator generator, StackBand band) {
        if (real.getClass() == WorldGenRegion.class) return new RegionBandLevel((WorldGenRegion) real, generator, band);
        return BandLevel.of(real, generator, band);
    }
}
