package dev.sphereworld.client.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.sphereworld.client.render.PlanetLodRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CameraUniforms", remap = false)
abstract class IrisFarMixin {
    @ModifyReturnValue(method = "getRenderDistanceInBlocks", at = @At("RETURN"))
    private static int sphereworld$horizonFar(int far) {
        if (Boolean.getBoolean("sphereworld.noHorizonFar")) return far;
        return Math.max(far, PlanetLodRenderer.packHorizonDistance());
    }
}
