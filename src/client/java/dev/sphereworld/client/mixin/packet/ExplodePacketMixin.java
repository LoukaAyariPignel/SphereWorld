package dev.sphereworld.client.mixin.packet;

import dev.sphereworld.client.ClientPlanet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;

@Mixin(ClientboundExplodePacket.class)
abstract class ExplodePacketMixin {
    @Inject(method = "center", at = @At("RETURN"), cancellable = true)
    private void sphereworld$center(CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(ClientPlanet.vec(cir.getReturnValue()));
    }
}
