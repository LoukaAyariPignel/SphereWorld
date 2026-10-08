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
import dev.sphereworld.net.PlanetDetailPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public final class DetailMeshes {
    public record Mesh(int level, int centreX, int centreZ, float half, GpuBuffer buffer, int indexCount) {
    }

    private record Built(PlanetDetailPayload patch, @Nullable ByteBufferBuilder bytes, @Nullable MeshData mesh) {
        void close() {
            if (mesh != null) mesh.close();
            if (bytes != null) bytes.close();
        }
    }

    private static final Map<String, PlanetDetailPayload> RECEIVED = new ConcurrentHashMap<>();
    private static final Map<String, Mesh> MESHES = new HashMap<>();
    private static final Map<String, CompletableFuture<Built>> BUILDING = new HashMap<>();
    private static volatile boolean clearRequested;

    private DetailMeshes() {
    }

    private static String key(Identifier dimension, int level) {
        return dimension + "#" + level;
    }

    public static void accept(PlanetDetailPayload patch) {
        RECEIVED.put(key(patch.dimension(), patch.level()), patch);
    }

    public static @Nullable Mesh get(Identifier dimension, int level, int seaLevel) {
        if (clearRequested) clearNow();
        String key = key(dimension, level);
        CompletableFuture<Built> building = BUILDING.get(key);
        if (building != null && building.isDone()) {
            BUILDING.remove(key);
            Built built = building.getNow(null);
            if (built != null) upload(key, built);
        }
        PlanetDetailPayload patch = RECEIVED.get(key);
        Mesh mesh = MESHES.get(key);
        boolean current = mesh != null && patch != null && mesh.centreX() == patch.centreX() && mesh.centreZ() == patch.centreZ();
        if (patch != null && !current && !BUILDING.containsKey(key)) {
            RECEIVED.remove(key, patch);
            BUILDING.put(key, CompletableFuture.supplyAsync(() -> build(patch, seaLevel), Util.backgroundExecutor())
                    .exceptionally(error -> {
                        SphereWorld.LOGGER.error("Could not build the planet's detail mesh", error);
                        return null;
                    }));
        }
        return mesh;
    }

    private static void upload(String key, Built built) {
        try {
            if (built.mesh() == null) return;
            PlanetDetailPayload patch = built.patch();
            GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "SphereWorld planet detail " + key, 32, built.mesh().vertexBuffer());

            float half = patch.cell() * patch.grid() / 2.0F - patch.cell();
            Mesh old = MESHES.put(key, new Mesh(patch.level(), patch.centreX(), patch.centreZ(), half, buffer, built.mesh().drawState().indexCount()));
            if (old != null) old.buffer().close();
        } finally {
            built.close();
        }
    }

    private static Built build(PlanetDetailPayload patch, int seaLevel) {
        int n = patch.grid();
        int cell = patch.cell();
        int half = cell * n / 2;
        short[] h = patch.heights();
        ByteBufferBuilder bytes = new ByteBufferBuilder(n * n * 4 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize());
        BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int quads = 0;
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                int r1 = Math.min(n - 1, row + 1);
                int c1 = Math.min(n - 1, col + 1);
                int i00 = row * n + col, i10 = row * n + c1, i01 = r1 * n + col, i11 = r1 * n + c1;
                if (h[i00] == PlanetAtlas.VOID || h[i10] == PlanetAtlas.VOID || h[i01] == PlanetAtlas.VOID || h[i11] == PlanetAtlas.VOID) continue;

                float slope = (h[i01] + h[i11] - h[i00] - h[i10]) / (2.0F * cell);
                int color = 0xFF000000 | AtlasMeshes.scale(patch.colors()[i00], Math.clamp(1.0F - slope * 1.5F, 0.6F, 1.25F));
                float x = patch.centreX() - half + col * cell + cell * 0.5F;
                float z = patch.centreZ() - half + row * cell + cell * 0.5F;
                builder.addVertex(x, h[i00], z).setUv(0, 0).setColor(color);
                builder.addVertex(x, h[i01], z).setUv(0, cell).setColor(color);
                builder.addVertex(x, h[i11], z).setUv(cell, cell).setColor(color);
                builder.addVertex(x, h[i10], z).setUv(cell, 0).setColor(color);
                quads++;
            }
        }
        if (quads == 0) {
            bytes.close();
            return new Built(patch, null, null);
        }
        return new Built(patch, bytes, builder.buildOrThrow());
    }

    public static void clear() {
        RECEIVED.clear();
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
