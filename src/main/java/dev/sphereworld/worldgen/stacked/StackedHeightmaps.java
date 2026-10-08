package dev.sphereworld.worldgen.stacked;

import java.util.function.Predicate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

public final class StackedHeightmaps {
    public interface Chunk {
        int sphereworld$overworldHeight(Heightmap.Types type, int localX, int localZ);
    }

    private StackedHeightmaps() {
    }

    public static int overworldHeight(LevelChunk chunk, Heightmap.Types type, int localX, int localZ) {
        StackBand band = StackBand.OVERWORLD;
        Predicate<BlockState> opaque = type.isOpaque();
        LevelChunkSection[] sections = chunk.getSections();
        int first = band.firstWorldSectionIndex();
        for (int s = Math.min(sections.length, first + band.sectionCount()) - 1; s >= first; s--) {
            LevelChunkSection section = sections[s];
            if (section.hasOnlyAir()) continue;
            for (int y = 15; y >= 0; y--) {
                if (opaque.test(section.getBlockState(localX, y, localZ))) return StackBand.WORLD_MIN_Y + (s << 4) + y;
            }
        }
        return band.worldMinY() - 1;
    }
}
