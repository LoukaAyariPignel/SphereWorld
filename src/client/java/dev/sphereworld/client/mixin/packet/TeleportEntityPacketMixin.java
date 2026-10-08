package dev.sphereworld.client.mixin.packet;

import dev.sphereworld.client.ClientPlanet;
import dev.sphereworld.client.PacketPositions;
import java.util.Set;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundTeleportEntityPacket.class)
abstract class TeleportEntityPacketMixin {
    @Shadow public abstract Set<Relative> relatives();

    @Inject(method = "change", at = @At("RETURN"), cancellable = true)
    private void sphereworld$change(CallbackInfoReturnable<PositionMoveRotation> cir) {
        cir.setReturnValue(PacketPositions.map(cir.getReturnValue(), relatives()));
    }
}
