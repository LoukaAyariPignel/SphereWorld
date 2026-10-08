package dev.sphereworld.client.mixin.packet;

import dev.sphereworld.client.ClientPlanet;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
abstract class SectionBlocksUpdatePacketMixin {
    @ModifyVariable(method = "runUpdates", at = @At("HEAD"), argsOnly = true)
    private BiConsumer<BlockPos, BlockState> sphereworld$mapUpdates(BiConsumer<BlockPos, BlockState> consumer) {
        if (ClientPlanet.geometry() == null) return consumer;
        return (pos, state) -> consumer.accept(ClientPlanet.pos(pos.immutable()), state);
    }
}
