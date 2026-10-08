package dev.sphereworld.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

public interface PlanetLight {
    @Nullable PlanetGeometry sphereworld$geometry();

    static long wrap(@Nullable PlanetGeometry g, long node) {
        if (g == null) return node;
        int x = BlockPos.getX(node);
        int z = BlockPos.getZ(node);
        if (g.isCanonical(x) && g.isCanonical(z)) return node;
        return BlockPos.asLong(g.canonical(x), BlockPos.getY(node), g.canonical(z));
    }
}
