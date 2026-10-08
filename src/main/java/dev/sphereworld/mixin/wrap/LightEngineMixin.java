package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetLight;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.world.level.lighting.LightEngine;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightEngine.class)
abstract class LightEngineMixin implements PlanetLight {
    @Unique private @Nullable PlanetGeometry sphereworld$geometry;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sphereworld$capture(CallbackInfo ci) {
        sphereworld$geometry = PlanetWrap.CONSTRUCTING.get();
    }

    @Override
    public @Nullable PlanetGeometry sphereworld$geometry() {
        return sphereworld$geometry;
    }
}
