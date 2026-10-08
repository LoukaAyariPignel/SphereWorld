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

    private int reach() {
        return Math.min(viewDistance + 1, geometry.halfChunks() - 1);
    }

    @Override
    public void forEach(Consumer<ChunkPos> consumer) {
        int r = reach();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (ChunkTrackingView.isWithinDistance(0, 0, viewDistance, dx, dz, true)) {
                    consumer.accept(new ChunkPos(geometry.canonicalChunk(center.x() + dx),
                            geometry.canonicalChunk(center.z() + dz)));
                }
            }
        }
    }

    public static void difference(ChunkTrackingView from, ChunkTrackingView to, Consumer<ChunkPos> onEnter, Consumer<ChunkPos> onLeave) {
        if (from.equals(to)) return;
        if (from instanceof PlanetTrackingView last && to instanceof PlanetTrackingView next && last.geometry.equals(next.geometry)) {
            PlanetGeometry g = next.geometry;
            int offsetX = g.chunkDelta(next.center.x(), last.center.x());
            int offsetZ = g.chunkDelta(next.center.z(), last.center.z());
            int minX = Math.min(-next.reach(), offsetX - last.reach());
            int maxX = Math.max(next.reach(), offsetX + last.reach());
            int minZ = Math.min(-next.reach(), offsetZ - last.reach());
            int maxZ = Math.max(next.reach(), offsetZ + last.reach());
            if (maxX - minX < g.chunks() && maxZ - minZ < g.chunks()) {
                for (int dx = minX; dx <= maxX; dx++) {
                    int x = g.canonicalChunk(next.center.x() + dx);
                    for (int dz = minZ; dz <= maxZ; dz++) {
                        int z = g.canonicalChunk(next.center.z() + dz);
                        boolean saw = last.contains(x, z, true);
                        boolean sees = next.contains(x, z, true);
                        if (saw != sees) (sees ? onEnter : onLeave).accept(new ChunkPos(x, z));
                    }
                }
                return;
            }
        }
        LongOpenHashSet before = collect(from);
        LongOpenHashSet after = collect(to);
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
