package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.SeamDistances;
import dev.sphereworld.wrap.PlanetRaids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.raid.Raids;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Raids.class)
abstract class RaidsSeamMixin implements PlanetRaids {
    @Unique private @Nullable PlanetGeometry sphereworld$geometry;

    @Override
    public void sphereworld$setGeometry(@Nullable PlanetGeometry geometry) {
        sphereworld$geometry = geometry;
    }

    @Redirect(method = "getNearbyRaid", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private double sphereworld$raidDistance(BlockPos center, Vec3i pos) {
        return SeamDistances.blockDistSqr(sphereworld$geometry, center, pos);
    }
}
