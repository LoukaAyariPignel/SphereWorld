package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.SeamContext;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Vec3i.class)
abstract class Vec3iDistanceMixin {
    @Shadow public abstract int getX();

    @Shadow public abstract int getY();

    @Shadow public abstract int getZ();

    @Inject(method = "distToCenterSqr(DDD)D", at = @At("HEAD"), cancellable = true)
    private void sphereworld$center(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return;
        double dx = g.delta(x, getX() + 0.5);
        double dy = getY() + 0.5 - y;
        double dz = g.delta(z, getZ() + 0.5);
        cir.setReturnValue(dx * dx + dy * dy + dz * dz);
    }

    @Inject(method = "distToLowCornerSqr(DDD)D", at = @At("HEAD"), cancellable = true)
    private void sphereworld$corner(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return;
        double dx = g.delta(x, (double) getX());
        double dy = getY() - y;
        double dz = g.delta(z, (double) getZ());
        cir.setReturnValue(dx * dx + dy * dy + dz * dz);
    }

    @Inject(method = "distManhattan", at = @At("HEAD"), cancellable = true)
    private void sphereworld$manhattan(Vec3i pos, CallbackInfoReturnable<Integer> cir) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return;
        cir.setReturnValue(Math.abs(g.delta(getX(), pos.getX())) + Math.abs(pos.getY() - getY()) + Math.abs(g.delta(getZ(), pos.getZ())));
    }
}
