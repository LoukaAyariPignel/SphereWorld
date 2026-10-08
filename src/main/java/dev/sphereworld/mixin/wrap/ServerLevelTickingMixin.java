package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
abstract class ServerLevelTickingMixin {
    @ModifyVariable(method = "shouldTickBlocksAt(J)Z", at = @At("HEAD"), argsOnly = true)
    private long sphereworld$canonicalChunk(long chunkPos) {
        PlanetGeometry g = Planets.of((ServerLevel) (Object) this);
        if (g == null) return chunkPos;
        return ChunkPos.pack(g.canonicalChunk(ChunkPos.getX(chunkPos)), g.canonicalChunk(ChunkPos.getZ(chunkPos)));
    }

    @ModifyVariable(method = "isPositionEntityTicking", at = @At("HEAD"), argsOnly = true)
    private BlockPos sphereworld$canonicalPos(BlockPos pos) {
        PlanetGeometry g = PlanetWrap.server((ServerLevel) (Object) this);
        return g == null ? pos : PlanetWrap.canonical(g, pos);
    }

    @ModifyVariable(method = "canSpawnEntitiesInChunk", at = @At("HEAD"), argsOnly = true)
    private ChunkPos sphereworld$canonicalChunkPos(ChunkPos pos) {
        PlanetGeometry g = Planets.of((ServerLevel) (Object) this);
        return g == null ? pos : new ChunkPos(g.canonicalChunk(pos.x()), g.canonicalChunk(pos.z()));
    }
}
