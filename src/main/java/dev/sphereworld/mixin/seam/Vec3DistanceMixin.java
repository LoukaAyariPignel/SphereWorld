package dev.sphereworld.mixin.seam;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.SeamContext;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Vec3.class)
abstract class Vec3DistanceMixin {
    @Shadow @Final public double x;
    @Shadow @Final public double y;
    @Shadow @Final public double z;

    @ModifyReturnValue(method = "distanceToSqr(DDD)D", at = @At("RETURN"))
    private double sphereworld$sqrXyz(double original, @Local(argsOnly = true, ordinal = 0) double px,
                                      @Local(argsOnly = true, ordinal = 1) double py, @Local(argsOnly = true, ordinal = 2) double pz) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return original;
        double dx = g.delta(x, px);
        double dy = py - y;
        double dz = g.delta(z, pz);
        return dx * dx + dy * dy + dz * dz;
    }

    @ModifyReturnValue(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At("RETURN"))
    private double sphereworld$sqr(double original, @Local(argsOnly = true) Vec3 other) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return original;
        double dx = g.delta(x, other.x);
        double dy = other.y - y;
        double dz = g.delta(z, other.z);
        return dx * dx + dy * dy + dz * dz;
    }

    @ModifyReturnValue(method = "distanceTo", at = @At("RETURN"))
    private double sphereworld$distance(double original, @Local(argsOnly = true) Vec3 other) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return original;
        double dx = g.delta(x, other.x);
        double dy = other.y - y;
        double dz = g.delta(z, other.z);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @ModifyReturnValue(method = "closerThan(Lnet/minecraft/world/phys/Vec3;DD)Z", at = @At("RETURN"))
    private boolean sphereworld$closer(boolean original, @Local(argsOnly = true) Vec3 other,
                                       @Local(argsOnly = true, ordinal = 0) double xz, @Local(argsOnly = true, ordinal = 1) double maxY) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return original;
        double dx = g.delta(x, other.x);
        double dz = g.delta(z, other.z);
        return dx * dx + dz * dz < xz * xz && Math.abs(other.y - y) < maxY;
    }
}
