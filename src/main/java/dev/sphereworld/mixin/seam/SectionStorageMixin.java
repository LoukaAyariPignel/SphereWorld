package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetPoi;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SectionStorage.class)
abstract class SectionStorageMixin {
    @ModifyVariable(method = "getOrLoad", at = @At("HEAD"), argsOnly = true)
    private long sphereworld$canonicalSection(long key) {
        if (!((Object) this instanceof PlanetPoi poi)) return key;
        PlanetGeometry g = poi.sphereworld$geometry();
        if (g == null) return key;
        int x = SectionPos.x(key);
        int z = SectionPos.z(key);
        int cx = g.canonicalChunk(x);
        int cz = g.canonicalChunk(z);
        return cx == x && cz == z ? key : SectionPos.asLong(cx, SectionPos.y(key), cz);
    }
}
