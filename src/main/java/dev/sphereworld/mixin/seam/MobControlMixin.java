package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MoveControl.class)
abstract class MobControlMixin {
    @Shadow @Final protected Mob mob;

    @ModifyVariable(method = "setWantedPosition", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double sphereworld$wantedX(double x) {
        PlanetGeometry g = PlanetWrap.server(mob.level());
        return g == null ? x : g.nearestImage(x, mob.getX());
    }

    @ModifyVariable(method = "setWantedPosition", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double sphereworld$wantedZ(double z) {
        PlanetGeometry g = PlanetWrap.server(mob.level());
        return g == null ? z : g.nearestImage(z, mob.getZ());
    }
}
