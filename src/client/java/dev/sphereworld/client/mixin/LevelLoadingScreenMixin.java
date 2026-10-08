package dev.sphereworld.client.mixin;

import dev.sphereworld.client.render.PlanetLoadingView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
abstract class LevelLoadingScreenMixin extends Screen {
    @Shadow private float smoothedProgress;
    @Shadow private LevelLoadingScreen.Reason reason;
    @Shadow private net.minecraft.client.multiplayer.LevelLoadTracker loadTracker;

    protected LevelLoadingScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void sphereworld$planet(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (reason != LevelLoadingScreen.Reason.OTHER || !PlanetLoadingView.active()) return;
        PlanetLoadingView.extract(graphics, this.font, this.width, this.height, loadTracker.serverProgress());
        ci.cancel();
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/LevelLoadTracker;isLevelReady()Z"))
    private boolean sphereworld$waitForPregen(boolean ready) {
        return ready && !dev.sphereworld.compat.VoxyPregen.active();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return PlanetLoadingView.keyPressed(event.isEscape()) || super.keyPressed(event);
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void sphereworld$space(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (reason != LevelLoadingScreen.Reason.OTHER || !PlanetLoadingView.active()) return;
        PlanetLoadingView.extractBackground(graphics, this.width, this.height);
        ci.cancel();
    }
}
