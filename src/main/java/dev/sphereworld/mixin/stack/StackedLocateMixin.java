package dev.sphereworld.mixin.stack;

import dev.sphereworld.worldgen.stacked.StackedChunkGenerator;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class StackedLocateMixin {
    private StackedLocateMixin() {
    }

    @Mixin(ServerChunkCache.class)
    public abstract static class ChunkCache {
        @Inject(method = "getGeneratorState", at = @At("HEAD"), cancellable = true)
        private void sphereworld$bandPlacements(CallbackInfoReturnable<ChunkGeneratorStructureState> cir) {
            StackedChunkGenerator.BandContext band = StackedChunkGenerator.LOCATING.get();
            if (band != null) cir.setReturnValue(band.structureState());
        }
    }

    @Mixin(ServerLevel.class)
    public abstract static class Level {
        @Inject(method = "structureManager", at = @At("HEAD"), cancellable = true)
        private void sphereworld$bandChecks(CallbackInfoReturnable<StructureManager> cir) {
            StackedChunkGenerator.BandContext band = StackedChunkGenerator.LOCATING.get();
            if (band != null) cir.setReturnValue(band.locateManager());
        }
    }
}
