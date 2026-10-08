package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
abstract class ChunkBlockEntityMixin {
    @Shadow @Final private Level level;

    @Inject(method = {"setBlockEntity", "addAndRegisterBlockEntity"}, at = @At("HEAD"))
    private void sphereworld$canonicalBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        PlanetGeometry g = PlanetWrap.server(level);
        if (g == null) return;
        BlockPos pos = blockEntity.getBlockPos();
        BlockPos canonical = PlanetWrap.canonical(g, pos);
        if (!canonical.equals(pos)) ((BlockEntityPositionAccessor) blockEntity).sphereworld$setWorldPosition(canonical);
    }
}
