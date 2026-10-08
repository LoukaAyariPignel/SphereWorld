package dev.sphereworld.client.mixin.packet;

import dev.sphereworld.client.ClientPlanet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;

@Mixin(ClientboundForgetLevelChunkPacket.class)
abstract class ForgetLevelChunkPacketMixin {
    @Inject(method = "pos", at = @At("RETURN"), cancellable = true)
    private void sphereworld$pos(CallbackInfoReturnable<ChunkPos> cir) {
        cir.setReturnValue(ClientPlanet.chunk(cir.getReturnValue()));
    }
}
