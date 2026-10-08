package dev.sphereworld.client.render;

import dev.sphereworld.atlas.PlanetAtlas;
import dev.sphereworld.net.PlanetAtlasUpdatePayload;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class ClientAtlases {
    private static final Map<Identifier, PlanetAtlas> ATLASES = new ConcurrentHashMap<>();
    private static final Map<Identifier, Long> VERSIONS = new ConcurrentHashMap<>();

    private ClientAtlases() {
    }

    public static void accept(PlanetAtlas atlas) {
        ATLASES.put(atlas.dimension(), atlas);
        VERSIONS.merge(atlas.dimension(), 1L, Long::sum);
    }

    public static void update(PlanetAtlasUpdatePayload payload) {
        PlanetAtlas atlas = ATLASES.get(payload.dimension());
        if (atlas == null) return;
        int cells = atlas.heights().length;
        for (int i = 0; i < payload.cells().length; i++) {
            int index = payload.cells()[i];
            if (index < 0 || index >= cells) continue;
            atlas.heights()[index] = payload.heights()[i];
            atlas.colors()[index] = payload.colors()[i];
        }
        VERSIONS.merge(payload.dimension(), 1L, Long::sum);
    }

    public static void clear() {
        ATLASES.clear();
        VERSIONS.clear();
    }

    public static @Nullable PlanetAtlas get(Identifier dimension) {
        return ATLASES.get(dimension);
    }

    public static long version(Identifier dimension) {
        return VERSIONS.getOrDefault(dimension, 0L);
    }
}
