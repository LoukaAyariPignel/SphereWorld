package dev.sphereworld.worldgen.stacked;

import dev.sphereworld.mixin.worldgen.StructureManagerAccessor;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.chunk.StructureAccess;
import org.jspecify.annotations.Nullable;

final class BandStructures {
    private static final Map<StructureStart, StructureStart> NATIVE = Collections.synchronizedMap(new WeakHashMap<>());

    private BandStructures() {
    }

    static StructureStart toWorld(StructureStart start, StackBand band) {
        if (band.offset() == 0 || !start.isValid()) return start;
        for (StructurePiece piece : start.getPieces()) piece.move(0, band.offset(), 0);
        return new StructureStart(start.getStructure(), start.getChunkPos(), start.getReferences(), new PiecesContainer(start.getPieces()));
    }

    static @Nullable StructureStart toNative(StructureStart start, StackBand band, LevelAccessor level) {
        if (band.offset() == 0 || !start.isValid()) return start;
        StructureStart cached = NATIVE.get(start);
        if (cached != null) return cached;

        ServerLevel server = level.getServer() == null ? null : level.getServer().overworld();
        if (server == null) return null;
        StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(server);
        CompoundTag tag = start.createTag(context, start.getChunkPos());
        StructureStart copy = StructureStart.loadStaticStart(context, tag, server.getSeed());
        if (copy == null) return null;
        for (StructurePiece piece : copy.getPieces()) piece.move(0, -band.offset(), 0);
        StructureStart shifted = new StructureStart(copy.getStructure(), copy.getChunkPos(), copy.getReferences(), new PiecesContainer(copy.getPieces()));
        NATIVE.put(start, shifted);
        return shifted;
    }

    static StructureManager manager(StructureManager real, StackedChunkGenerator.BandContext band) {
        StructureManagerAccessor access = (StructureManagerAccessor) real;
        return new BandStructureManager(access.sphereworld$level(), access, band);
    }

    private static final class BandStructureManager extends StructureManager {
        private final StackedChunkGenerator.BandContext band;
        private final LevelAccessor level;

        BandStructureManager(LevelAccessor level, StructureManagerAccessor real, StackedChunkGenerator.BandContext band) {
            super(level, real.sphereworld$worldOptions(), real.sphereworld$structureCheck());
            this.band = band;
            this.level = level;
        }

        @Override
        public @Nullable StructureStart getStartForStructure(Structure structure, StructureAccess chunk) {
            if (!band.structures().contains(structure)) return null;
            StructureStart start = super.getStartForStructure(structure, chunk);
            return start == null ? null : toNative(start, band.band(), level);
        }
    }
}
