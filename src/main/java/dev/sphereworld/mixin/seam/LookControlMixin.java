package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LookControl.class)
abstract class LookControlMixin {
    @Shadow @Final protected Mob mob;

    @ModifyVariable(method = "setLookAt(DDDFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double sphereworld$lookX(double x) {
        PlanetGeometry g = PlanetWrap.server(mob.level());
        return g == null ? x : g.nearestImage(x, mob.getX());
    }

    @ModifyVariable(method = "setLookAt(DDDFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double sphereworld$lookZ(double z) {
        PlanetGeometry g = PlanetWrap.server(mob.level());
        return g == null ? z : g.nearestImage(z, mob.getZ());
    }
}
