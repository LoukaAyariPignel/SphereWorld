package dev.sphereworld.mixin.seam;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.SeamContext;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Vec3i.class)
abstract class Vec3iDistanceMixin {
    @Shadow public abstract int getX();

    @Shadow public abstract int getY();

    @Shadow public abstract int getZ();

    @ModifyReturnValue(method = "distToCenterSqr(DDD)D", at = @At("RETURN"))
    private double sphereworld$center(double original, @Local(argsOnly = true, ordinal = 0) double x,
                                      @Local(argsOnly = true, ordinal = 1) double y, @Local(argsOnly = true, ordinal = 2) double z) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return original;
        double dx = g.delta(x, getX() + 0.5);
        double dy = getY() + 0.5 - y;
        double dz = g.delta(z, getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz;
    }

    @ModifyReturnValue(method = "distToLowCornerSqr(DDD)D", at = @At("RETURN"))
    private double sphereworld$corner(double original, @Local(argsOnly = true, ordinal = 0) double x,
                                      @Local(argsOnly = true, ordinal = 1) double y, @Local(argsOnly = true, ordinal = 2) double z) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return original;
        double dx = g.delta(x, (double) getX());
        double dy = getY() - y;
        double dz = g.delta(z, (double) getZ());
        return dx * dx + dy * dy + dz * dz;
    }

    @ModifyReturnValue(method = "distManhattan", at = @At("RETURN"))
    private int sphereworld$manhattan(int original, @Local(argsOnly = true) Vec3i pos) {
        PlanetGeometry g = SeamContext.current();
        if (g == null) return original;
        return Math.abs(g.delta(getX(), pos.getX())) + Math.abs(pos.getY() - getY()) + Math.abs(g.delta(getZ(), pos.getZ()));
    }
}
