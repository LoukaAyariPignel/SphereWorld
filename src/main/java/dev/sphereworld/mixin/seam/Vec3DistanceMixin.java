package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.SeamContext;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Vec3.class)
abstract class Vec3DistanceMixin {
    @Shadow @Final public double x;
    @Shadow @Final public double y;
    @Shadow @Final public double z;

    @Inject(method = "distanceToSqr(DDD)D", at = @At("HEAD"), cancellable = true)
    private void sphereworld$sqrXyz(double px, double py, double pz, CallbackInfoReturnable<Double> cir) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return;
        double dx = g.delta(x, px);
        double dy = py - y;
        double dz = g.delta(z, pz);
        cir.setReturnValue(dx * dx + dy * dy + dz * dz);
    }

    @Inject(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At("HEAD"), cancellable = true)
    private void sphereworld$sqr(Vec3 other, CallbackInfoReturnable<Double> cir) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return;
        double dx = g.delta(x, other.x);
        double dy = other.y - y;
        double dz = g.delta(z, other.z);
        cir.setReturnValue(dx * dx + dy * dy + dz * dz);
    }

    @Inject(method = "distanceTo", at = @At("HEAD"), cancellable = true)
    private void sphereworld$distance(Vec3 other, CallbackInfoReturnable<Double> cir) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return;
        double dx = g.delta(x, other.x);
        double dy = other.y - y;
        double dz = g.delta(z, other.z);
        cir.setReturnValue(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    @Inject(method = "closerThan(Lnet/minecraft/world/phys/Vec3;DD)Z", at = @At("HEAD"), cancellable = true)
    private void sphereworld$closer(Vec3 other, double xz, double maxY, CallbackInfoReturnable<Boolean> cir) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return;
        double dx = g.delta(x, other.x);
        double dz = g.delta(z, other.z);
        cir.setReturnValue(dx * dx + dz * dz < xz * xz && Math.abs(other.y - y) < maxY);
    }
}
