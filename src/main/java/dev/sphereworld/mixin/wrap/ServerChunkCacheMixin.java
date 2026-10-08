package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerChunkCache.class)
abstract class ServerChunkCacheMixin {
    @Shadow @Final private ServerLevel level;

    @ModifyVariable(method = {"getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            "getChunkNow", "getChunkFuture", "hasChunk", "getChunkForLighting"},
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sphereworld$wrapChunkX(int x) {
        PlanetGeometry g = Planets.of(level);
        return g == null ? x : g.canonicalChunk(x);
    }

    @ModifyVariable(method = {"getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            "getChunkNow", "getChunkFuture", "hasChunk", "getChunkForLighting"},
            at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int sphereworld$wrapChunkZ(int z) {
        PlanetGeometry g = Planets.of(level);
        return g == null ? z : g.canonicalChunk(z);
    }
}
