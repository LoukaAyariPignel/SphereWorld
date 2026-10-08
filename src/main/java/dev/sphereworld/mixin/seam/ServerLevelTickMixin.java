package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.Planets;
import dev.sphereworld.wrap.SeamContext;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class ServerLevelTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void sphereworld$enter(BooleanSupplier haveTime, CallbackInfo ci) {
        SeamContext.enter(Planets.of((ServerLevel) (Object) this));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void sphereworld$exit(BooleanSupplier haveTime, CallbackInfo ci) {
        SeamContext.exit();
    }
}
