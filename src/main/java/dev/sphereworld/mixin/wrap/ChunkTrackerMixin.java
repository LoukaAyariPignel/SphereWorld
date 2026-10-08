package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.server.level.ChunkTracker;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkTracker.class)
abstract class ChunkTrackerMixin {
    @Unique private @Nullable PlanetGeometry sphereworld$geometry;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sphereworld$capturePlanet(CallbackInfo ci) {
        sphereworld$geometry = PlanetWrap.CONSTRUCTING.get();
    }

    @Redirect(method = {"checkNeighborsAfterUpdate", "getComputedLevel"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;pack(II)J"))
    private long sphereworld$wrapNeighbour(int x, int z) {
        PlanetGeometry g = sphereworld$geometry;
        return g == null ? ChunkPos.pack(x, z) : ChunkPos.pack(g.canonicalChunk(x), g.canonicalChunk(z));
    }
}
