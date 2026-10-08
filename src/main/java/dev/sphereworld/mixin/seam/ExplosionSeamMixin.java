package dev.sphereworld.mixin.seam;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerExplosion.class)
abstract class ExplosionSeamMixin {
    @Shadow @Final private net.minecraft.server.level.ServerLevel level;

    @ModifyExpressionValue(method = "hurtEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 sphereworld$shortWay(Vec3 direction) {
        PlanetGeometry g = Planets.of(level);
        return g == null ? direction : new Vec3(g.canonical(direction.x), direction.y, g.canonical(direction.z));
    }

    @WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerExplosion;getSeenPercent(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)F"))
    private float sphereworld$nearImage(Vec3 center, Entity entity, Operation<Float> original) {
        PlanetGeometry g = Planets.of(level);
        if (g != null) {
            center = new Vec3(entity.getX() + g.canonical(center.x - entity.getX()), center.y,
                    entity.getZ() + g.canonical(center.z - entity.getZ()));
        }
        return original.call(center, entity);
    }
}
