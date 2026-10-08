package dev.sphereworld.mixin.worldgen;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkAccess.class)
abstract class ChunkAccessMixin {
    @Unique private long sphereworld$heights;

    @Unique
    private long sphereworld$heights(LevelHeightAccessor accessor) {
        long heights = sphereworld$heights;
        if (heights == 0) sphereworld$heights = heights = (long) accessor.getMinY() << 32 | accessor.getHeight() & 0xFFFFFFFFL;
        return heights;
    }

    @Redirect(method = "getMinY", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelHeightAccessor;getMinY()I"))
    private int sphereworld$minY(LevelHeightAccessor accessor) {
        return (int) (sphereworld$heights(accessor) >> 32);
    }

    @Redirect(method = "getHeight()I", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelHeightAccessor;getHeight()I"))
    private int sphereworld$height(LevelHeightAccessor accessor) {
        return (int) sphereworld$heights(accessor);
    }
}
