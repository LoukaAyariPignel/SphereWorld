package dev.sphereworld.mixin.level;

import dev.sphereworld.atlas.PlanetAtlases;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedHeightmaps;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
abstract class ChunkAtlasMixin {
    @Shadow @Final private Level level;

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void sphereworld$surfaceChanged(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() == null || level.isClientSide()) return;
        LevelChunk chunk = (LevelChunk) (Object) this;

        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() & 15, pos.getZ() & 15);
        if (pos.getY() + 1 < top) {
            if (top <= StackBand.OVERWORLD.worldMaxY() + 1 || !StackBand.OVERWORLD.containsWorldY(pos.getY()) || !StackedWorld.is(level)) return;
            if (pos.getY() + 1 < StackedHeightmaps.overworldHeight(chunk, Heightmap.Types.WORLD_SURFACE, pos.getX() & 15, pos.getZ() & 15)) return;
        }
        PlanetAtlases.markDirty(level, chunk.getPos());
    }
}
