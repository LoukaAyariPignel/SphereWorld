package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.SeamDistances;
import dev.sphereworld.planet.Planets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Raid.class)
abstract class RaidSeamMixin {
    @Redirect(method = "updateRaiders", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private double sphereworld$raiderDistance(BlockPos center, Vec3i raider, ServerLevel level) {
        return SeamDistances.blockDistSqr(Planets.of(level), center, raider);
    }
}
