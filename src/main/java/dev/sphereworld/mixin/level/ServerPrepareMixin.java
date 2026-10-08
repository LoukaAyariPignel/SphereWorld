package dev.sphereworld.mixin.level;

import dev.sphereworld.atlas.PlanetAtlases;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
abstract class ServerPrepareMixin {
    @Inject(method = "prepareLevels", at = @At("HEAD"))
    private void sphereworld$buildAtlases(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (!server.isDedicatedServer()) PlanetAtlases.buildBeforeStart(server);
    }
}
