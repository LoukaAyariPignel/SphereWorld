package dev.sphereworld.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import org.jspecify.annotations.Nullable;

public final class SeamContext {
    private static volatile @Nullable PlanetGeometry active;
    private static volatile @Nullable Thread thread;

    private SeamContext() {
    }

    public static void enter(@Nullable PlanetGeometry geometry) {
        thread = Thread.currentThread();
        active = geometry;
    }

    public static void exit() {
        active = null;
        thread = null;
    }

    public static @Nullable PlanetGeometry current() {
        PlanetGeometry g = active;
        return g != null && Thread.currentThread() == thread ? g : null;
    }
}
