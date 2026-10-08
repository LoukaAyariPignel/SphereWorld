package dev.sphereworld.mixin.level;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.PlanetLevel;
import dev.sphereworld.planet.PlanetServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void sphereworld$bindPlanet(CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        PlanetGeometry geometry = PlanetServer.geometry(self.getServer(), self.dimension());
        if (geometry != null) {
            ((PlanetLevel) this).sphereworld$setGeometry(geometry);
            ((dev.sphereworld.wrap.PlanetTicks) self.getBlockTicks()).sphereworld$setGeometry(geometry);
            ((dev.sphereworld.wrap.PlanetTicks) self.getFluidTicks()).sphereworld$setGeometry(geometry);
            ((dev.sphereworld.wrap.PlanetRaids) self.getRaids()).sphereworld$setGeometry(geometry);
            ((dev.sphereworld.wrap.PlanetPoi) self.getPoiManager()).sphereworld$setGeometry(geometry);
            SphereWorld.LOGGER.info("{} is a planet: circumference {} blocks (radius {})",
                    self.dimension().identifier(), geometry.circumference(), Math.round(geometry.radius()));
        }
    }
}
