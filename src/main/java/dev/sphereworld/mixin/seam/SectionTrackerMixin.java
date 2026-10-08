package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.SectionTracker;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionTracker.class)
abstract class SectionTrackerMixin {
    @Unique private @Nullable PlanetGeometry sphereworld$geometry;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sphereworld$capturePlanet(CallbackInfo ci) {
        sphereworld$geometry = PlanetWrap.CONSTRUCTING.get();
    }

    @Redirect(method = {"checkNeighborsAfterUpdate", "getComputedLevel"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;offset(JIII)J"))
    private long sphereworld$wrapNeighbour(long node, int dx, int dy, int dz) {
        long neighbour = SectionPos.offset(node, dx, dy, dz);
        PlanetGeometry g = sphereworld$geometry;
        if (g == null) return neighbour;
        return SectionPos.asLong(g.canonicalChunk(SectionPos.x(neighbour)), SectionPos.y(neighbour),
                g.canonicalChunk(SectionPos.z(neighbour)));
    }
}
