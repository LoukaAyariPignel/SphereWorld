package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = {
        "net.minecraft.world.level.dimension.end.DragonRespawnStage$1",
        "net.minecraft.world.level.dimension.end.DragonRespawnStage$2",
        "net.minecraft.world.level.dimension.end.DragonRespawnStage$3",
        "net.minecraft.world.level.dimension.end.DragonRespawnStage$4"
})
public abstract class DragonRespawnStageMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "CONSTANT", args = "intValue=128"))
    private int sphereworld$beamHeight(int y, @Local(argsOnly = true) ServerLevel level) {
        return StackedWorld.is(level) ? y + StackBand.END.offset() : y;
    }

    @WrapOperation(method = "tick", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/feature/EndSpikeFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean sphereworld$pillarInEndLayer(EndSpikeFeature feature, WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin,
            Operation<Boolean> original) {
        if (!(level instanceof ServerLevel server) || !StackedWorld.is(server)) return original.call(feature, level, generator, random, origin);
        int offset = StackBand.END.offset();
        EndSpikeFeature nativeFeature = new EndSpikeFeature(
                feature.spikes().stream()
                        .map(s -> new EndSpikeFeature.EndSpike(s.getCenterX(), s.getCenterZ(), s.getRadius(), s.getHeight() - offset, s.isGuarded()))
                        .toList(),
                feature.crystalInvulnerable(),
                feature.crystalBeamTarget().map(pos -> pos.below(offset)));
        return original.call(nativeFeature, StackedWorld.endLayer(server), generator, random, origin);
    }
}
