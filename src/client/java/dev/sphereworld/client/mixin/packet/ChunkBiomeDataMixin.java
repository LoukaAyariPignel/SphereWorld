package dev.sphereworld.client.mixin.packet;

import dev.sphereworld.client.ClientPlanet;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundChunksBiomesPacket.ChunkBiomeData.class)
abstract class ChunkBiomeDataMixin {
    @Inject(method = "pos", at = @At("RETURN"), cancellable = true)
    private void sphereworld$pos(CallbackInfoReturnable<ChunkPos> cir) {
        cir.setReturnValue(ClientPlanet.chunk(cir.getReturnValue()));
    }
}
