package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkMap.class)
abstract class SpawnDistanceMixin {
    @Shadow @Final private ServerLevel level;

    @Redirect(method = "playerIsCloseEnoughForSpawning", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkMap;euclideanDistanceSquared(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/phys/Vec3;)D"))
    private double sphereworld$spawnDistance(ChunkPos chunk, Vec3 pos) {
        double cx = SectionPos.sectionToBlockCoord(chunk.x(), 8);
        double cz = SectionPos.sectionToBlockCoord(chunk.z(), 8);
        PlanetGeometry g = Planets.of(level);
        double dx = g == null ? cx - pos.x : g.delta(pos.x, cx);
        double dz = g == null ? cz - pos.z : g.delta(pos.z, cz);
        return dx * dx + dz * dz;
    }

    @Redirect(method = "playerIsCloseEnoughTo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private double sphereworld$closeEnough(Vec3 player, Vec3 target) {
        PlanetGeometry g = Planets.of(level);
        if (g == null) return player.distanceTo(target);
        double dx = g.delta(player.x, target.x);
        double dy = target.y - player.y;
        double dz = g.delta(player.z, target.z);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
