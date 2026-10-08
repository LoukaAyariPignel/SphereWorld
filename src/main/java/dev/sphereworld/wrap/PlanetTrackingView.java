package dev.sphereworld.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.function.Consumer;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

public record PlanetTrackingView(ChunkPos center, int viewDistance, PlanetGeometry geometry) implements ChunkTrackingView {
    @Override
    public boolean contains(int chunkX, int chunkZ, boolean includeNeighbors) {
        int dx = geometry.chunkDelta(center.x(), chunkX);
        int dz = geometry.chunkDelta(center.z(), chunkZ);
        return ChunkTrackingView.isWithinDistance(0, 0, viewDistance, dx, dz, includeNeighbors);
    }

    @Override
    public void forEach(Consumer<ChunkPos> consumer) {
        int r = Math.min(viewDistance + 1, geometry.halfChunks() - 1);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (ChunkTrackingView.isWithinDistance(0, 0, viewDistance, dx, dz, true)) {
                    consumer.accept(new ChunkPos(geometry.canonicalChunk(center.x() + dx),
                            geometry.canonicalChunk(center.z() + dz)));
                }
            }
        }
    }

    private LongOpenHashSet keys() {
        LongOpenHashSet set = new LongOpenHashSet();
        forEach(pos -> set.add(pos.pack()));
        return set;
    }

    public static void difference(ChunkTrackingView from, ChunkTrackingView to, Consumer<ChunkPos> onEnter, Consumer<ChunkPos> onLeave) {
        if (from.equals(to)) return;
        LongOpenHashSet before = from instanceof PlanetTrackingView p ? p.keys() : collect(from);
        LongOpenHashSet after = to instanceof PlanetTrackingView p ? p.keys() : collect(to);
        before.forEach(key -> {
            if (!after.contains(key)) onLeave.accept(ChunkPos.unpack(key));
        });
        after.forEach(key -> {
            if (!before.contains(key)) onEnter.accept(ChunkPos.unpack(key));
        });
    }

    private static LongOpenHashSet collect(ChunkTrackingView view) {
        LongOpenHashSet set = new LongOpenHashSet();
        view.forEach(pos -> set.add(pos.pack()));
        return set;
    }
}
