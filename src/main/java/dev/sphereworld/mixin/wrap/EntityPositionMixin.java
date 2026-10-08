package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import dev.sphereworld.wrap.SeamShift;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class EntityPositionMixin {
    @Shadow private Level level;

    @Unique private double sphereworld$shiftX;
    @Unique private double sphereworld$shiftZ;

    @ModifyVariable(method = "setPosRaw", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double sphereworld$wrapX(double x) {
        PlanetGeometry g = PlanetWrap.server(level);
        if (g == null || g.isCanonical(x)) return x;
        double wrapped = g.canonical(x);
        sphereworld$shiftX = wrapped - x;
        return wrapped;
    }

    @ModifyVariable(method = "setPosRaw", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double sphereworld$wrapZ(double z) {
        PlanetGeometry g = PlanetWrap.server(level);
        if (g == null || g.isCanonical(z)) return z;
        double wrapped = g.canonical(z);
        sphereworld$shiftZ = wrapped - z;
        return wrapped;
    }

    @Inject(method = "setPosRaw", at = @At("RETURN"))
    private void sphereworld$afterWrap(CallbackInfo ci) {
        if (sphereworld$shiftX == 0 && sphereworld$shiftZ == 0) return;
        double dx = sphereworld$shiftX;
        double dz = sphereworld$shiftZ;
        sphereworld$shiftX = 0;
        sphereworld$shiftZ = 0;
        SeamShift.afterWrap((Entity) (Object) this, dx, dz);
    }
}
