package dev.sphereworld.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.sphereworld.SphereWorld;
import dev.sphereworld.atlas.PlanetAtlas;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import java.util.concurrent.CompletableFuture;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public final class PlanetLodRenderer {
    public static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(SphereWorld.id("pipeline/planet_lod"))
                    .withVertexShader(SphereWorld.id("core/planet_lod"))
                    .withFragmentShader(SphereWorld.id("core/planet_lod"))
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withColorTargetState(ColorTargetState.DEFAULT)
                    .withCull(false)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .build());

    private static final RenderSystem.AutoStorageIndexBuffer QUAD_INDICES = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
    private static final double PACK_REBUILD_DISTANCE = 48.0;
    private static final long PACK_REFRESH_MILLIS = 5000;

    private static final boolean VOXY = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("voxy");
    private static final float UNDER_VOXY = VOXY ? 12.0F : 0.0F;

    private record PackBuilt(Vec3 origin, AtlasMeshes.Mesh source, long version, @Nullable ByteBufferBuilder bytes, @Nullable MeshData mesh) {
        void close() {
            if (mesh != null) mesh.close();
            if (bytes != null) bytes.close();
        }
    }

    private static @Nullable GpuBuffer packBuffer;
    private static int packIndexCount;
    private static @Nullable Vec3 packOrigin;
    private static AtlasMeshes.@Nullable Mesh packSource;
    private static long packVersion;
    private static long packStarted;
    private static @Nullable CompletableFuture<PackBuilt> packBuilding;
    private static volatile boolean closeRequested;

    private PlanetLodRenderer() {
    }

    public static void init() {
        IrisLodSupport.init();
    }

    public static void render(RenderPass pass) {
        if (closeRequested) closeNow();
        RemotePlanetRenderer.render(pass);
        if (!VOXY) renderPlanet(pass);
    }

    public static void renderAfterTerrain(RenderPass pass) {
        if (VOXY) renderPlanet(pass);
    }

    private static void renderPlanet(RenderPass pass) {
        Minecraft client = Minecraft.getInstance();
        AtlasMeshes.Mesh mesh = currentMesh(client);
        if (mesh == null) return;
        if (IrisLodSupport.shaderPackInUse()) {
            if (IrisLodSupport.renderingShadowPass()) return;
            PlanetGeometry geometry = Planets.of(client.level);
            if (geometry != null) renderForPack(pass, client, geometry, mesh);
            return;
        }
        Matrix4f modelView = RenderSystem.getModelViewMatrixCopy().translate(0.0F, -UNDER_VOXY, 0.0F);

        var dimension = client.level.dimension().identifier();
        DetailMeshes.Mesh fine = DetailMeshes.get(dimension, 0, mesh.atlas().seaLevel());
        DetailMeshes.Mesh mid = DetailMeshes.get(dimension, 1, mesh.atlas().seaLevel());
        draw(pass, PIPELINE, lodTransform(modelView, mid != null ? mid : fine, null), mesh.buffer(), mesh.indexCount());
        if (mid != null) draw(pass, PIPELINE, lodTransform(modelView, fine, mid), mid.buffer(), mid.indexCount());
        if (fine != null) draw(pass, PIPELINE, lodTransform(modelView, null, fine), fine.buffer(), fine.indexCount());
    }

    private static GpuBufferSlice lodTransform(Matrix4f modelView, DetailMeshes.@Nullable Mesh finer, DetailMeshes.@Nullable Mesh own) {
        Matrix4f lod = new Matrix4f().zero();
        lod.setColumn(0, new Vector4f(finer == null ? 0.0F : finer.centreX(), finer == null ? 0.0F : finer.centreZ(),
                finer == null ? 0.0F : finer.half(), own == null ? 0.0F : own.half()));
        lod.setColumn(1, new Vector4f(own == null ? 0.0F : own.centreX(), own == null ? 0.0F : own.centreZ(), 0.0F, 0.0F));
        return RenderSystem.getDynamicUniforms().writeTransform(modelView, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new org.joml.Vector3f(), lod);
    }

    private static AtlasMeshes.@Nullable Mesh currentMesh(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || level.dimensionType().hasCeiling() || Planets.of(level) == null) return null;
        PlanetAtlas atlas = ClientAtlases.get(level.dimension().identifier());
        if (atlas == null) return null;

        if (client.gameRenderer.mainCamera().position().y < atlas.seaLevel() - 16) return null;
        return AtlasMeshes.get(level.dimension().identifier());
    }

    public static int packHorizonDistance() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || level.dimensionType().hasCeiling() || !IrisLodSupport.shaderPackInUse()) return 0;
        PlanetGeometry geometry = Planets.of(level);
        PlanetAtlas current = ClientAtlases.get(level.dimension().identifier());
        if (geometry == null || current == null) return 0;
        double cameraY = client.gameRenderer.mainCamera().position().y;
        if (cameraY < current.seaLevel() - 16) return 0;
        double r = geometry.radius();
        double h = Math.max(1.0, cameraY - geometry.surfaceY());
        double reach = Math.sqrt(2.0 * r * h + h * h) + Math.sqrt(2.0 * r * 128.0);
        return (int) Math.min(reach, Math.PI * r);
    }

    private static void renderForPack(RenderPass pass, Minecraft client, PlanetGeometry geometry, AtlasMeshes.Mesh mesh) {
        Vec3 camera = client.gameRenderer.mainCamera().position();
        if (packBuilding != null && packBuilding.isDone()) {
            PackBuilt built = packBuilding.getNow(null);
            packBuilding = null;
            if (built != null) uploadPack(built);
        }
        boolean moved = packOrigin == null || packOrigin.distanceTo(camera) > PACK_REBUILD_DISTANCE;
        boolean replaced = packSource != mesh;
        boolean refreshed = packVersion != mesh.version() && Util.getMillis() - packStarted > PACK_REFRESH_MILLIS;
        if (packBuilding == null && (moved || replaced || refreshed)) {
            packStarted = Util.getMillis();
            Vec3 origin = new Vec3(Math.floor(camera.x), Math.floor(camera.y), Math.floor(camera.z));
            double hole = Math.max(16.0, client.options.getEffectiveRenderDistance() * 16.0 - 24.0);
            PlanetAtlas atlas = mesh.atlas();
            short[] heights = atlas.heights().clone();
            int[] colors = mesh.shadedColors().clone();
            long version = mesh.version();
            packBuilding = CompletableFuture.supplyAsync(() -> buildAroundCamera(geometry, atlas, heights, colors, origin, hole, mesh, version),
                    Util.backgroundExecutor()).exceptionally(error -> {
                        SphereWorld.LOGGER.error("Could not build the distant planet mesh for the shader pack", error);
                        return null;
                    });
        }
        if (packBuffer == null || packIndexCount == 0 || packOrigin == null) return;

        Matrix4f modelView = client.gameRenderer.mainCamera().getViewRotationMatrix(new Matrix4f())
                .translate((float) (packOrigin.x - camera.x), (float) (packOrigin.y - camera.y) - UNDER_VOXY, (float) (packOrigin.z - camera.z));
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(modelView, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F));
        var white = IrisLodSupport.whiteIfReady();
        if (white == null) return;
        pass.setPipeline(RenderSystem.getCompiledPipeline(IrisLodSupport.PIPELINE));
        pass.setUniform("Sampler0", white.getTextureView(), white.getSampler());
        draw(pass, IrisLodSupport.PIPELINE, transforms, packBuffer, packIndexCount);
    }

    private static void draw(RenderPass pass, RenderPipeline pipeline, GpuBufferSlice transforms, GpuBuffer buffer, int indexCount) {
        pass.pushDebugGroup(() -> "SphereWorld planet");
        pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
        RenderSystem.bindDefaultUniforms(pass);
        pass.setUniform("DynamicTransforms", transforms);
        if (pipeline == PIPELINE) {
            pass.setUniform("Sampler2", Minecraft.getInstance().gameRenderer.levelLightmap(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        }
        pass.setVertexBuffer(0, buffer.slice());
        pass.setIndexBuffer(QUAD_INDICES.getBuffer(indexCount), QUAD_INDICES.type());
        pass.drawIndexed(indexCount, 1, 0, 0, 0);
        pass.popDebugGroup();
    }

    private static PackBuilt buildAroundCamera(PlanetGeometry geometry, PlanetAtlas a, short[] heights, int[] colors, Vec3 origin, double hole,
                                               AtlasMeshes.Mesh source, long version) {
        int n = a.size();
        int cell = a.cellSize();
        int half = a.circumference() / 2;
        int sea = a.seaLevel();
        ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(n * n * 4 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize());
        BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int quads = 0;
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                int color = colors[row * n + col];
                if (color == 0) continue;
                double cx = -half + col * cell + cell;
                double cz = -half + row * cell + cell;
                double dx = geometry.delta(origin.x, cx);
                double dz = geometry.delta(origin.z, cz);
                if (dx * dx + dz * dz < hole * hole && Math.abs(AtlasMeshes.height(heights, n, sea, row, col) - origin.y) < hole) continue;
                float x = (float) (dx - cell * 0.5);
                float z = (float) (dz - cell * 0.5);
                builder.addVertex(x, (float) (AtlasMeshes.height(heights, n, sea, row, col) - origin.y), z).setUv(0.5F, 0.5F).setColor(color);
                builder.addVertex(x, (float) (AtlasMeshes.height(heights, n, sea, row + 1, col) - origin.y), z + cell).setUv(0.5F, 0.5F).setColor(color);
                builder.addVertex(x + cell, (float) (AtlasMeshes.height(heights, n, sea, row + 1, col + 1) - origin.y), z + cell).setUv(0.5F, 0.5F).setColor(color);
                builder.addVertex(x + cell, (float) (AtlasMeshes.height(heights, n, sea, row, col + 1) - origin.y), z).setUv(0.5F, 0.5F).setColor(color);
                quads++;
            }
        }
        if (quads == 0) {
            bytes.close();
            return new PackBuilt(origin, source, version, null, null);
        }
        return new PackBuilt(origin, source, version, bytes, builder.buildOrThrow());
    }

    private static void uploadPack(PackBuilt built) {
        try {
            if (packBuffer != null) packBuffer.close();
            packBuffer = null;
            packIndexCount = 0;
            packOrigin = built.origin();
            packSource = built.source();
            packVersion = built.version();
            if (built.mesh() == null) return;
            packIndexCount = built.mesh().drawState().indexCount();
            packBuffer = RenderSystem.getDevice().createBuffer(() -> "SphereWorld planet LOD (shader pack)", GpuBuffer.USAGE_VERTEX, built.mesh().vertexBuffer());
        } finally {
            built.close();
        }
    }

    public static void close() {
        closeRequested = true;
    }

    private static void closeNow() {
        closeRequested = false;
        if (packBuffer != null) {
            packBuffer.close();
            packBuffer = null;
        }
        if (packBuilding != null) {
            packBuilding.thenAccept(built -> {
                if (built != null) built.close();
            });
            packBuilding = null;
        }
        packIndexCount = 0;
        packOrigin = null;
        packSource = null;
        packVersion = 0;
    }
}
