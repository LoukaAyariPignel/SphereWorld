package dev.sphereworld.client.mixin;

import com.mojang.renderpearl.api.commands.RenderPass;
import dev.sphereworld.client.render.PlanetLodRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @Inject(method = "executeSolid", at = @At("HEAD"))
    private void sphereworld$drawPlanet(CallbackInfo ci,
                                        @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) RenderPass renderPass) {
        PlanetLodRenderer.render(renderPass);
    }

    @Inject(method = "executeSolid", at = @At("TAIL"))
    private void sphereworld$drawPlanetAfterTerrain(CallbackInfo ci,
                                                    @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) RenderPass renderPass) {
        PlanetLodRenderer.renderAfterTerrain(renderPass);
    }
}
