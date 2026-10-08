package dev.sphereworld.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.Nullable;

public final class SeamDistances {
    private SeamDistances() {
    }

    public static double blockDistSqr(@Nullable PlanetGeometry g, BlockPos a, Vec3i b) {
        if (g == null) return a.distSqr(b);
        double dx = g.delta(a.getX(), b.getX());
        double dy = b.getY() - a.getY();
        double dz = g.delta(a.getZ(), b.getZ());
        return dx * dx + dy * dy + dz * dz;
    }
}
