package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
abstract class TrackedEntityMixin {
    @Redirect(method = "updatePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 sphereworld$periodicDelta(Vec3 playerPos, Vec3 entityPos,
                                           net.minecraft.server.level.ServerPlayer player) {
        Vec3 delta = playerPos.subtract(entityPos);
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? delta : new Vec3(g.canonical(delta.x), delta.y, g.canonical(delta.z));
    }
}
