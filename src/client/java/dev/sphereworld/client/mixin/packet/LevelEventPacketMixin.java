package dev.sphereworld.client.mixin.packet;

import dev.sphereworld.client.ClientPlanet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;

@Mixin(ClientboundLevelEventPacket.class)
abstract class LevelEventPacketMixin {
    @Inject(method = "getPos", at = @At("RETURN"), cancellable = true)
    private void sphereworld$getPos(CallbackInfoReturnable<BlockPos> cir) {
        cir.setReturnValue(ClientPlanet.pos(cir.getReturnValue()));
    }
}
