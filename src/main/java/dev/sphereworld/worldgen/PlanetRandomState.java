package dev.sphereworld.worldgen;

import dev.sphereworld.planet.PlanetGeometry;
import org.jspecify.annotations.Nullable;

public interface PlanetRandomState {
    default void sphereworld$attachPlanet(PlanetGeometry geometry) {
        sphereworld$attachPlanet(geometry, ClimateWindow.VANILLA);
    }

    void sphereworld$attachPlanet(PlanetGeometry geometry, ClimateWindow window);

    @Nullable PlanetGeometry sphereworld$planet();

    void sphereworld$setBoundaries(dev.sphereworld.stack.StackTravel.@Nullable Boundaries boundaries);

    dev.sphereworld.stack.StackTravel.@Nullable Boundaries sphereworld$boundaries();
}
