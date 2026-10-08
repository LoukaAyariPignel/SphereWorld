package dev.sphereworld.planet;

import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class Planets {
    private Planets() {
    }

    public static @Nullable PlanetGeometry of(@Nullable Level level) {
        return level == null ? null : ((PlanetLevel) level).sphereworld$geometry();
    }

    public static boolean isPlanet(@Nullable Level level) {
        return of(level) != null;
    }
}
