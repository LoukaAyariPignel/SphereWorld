package dev.sphereworld.client.mixin;

import dev.sphereworld.client.ClientPlanets;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void sphereworld$bindPlanet(CallbackInfo ci) {
        ClientPlanets.bind((ClientLevel) (Object) this);
    }
}
