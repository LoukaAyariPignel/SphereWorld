package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedAmbience;
import dev.sphereworld.worldgen.stacked.StackedHeightmaps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MapItem.class)
public abstract class StackedMapMixin {
    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/dimension/DimensionType;hasCeiling()Z"))
    private boolean sphereworld$netherCeiling(boolean ceiling, @Local(argsOnly = true) Level level, @Local(argsOnly = true) Entity player) {
        return ceiling || StackedAmbience.bandAt(level, player.position()) == StackBand.NETHER;
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private int sphereworld$layerSurface(LevelChunk chunk, Heightmap.Types type, int x, int z, Operation<Integer> original,
            @Local(argsOnly = true) Level level, @Local(argsOnly = true) Entity player) {
        if (StackedAmbience.bandAt(level, player.position()) == StackBand.OVERWORLD) {
            return ((StackedHeightmaps.Chunk) chunk).sphereworld$overworldHeight(type, x & 15, z & 15);
        }
        return original.call(chunk, type, x, z);
    }
}
