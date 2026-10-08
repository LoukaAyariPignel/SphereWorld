package dev.sphereworld.mixin.level;

import dev.sphereworld.atlas.PlanetAtlases;
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

        if (pos.getY() + 1 < chunk.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() & 15, pos.getZ() & 15)) return;
        PlanetAtlases.markDirty(level, chunk.getPos());
    }
}
