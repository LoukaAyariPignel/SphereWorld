package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ProjectileUtil.class)
abstract class ProjectileUtilMixin {
    @Redirect(method = {
            "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private static AABB sphereworld$nearestBox(Entity entity,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true, ordinal = 0) Vec3 from) {
        AABB box = entity.getBoundingBox();
        PlanetGeometry g = PlanetWrap.server(entity.level());
        if (g == null || from == null) return box;
        double dx = g.nearestImage(box.getCenter().x, from.x) - box.getCenter().x;
        double dz = g.nearestImage(box.getCenter().z, from.z) - box.getCenter().z;
        return dx == 0 && dz == 0 ? box : box.move(dx, 0, dz);
    }
}
