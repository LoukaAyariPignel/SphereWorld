package dev.sphereworld.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import dev.sphereworld.SphereWorld;
import dev.sphereworld.atlas.PlanetAtlas;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public final class AtlasMeshes {
    private static final int QUAD_BYTES = 4 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize();

    public static final class Mesh {
        private final PlanetAtlas atlas;
        private final GpuBuffer buffer;
        private final int indexCount;
        private final int[] shadedColors;
        private long version;

        Mesh(PlanetAtlas atlas, GpuBuffer buffer, int indexCount, int[] shadedColors) {
            this.atlas = atlas;
            this.buffer = buffer;
            this.indexCount = indexCount;
            this.shadedColors = shadedColors;
        }

        public PlanetAtlas atlas() {
            return atlas;
        }

        public GpuBuffer buffer() {
            return buffer;
        }

        public int indexCount() {
            return indexCount;
        }

        public int[] shadedColors() {
            return shadedColors;
        }

        public long version() {
            return version;
        }
    }

    private record Built(PlanetAtlas atlas, int[] shadedColors, ByteBufferBuilder bytes, MeshData mesh) {
        void close() {
            mesh.close();
            bytes.close();
        }
    }

    private static final Map<Identifier, Mesh> MESHES = new HashMap<>();
    private static final Map<Identifier, CompletableFuture<Built>> BUILDING = new HashMap<>();

    private static volatile boolean clearRequested;

    private AtlasMeshes() {
    }

    public static @Nullable Mesh get(Identifier dimension) {
        if (clearRequested) clearNow();
        PlanetAtlas atlas = ClientAtlases.get(dimension);
        if (atlas == null) return null;
        CompletableFuture<Built> building = BUILDING.get(dimension);
        if (building != null && building.isDone()) {
            BUILDING.remove(dimension);
            Built built = building.getNow(null);
            if (built != null) upload(dimension, built);
        }
        Mesh mesh = MESHES.get(dimension);
        if ((mesh == null || mesh.atlas() != atlas) && !BUILDING.containsKey(dimension)) {
            ClientAtlases.takeChanges(dimension);
            PlanetAtlas snapshot = atlas.copy();
            BUILDING.put(dimension, CompletableFuture.supplyAsync(() -> build(atlas, snapshot), Util.backgroundExecutor())
                    .exceptionally(error -> {
                        SphereWorld.LOGGER.error("Could not build the distant planet mesh for {}", dimension, error);
                        return null;
                    }));
        }
        return mesh != null && mesh.atlas().size() == atlas.size() ? mesh : null;
    }

    public static void applyChanges() {
        if (clearRequested) clearNow();
        for (Map.Entry<Identifier, Mesh> entry : MESHES.entrySet()) {
            Mesh mesh = entry.getValue();
            if (ClientAtlases.get(entry.getKey()) != mesh.atlas()) continue;
            IntOpenHashSet changed = ClientAtlases.takeChanges(entry.getKey());
            if (changed != null && !changed.isEmpty()) patch(mesh, changed);
        }
    }

    private static void patch(Mesh mesh, IntOpenHashSet changed) {
        PlanetAtlas atlas = mesh.atlas();
        int n = atlas.size();
        IntOpenHashSet affected = new IntOpenHashSet(changed.size() * 4);
        changed.forEach(index -> {
            int row = index / n;
            int col = index % n;
            for (int dr = -1; dr <= 0; dr++) {
                for (int dc = -1; dc <= 0; dc++) {
                    affected.add(Math.floorMod(row + dr, n) * n + Math.floorMod(col + dc, n));
                }
            }
        });
        int[] quads = affected.toIntArray();
        Arrays.sort(quads);
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(quads.length * QUAD_BYTES)) {
            BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int quad : quads) {
                mesh.shadedColors[quad] = shade(atlas, quad);
                addQuad(builder, atlas, quad / n, quad % n, mesh.shadedColors[quad]);
            }
            try (MeshData data = builder.buildOrThrow()) {
                ByteBuffer vertices = data.vertexBuffer();
                CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                int start = 0;
                while (start < quads.length) {
                    int end = start + 1;
                    while (end < quads.length && quads[end] == quads[end - 1] + 1) end++;
                    encoder.writeToBuffer(mesh.buffer.slice((long) quads[start] * QUAD_BYTES, (long) (end - start) * QUAD_BYTES),
                            vertices.slice(start * QUAD_BYTES, (end - start) * QUAD_BYTES));
                    start = end;
                }
            }
        }
        mesh.version++;
    }

    private static void upload(Identifier dimension, Built built) {
        try {
            GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "SphereWorld planet " + dimension,
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, built.mesh().vertexBuffer());
            Mesh old = MESHES.put(dimension, new Mesh(built.atlas(), buffer, built.mesh().drawState().indexCount(), built.shadedColors()));
            if (old != null) old.buffer().close();
        } finally {
            built.close();
        }
    }

    private static Built build(PlanetAtlas source, PlanetAtlas snapshot) {
        int n = snapshot.size();
        int[] shaded = new int[n * n];
        ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(n * n * QUAD_BYTES);
        BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int quad = 0; quad < n * n; quad++) {
            shaded[quad] = shade(snapshot, quad);
            addQuad(builder, snapshot, quad / n, quad % n, shaded[quad]);
        }
        return new Built(source, shaded, bytes, builder.buildOrThrow());
    }

    private static void addQuad(BufferBuilder builder, PlanetAtlas atlas, int row, int col, int color) {
        int cell = atlas.cellSize();
        int half = atlas.circumference() / 2;
        float x = -half + col * cell + cell * 0.5F;
        float z = -half + row * cell + cell * 0.5F;
        if (color == 0) {
            for (int i = 0; i < 4; i++) builder.addVertex(x, atlas.seaLevel(), z).setUv(0, 0).setColor(0);
            return;
        }
        builder.addVertex(x, height(atlas, row, col), z).setUv(0, 0).setColor(color);
        builder.addVertex(x, height(atlas, row + 1, col), z).setUv(0, cell).setColor(color);
        builder.addVertex(x, height(atlas, row + 1, col + 1), z).setUv(cell, cell).setColor(color);
        builder.addVertex(x, height(atlas, row, col + 1), z).setUv(cell, 0).setColor(color);
    }

    private static int shade(PlanetAtlas atlas, int quad) {
        int n = atlas.size();
        int row = quad / n;
        int col = quad % n;
        int i10 = row * n + (col + 1) % n;
        int i01 = ((row + 1) % n) * n + col;
        int i11 = ((row + 1) % n) * n + (col + 1) % n;
        short[] h = atlas.heights();
        if (h[quad] == PlanetAtlas.VOID || h[i10] == PlanetAtlas.VOID || h[i01] == PlanetAtlas.VOID || h[i11] == PlanetAtlas.VOID) return 0;
        float slope = (h[i01] + h[i11] - h[quad] - h[i10]) / (2.0F * atlas.cellSize());
        return 0xFF000000 | scale(atlas.colors()[quad], Math.clamp(1.0F - slope * 1.5F, 0.6F, 1.25F));
    }

    static float height(PlanetAtlas atlas, int row, int col) {
        int n = atlas.size();
        int h = atlas.heights()[Math.floorMod(row, n) * n + Math.floorMod(col, n)];
        return h == PlanetAtlas.VOID ? atlas.seaLevel() : h;
    }

    static float height(short[] heights, int n, int seaLevel, int row, int col) {
        int h = heights[Math.floorMod(row, n) * n + Math.floorMod(col, n)];
        return h == PlanetAtlas.VOID ? seaLevel : h;
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
    }
}
