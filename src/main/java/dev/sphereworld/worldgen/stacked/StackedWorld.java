package dev.sphereworld.worldgen.stacked;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public final class StackedWorld {
    public static final ThreadLocal<StackBand> PORTAL_BAND = new ThreadLocal<>();

    public static final ThreadLocal<BlockPos> FLUID_AT = new ThreadLocal<>();

    public static final net.minecraft.resources.Identifier DIMENSION_TYPE = dev.sphereworld.SphereWorld.id("stacked_planet");

    private StackedWorld() {
    }

    public static boolean is(@Nullable Level level) {
        return level instanceof ServerLevel server && server.getChunkSource().getGenerator() instanceof StackedChunkGenerator;
    }

    public static BlockPos sameNativeHeight(BlockPos pos, StackBand from, StackBand to) {
        int nativeY = pos.getY() - from.offset();
        int clamped = Math.max(to.nativeMinY() + 2, Math.min(to.nativeMinY() + to.nativeHeight() - 10, nativeY));
        return new BlockPos(pos.getX(), clamped + to.offset(), pos.getZ());
    }

    public static int bandSurface(ServerLevel level, StackBand band, int x, int z, int realHeight) {
        if (realHeight <= band.worldMaxY() + 1 && realHeight > band.worldMinY()) return realHeight;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, band.worldMaxY(), z);
        for (int y = band.worldMaxY(); y >= band.worldMinY(); y--) {
            pos.setY(y);
            if (Heightmap.Types.MOTION_BLOCKING.isOpaque().test(level.getBlockState(pos))) return y + 1;
        }
        return band.worldMinY();
    }

    public static WorldGenLevel endLayer(ServerLevel level) {
        return BandLevel.of(level, (StackedChunkGenerator) level.getChunkSource().getGenerator(), StackBand.END).level();
    }

    public static int endY(@Nullable Level level, int y) {
        return is(level) ? y + StackBand.END.offset() : y;
    }
}
