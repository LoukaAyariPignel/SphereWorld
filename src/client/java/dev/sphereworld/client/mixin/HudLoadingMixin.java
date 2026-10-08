package dev.sphereworld.client.mixin;

import dev.sphereworld.client.render.PlanetLoadingView;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class HudLoadingMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void sphereworld$hiddenWhileLoading(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() instanceof LevelLoadingScreen && PlanetLoadingView.active()) ci.cancel();
    }
}
