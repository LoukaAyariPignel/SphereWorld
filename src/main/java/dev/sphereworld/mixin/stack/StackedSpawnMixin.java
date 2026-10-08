package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedHeightmaps;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerSpawnFinder.class)
public abstract class StackedSpawnMixin {
    @WrapOperation(method = "getLevelRespawnPos", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private static int sphereworld$overworldSurface(LevelChunk chunk, Heightmap.Types type, int x, int z, Operation<Integer> original,
            @Local(argsOnly = true) ServerLevel level) {
        if (StackedWorld.is(level)) return ((StackedHeightmaps.Chunk) chunk).sphereworld$overworldHeight(type, x, z);
        return original.call(chunk, type, x, z);
    }

    @ModifyExpressionValue(method = "fixupSpawnHeight", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/CollisionGetter;getMaxY()I"))
    private static int sphereworld$belowEnd(int maxY, @Local(argsOnly = true) CollisionGetter level) {
        return level instanceof Level real && StackedWorld.is(real) ? Math.min(maxY, StackBand.OVERWORLD.worldMaxY()) : maxY;
    }
}
