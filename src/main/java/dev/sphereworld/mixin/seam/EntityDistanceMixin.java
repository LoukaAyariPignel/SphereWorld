package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class EntityDistanceMixin {
    @Shadow private Level level;

    @Shadow public abstract double getX();

    @Shadow public abstract double getY();

    @Shadow public abstract double getZ();

    @Inject(method = "distanceTo(Lnet/minecraft/world/entity/Entity;)F", at = @At("HEAD"), cancellable = true)
    private void sphereworld$distanceTo(Entity entity, CallbackInfoReturnable<Float> cir) {
        PlanetGeometry g = PlanetWrap.server(level);
        if (g == null) return;
        double dx = g.delta(entity.getX(), getX());
        double dy = getY() - entity.getY();
        double dz = g.delta(entity.getZ(), getZ());
        cir.setReturnValue((float) Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    @Inject(method = "distanceToSqr(DDD)D", at = @At("HEAD"), cancellable = true)
    private void sphereworld$distanceToSqr(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
        PlanetGeometry g = PlanetWrap.server(level);
        if (g == null) return;
        double dx = g.delta(x, getX());
        double dy = getY() - y;
        double dz = g.delta(z, getZ());
        cir.setReturnValue(dx * dx + dy * dy + dz * dz);
    }

    @Inject(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At("HEAD"), cancellable = true)
    private void sphereworld$distanceToSqrVec(Vec3 pos, CallbackInfoReturnable<Double> cir) {
        PlanetGeometry g = PlanetWrap.server(level);
        if (g == null) return;
        double dx = g.delta(pos.x, getX());
        double dy = getY() - pos.y;
        double dz = g.delta(pos.z, getZ());
        cir.setReturnValue(dx * dx + dy * dy + dz * dz);
    }
}
