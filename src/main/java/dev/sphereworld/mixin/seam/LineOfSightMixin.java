package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
abstract class LineOfSightMixin {
    @Redirect(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double sphereworld$targetX(Entity target) {
        LivingEntity self = (LivingEntity) (Object) this;
        PlanetGeometry g = PlanetWrap.server(self.level());
        return g == null ? target.getX() : g.nearestImage(target.getX(), self.getX());
    }

    @Redirect(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double sphereworld$targetZ(Entity target) {
        LivingEntity self = (LivingEntity) (Object) this;
        PlanetGeometry g = PlanetWrap.server(self.level());
        return g == null ? target.getZ() : g.nearestImage(target.getZ(), self.getZ());
    }
}
