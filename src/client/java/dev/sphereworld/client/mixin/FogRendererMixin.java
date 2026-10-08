package dev.sphereworld.client.mixin;

import dev.sphereworld.planet.Planets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void sphereworld$noDistanceWall(CallbackInfoReturnable<FogData> cir,
                                           @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ClientLevel level) {
        if (Planets.isPlanet(level) && !level.dimensionType().hasCeiling()) {
            FogData fog = cir.getReturnValue();
            fog.renderDistanceStart *= 8.0F;
            fog.renderDistanceEnd *= 8.0F;
        }
    }
}
