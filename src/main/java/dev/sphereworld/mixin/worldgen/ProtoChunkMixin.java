package dev.sphereworld.mixin.worldgen;

import dev.sphereworld.worldgen.stacked.BandViews;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ProtoChunk.class)
abstract class ProtoChunkMixin implements BandViews {
    @Unique private final ProtoChunk[] sphereworld$bandViews = new ProtoChunk[3];

    @Override
    public ProtoChunk[] sphereworld$bandViews() {
        return sphereworld$bandViews;
    }
}
