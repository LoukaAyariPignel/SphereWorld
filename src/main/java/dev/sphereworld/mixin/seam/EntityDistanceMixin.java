package dev.sphereworld.mixin.seam;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
abstract class EntityDistanceMixin {
    @Shadow private Level level;

    @Shadow public abstract double getX();

    @Shadow public abstract double getY();

    @Shadow public abstract double getZ();

    @ModifyReturnValue(method = "distanceTo(Lnet/minecraft/world/entity/Entity;)F", at = @At("RETURN"))
    private float sphereworld$distanceTo(float original, @Local(argsOnly = true) Entity entity) {
        PlanetGeometry g = PlanetWrap.server(level);
        if (g == null) return original;
        double dx = g.delta(entity.getX(), getX());
        double dy = getY() - entity.getY();
        double dz = g.delta(entity.getZ(), getZ());
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @ModifyReturnValue(method = "distanceToSqr(DDD)D", at = @At("RETURN"))
    private double sphereworld$distanceToSqr(double original, @Local(argsOnly = true, ordinal = 0) double x,
                                             @Local(argsOnly = true, ordinal = 1) double y, @Local(argsOnly = true, ordinal = 2) double z) {
        PlanetGeometry g = PlanetWrap.server(level);
        if (g == null) return original;
        double dx = g.delta(x, getX());
        double dy = getY() - y;
        double dz = g.delta(z, getZ());
        return dx * dx + dy * dy + dz * dz;
    }

    @ModifyReturnValue(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", at = @At("RETURN"))
    private double sphereworld$distanceToSqrVec(double original, @Local(argsOnly = true) Vec3 pos) {
        PlanetGeometry g = PlanetWrap.server(level);
        if (g == null) return original;
        double dx = g.delta(pos.x, getX());
        double dy = getY() - pos.y;
        double dz = g.delta(pos.z, getZ());
        return dx * dx + dy * dy + dz * dz;
    }
}
