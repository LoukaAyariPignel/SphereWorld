package dev.sphereworld.client.mixin.packet;

import dev.sphereworld.client.ClientPlanet;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.PositionStep;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundEntityPositionSyncPacket.class)
abstract class EntityPositionSyncPacketMixin {
    @Inject(method = "position", at = @At("RETURN"), cancellable = true)
    private void sphereworld$position(CallbackInfoReturnable<PositionPath> cir) {
        PositionPath path = cir.getReturnValue();
        Vec3 end = path.endPosition();
        Vec3 mapped = ClientPlanet.vec(end);
        if (mapped == end) return;
        Vec3 offset = mapped.subtract(end);
        if (path instanceof PositionPath.Stepped stepped) {
            List<PositionStep> steps = stepped.steps().stream()
                    .map(step -> new PositionStep(step.position().add(offset), step.tickOffset()))
                    .toList();
            cir.setReturnValue(PositionPath.stepped(steps));
        } else {
            cir.setReturnValue(PositionPath.of(mapped));
        }
    }
}
