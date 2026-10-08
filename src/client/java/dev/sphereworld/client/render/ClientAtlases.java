package dev.sphereworld.client.render;

import dev.sphereworld.atlas.PlanetAtlas;
import dev.sphereworld.net.PlanetAtlasUpdatePayload;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class ClientAtlases {
    private static final Map<Identifier, PlanetAtlas> ATLASES = new ConcurrentHashMap<>();
    private static final Map<Identifier, IntOpenHashSet> CHANGES = new ConcurrentHashMap<>();

    private ClientAtlases() {
    }

    public static void accept(PlanetAtlas atlas) {
        ATLASES.put(atlas.dimension(), atlas);
        CHANGES.remove(atlas.dimension());
    }

    public static void update(PlanetAtlasUpdatePayload payload) {
        PlanetAtlas atlas = ATLASES.get(payload.dimension());
        if (atlas == null) return;
        int cells = atlas.heights().length;
        IntOpenHashSet changed = CHANGES.computeIfAbsent(payload.dimension(), key -> new IntOpenHashSet());
        for (int i = 0; i < payload.cells().length; i++) {
            int index = payload.cells()[i];
            if (index < 0 || index >= cells) continue;
            atlas.heights()[index] = payload.heights()[i];
            atlas.colors()[index] = payload.colors()[i];
            changed.add(index);
        }
    }

    public static @Nullable IntOpenHashSet takeChanges(Identifier dimension) {
        return CHANGES.remove(dimension);
    }

    public static void clear() {
        ATLASES.clear();
        CHANGES.clear();
    }

    public static @Nullable PlanetAtlas get(Identifier dimension) {
        return ATLASES.get(dimension);
    }
}
