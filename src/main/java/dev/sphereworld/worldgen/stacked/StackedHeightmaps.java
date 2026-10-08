package dev.sphereworld.worldgen.stacked;

import java.util.Arrays;
import java.util.EnumSet;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.ticks.ProtoChunkTicks;

public final class StackedHeightmaps {
    private static final EnumSet<Heightmap.Types> TYPES = EnumSet.allOf(Heightmap.Types.class);

    public interface Chunk {
        int sphereworld$overworldHeight(Heightmap.Types type, int localX, int localZ);
    }

    private StackedHeightmaps() {
    }

    public static ProtoChunk overworldView(LevelChunk chunk, PalettedContainerFactory containers) {
        StackBand band = StackBand.OVERWORLD;
        int first = band.firstWorldSectionIndex();
        LevelChunkSection[] sections = Arrays.copyOfRange(chunk.getSections(), first, first + band.sectionCount());
        ProtoChunk view = new ProtoChunk(chunk.getPos(), UpgradeData.EMPTY, sections, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(),
                band.heightAccessor(), containers, null);
        Heightmap.primeHeightmaps(view, TYPES);
        return view;
    }

    public static Iterable<Heightmap.Types> types() {
        return TYPES;
    }
}
