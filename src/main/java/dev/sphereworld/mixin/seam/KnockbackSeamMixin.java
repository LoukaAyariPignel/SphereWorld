package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
abstract class KnockbackSeamMixin {
    private static final String KNOCKBACK =
            "Lnet/minecraft/world/entity/LivingEntity;knockback(DDDLnet/minecraft/world/damagesource/DamageSource;F)V";
    private static final String INDICATE_DAMAGE = "Lnet/minecraft/world/entity/LivingEntity;indicateDamage(DD)V";

    @ModifyArg(method = {"dealDefaultKnockback", "blockedByItem"}, at = @At(value = "INVOKE", target = KNOCKBACK), index = 1)
    private double sphereworld$knockbackX(double xd) {
        return sphereworld$wrap(xd);
    }

    @ModifyArg(method = {"dealDefaultKnockback", "blockedByItem"}, at = @At(value = "INVOKE", target = KNOCKBACK), index = 2)
    private double sphereworld$knockbackZ(double zd) {
        return sphereworld$wrap(zd);
    }

    @ModifyArg(method = "dealDefaultKnockback", at = @At(value = "INVOKE", target = INDICATE_DAMAGE), index = 0)
    private double sphereworld$indicateX(double xd) {
        return sphereworld$wrap(xd);
    }

    @ModifyArg(method = "dealDefaultKnockback", at = @At(value = "INVOKE", target = INDICATE_DAMAGE), index = 1)
    private double sphereworld$indicateZ(double zd) {
        return sphereworld$wrap(zd);
    }

    private double sphereworld$wrap(double delta) {
        PlanetGeometry g = Planets.of(((LivingEntity) (Object) this).level());
        return g == null ? delta : g.canonical(delta);
    }
}
