package dev.sphereworld.planet;

import org.jspecify.annotations.Nullable;

public interface PlanetLevel {
    @Nullable PlanetGeometry sphereworld$geometry();

    void sphereworld$setGeometry(@Nullable PlanetGeometry geometry);
}
