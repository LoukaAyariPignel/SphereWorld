package dev.sphereworld.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import org.jspecify.annotations.Nullable;

public interface PlanetPoi {
    void sphereworld$setGeometry(@Nullable PlanetGeometry geometry);

    @Nullable PlanetGeometry sphereworld$geometry();
}
