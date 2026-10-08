package dev.sphereworld.mixin.world;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EyeOfEnder.class)
abstract class EyeOfEnderMixin {
    @ModifyVariable(method = "signalTo", at = @At("HEAD"), argsOnly = true)
    private Vec3 sphereworld$nearestTarget(Vec3 target) {
        EyeOfEnder self = (EyeOfEnder) (Object) this;
        PlanetGeometry g = PlanetWrap.server(self.level());
        return g == null ? target : PlanetWrap.nearestImage(g, target, self.position());
    }
}
