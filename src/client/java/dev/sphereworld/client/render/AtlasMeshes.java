package dev.sphereworld.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import dev.sphereworld.SphereWorld;
import dev.sphereworld.atlas.PlanetAtlas;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public final class AtlasMeshes {
    private static final long REBUILD_INTERVAL_MS = 2000;

    public record Mesh(PlanetAtlas atlas, long version, GpuBuffer buffer, int indexCount, int[] shadedColors) {
    }

    private record Built(PlanetAtlas atlas, long version, int[] shadedColors, @Nullable ByteBufferBuilder bytes, @Nullable MeshData mesh) {
        void close() {
            if (mesh != null) mesh.close();
            if (bytes != null) bytes.close();
        }
    }

    private static final Map<Identifier, Mesh> MESHES = new HashMap<>();
    private static final Map<Identifier, CompletableFuture<Built>> BUILDING = new HashMap<>();
    private static final Map<Identifier, Long> LAST_START = new HashMap<>();

    private static volatile boolean clearRequested;

    private AtlasMeshes() {
    }

    public static @Nullable Mesh get(Identifier dimension) {
        if (clearRequested) clearNow();
        PlanetAtlas atlas = ClientAtlases.get(dimension);
        if (atlas == null) return null;
        long version = ClientAtlases.version(dimension);
        CompletableFuture<Built> building = BUILDING.get(dimension);
        if (building != null && building.isDone()) {
            BUILDING.remove(dimension);
            Built built = building.getNow(null);
            if (built != null) upload(dimension, built);
        }
        Mesh mesh = MESHES.get(dimension);
        boolean stale = mesh == null || mesh.version() != version;
        long now = Util.getMillis();
        if (stale && !BUILDING.containsKey(dimension)
                && (mesh == null || mesh.atlas() != atlas || now - LAST_START.getOrDefault(dimension, 0L) > REBUILD_INTERVAL_MS)) {
            LAST_START.put(dimension, now);
            PlanetAtlas snapshot = atlas.copy();
            BUILDING.put(dimension, CompletableFuture.supplyAsync(() -> build(snapshot, version), Util.backgroundExecutor())
                    .exceptionally(error -> {
                        SphereWorld.LOGGER.error("Could not build the distant planet mesh for {}", dimension, error);
                        return null;
                    }));
        }

        return mesh != null && mesh.atlas().dimension().equals(dimension) && mesh.atlas().size() == atlas.size() ? mesh : null;
    }

    private static void upload(Identifier dimension, Built built) {
        try {
            if (built.mesh() == null) return;
            GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "SphereWorld planet " + dimension, 32, built.mesh().vertexBuffer());
            Mesh old = MESHES.put(dimension, new Mesh(built.atlas(), built.version(), buffer, built.mesh().drawState().indexCount(), built.shadedColors()));
            if (old != null) old.buffer().close();
        } finally {
            built.close();
        }
    }

    private static Built build(PlanetAtlas atlas, long version) {
        int n = atlas.size();
        int cell = atlas.cellSize();
        int half = atlas.circumference() / 2;
        int[] shaded = shade(atlas);
        ByteBufferBuilder bytes = new ByteBufferBuilder(n * n * 4 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize());
        BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int quads = 0;
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                int color = shaded[row * n + col];
                if (color == 0) continue;
                float x = -half + col * cell + cell * 0.5F;
                float z = -half + row * cell + cell * 0.5F;
                builder.addVertex(x, height(atlas, row, col), z).setUv(0, 0).setColor(color);
                builder.addVertex(x, height(atlas, row + 1, col), z).setUv(0, cell).setColor(color);
                builder.addVertex(x, height(atlas, row + 1, col + 1), z).setUv(cell, cell).setColor(color);
                builder.addVertex(x, height(atlas, row, col + 1), z).setUv(cell, 0).setColor(color);
                quads++;
            }
        }
        if (quads == 0) {
            bytes.close();
            return new Built(atlas, version, shaded, null, null);
        }
        return new Built(atlas, version, shaded, bytes, builder.buildOrThrow());
    }

    private static int[] shade(PlanetAtlas atlas) {
        int n = atlas.size();
        int cell = atlas.cellSize();
        int[] shaded = new int[n * n];
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                int i00 = row * n + col;
                int i10 = row * n + (col + 1) % n;
                int i01 = ((row + 1) % n) * n + col;
                int i11 = ((row + 1) % n) * n + (col + 1) % n;
                short[] h = atlas.heights();
                if (h[i00] == PlanetAtlas.VOID || h[i10] == PlanetAtlas.VOID || h[i01] == PlanetAtlas.VOID || h[i11] == PlanetAtlas.VOID) continue;
                float slope = (h[i01] + h[i11] - h[i00] - h[i10]) / (2.0F * cell);
                shaded[i00] = 0xFF000000 | scale(atlas.colors()[i00], Math.clamp(1.0F - slope * 1.5F, 0.6F, 1.25F));
            }
        }
        return shaded;
    }

    static float height(PlanetAtlas atlas, int row, int col) {
        int n = atlas.size();
        int h = atlas.heights()[Math.floorMod(row, n) * n + Math.floorMod(col, n)];
        return h == PlanetAtlas.VOID ? atlas.seaLevel() : h;
    }

    static int scale(int rgb, float factor) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * factor));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * factor));
        int b = Math.min(255, (int) ((rgb & 255) * factor));
        return (r << 16) | (g << 8) | b;
    }

    public static void clear() {
        clearRequested = true;
    }

    private static void clearNow() {
        clearRequested = false;
        MESHES.values().forEach(mesh -> mesh.buffer().close());
        MESHES.clear();
        BUILDING.values().forEach(future -> future.thenAccept(built -> {
            if (built != null) built.close();
        }));
        BUILDING.clear();
        LAST_START.clear();
    }
}
