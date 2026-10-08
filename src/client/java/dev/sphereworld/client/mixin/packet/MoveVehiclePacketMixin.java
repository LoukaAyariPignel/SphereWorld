package dev.sphereworld.client.mixin.packet;

import dev.sphereworld.client.ClientPlanet;
import net.minecraft.core.PositionAndRotation;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundMoveVehiclePacket.class)
abstract class MoveVehiclePacketMixin {
    @Inject(method = "movingTo", at = @At("RETURN"), cancellable = true)
    private void sphereworld$movingTo(CallbackInfoReturnable<PositionAndRotation> cir) {
        PositionAndRotation value = cir.getReturnValue();
        Vec3 mapped = ClientPlanet.vec(value.position());
        if (mapped != value.position()) {
            cir.setReturnValue(PositionAndRotation.of(mapped, value.yRot(), value.xRot()));
        }
    }
}
