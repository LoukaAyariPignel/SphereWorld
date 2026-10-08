package dev.sphereworld.client;

import dev.sphereworld.net.PlanetSyncPayload;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.PlanetLevel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class ClientPlanets {
    private static final Map<Identifier, PlanetGeometry> PLANETS = new HashMap<>();
    private static List<Identifier> stack = List.of();

    private ClientPlanets() {
    }

    public static synchronized void accept(PlanetSyncPayload payload) {
        PLANETS.clear();
        for (PlanetSyncPayload.Entry entry : payload.planets()) {
            PLANETS.put(entry.dimension(), new PlanetGeometry(entry.circumference(), entry.surfaceY()));
        }
        stack = List.copyOf(payload.stack());
    }

    public static synchronized void clear() {
        PLANETS.clear();
        stack = List.of();
    }

    public static synchronized @Nullable PlanetGeometry get(Identifier dimension) {
        return PLANETS.get(dimension);
    }

    public static synchronized List<Identifier> stack() {
        return stack;
    }

    public static void bind(@Nullable ClientLevel level) {
        if (level != null) {
            ((PlanetLevel) level).sphereworld$setGeometry(get(level.dimension().identifier()));
        }
    }
}
