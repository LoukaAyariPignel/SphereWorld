package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(WorldGenRegion.class)
abstract class WorldGenRegionMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private int centerChunkX;
    @Shadow @Final private int centerChunkZ;
    @Shadow @Final private int writeRadius;

    @Unique
    private static int sphereworld$distance(PlanetGeometry g, ChunkPos center, int x, int z) {
        return Math.max(Math.abs(g.chunkDelta(center.x(), x)), Math.abs(g.chunkDelta(center.z(), z)));
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/StaticCache2D;map(Lnet/minecraft/util/StaticCache2D$MappingFunction;)Lnet/minecraft/util/StaticCache2D;"))
    private StaticCache2D<@Nullable ChunkAccess> sphereworld$periodicNeighbourhood(StaticCache2D<GenerationChunkHolder> holders,
                                                                              StaticCache2D.MappingFunction<GenerationChunkHolder, @Nullable ChunkAccess> function,
                                                                              Operation<StaticCache2D<@Nullable ChunkAccess>> original,
                                                                              @Local(argsOnly = true) ServerLevel level,
                                                                              @Local(argsOnly = true) ChunkStep step,
                                                                              @Local(argsOnly = true) ChunkAccess center) {
        PlanetGeometry g = Planets.of(level);
        if (g == null) return original.call(holders, function);
        ChunkPos centerPos = center.getPos();
        return holders.map((holder, x, z) -> {
            int distance = sphereworld$distance(g, centerPos, x, z);
            ChunkStatus allowed = distance >= step.directDependencies().size() ? null : step.directDependencies().get(distance);
            return allowed == null ? null : holder.getChunkIfPresentUnchecked(allowed);
        });
    }

    @Redirect(method = {"getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", "hasChunk"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;getChessboardDistance(II)I"))
    private int sphereworld$periodicDistance(ChunkPos centerPos, int x, int z) {
        PlanetGeometry g = Planets.of(level);
        return g == null ? centerPos.getChessboardDistance(x, z) : sphereworld$distance(g, centerPos, x, z);
    }

    @ModifyReturnValue(method = "isWithinWriteZone(II)Z", at = @At("RETURN"))
    private boolean sphereworld$periodicWriteZone(boolean original, @Local(argsOnly = true, ordinal = 0) int chunkX,
                                                  @Local(argsOnly = true, ordinal = 1) int chunkZ) {
        PlanetGeometry g = Planets.of(level);
        if (g == null || original) return original;
        return Math.abs(g.chunkDelta(centerChunkX, chunkX)) <= writeRadius && Math.abs(g.chunkDelta(centerChunkZ, chunkZ)) <= writeRadius;
    }

    @ModifyVariable(method = "getBlockEntity", at = @At("HEAD"), argsOnly = true)
    private BlockPos sphereworld$canonicalBlockEntity(BlockPos pos) {
        PlanetGeometry g = Planets.of(level);
        return g == null ? pos : PlanetWrap.canonical(g, pos);
    }

    @WrapOperation(method = "setBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockEntityNbt(Lnet/minecraft/nbt/CompoundTag;)V"))
    private void sphereworld$canonicalPlaceholder(ChunkAccess chunk, CompoundTag tag, Operation<Void> original) {
        PlanetGeometry g = Planets.of(level);
        if (g != null) {
            tag.putInt("x", g.canonical(tag.getIntOr("x", 0)));
            tag.putInt("z", g.canonical(tag.getIntOr("z", 0)));
        }
        original.call(chunk, tag);
    }
}
