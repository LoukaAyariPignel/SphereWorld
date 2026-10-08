package dev.sphereworld.mixin.stack;

import dev.sphereworld.stack.StackTravel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class EntityStackMixin {
    @Inject(method = "checkBelowWorld", at = @At("HEAD"), cancellable = true)
    private void sphereworld$travelBetweenShells(CallbackInfo ci) {
        if (StackTravel.tick((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}
