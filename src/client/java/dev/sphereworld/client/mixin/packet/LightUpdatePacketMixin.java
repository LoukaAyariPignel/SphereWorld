package dev.sphereworld.client.mixin.packet;

import dev.sphereworld.client.ClientPlanet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;

@Mixin(ClientboundLightUpdatePacket.class)
abstract class LightUpdatePacketMixin {
    @Inject(method = "x", at = @At("RETURN"), cancellable = true)
    private void sphereworld$x(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ClientPlanet.chunkX(cir.getReturnValue()));
    }

    @Inject(method = "z", at = @At("RETURN"), cancellable = true)
    private void sphereworld$z(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ClientPlanet.chunkZ(cir.getReturnValue()));
    }
}
