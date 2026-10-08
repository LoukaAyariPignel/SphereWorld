package dev.sphereworld.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.sphereworld.SphereWorld;
import dev.sphereworld.atlas.PlanetAtlas;
import dev.sphereworld.client.ClientPlanets;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;

public final class RemotePlanetRenderer {
    public static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(SphereWorld.id("pipeline/planet_remote"))
                    .withVertexShader(SphereWorld.id("core/planet_remote"))
                    .withFragmentShader(SphereWorld.id("core/planet_remote"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withColorTargetState(ColorTargetState.DEFAULT)
                    .withCull(false)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .build());

    private static final RenderSystem.AutoStorageIndexBuffer QUAD_INDICES = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);

    private RemotePlanetRenderer() {
    }

    public static void render(RenderPass pass) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || level.dimensionType().hasCeiling() || IrisLodSupport.shaderPackInUse()) return;
        PlanetGeometry current = Planets.of(level);
        if (current == null) return;
        List<Identifier> stack = ClientPlanets.stack();
        int layer = stack.indexOf(level.dimension().identifier());
        if (layer <= 0) return;
        for (int i = layer - 1; i >= 0; i--) {
            Identifier inner = stack.get(i);
            PlanetGeometry geometry = ClientPlanets.get(inner);
            AtlasMeshes.Mesh mesh = AtlasMeshes.get(inner);
            if (geometry == null || mesh == null) continue;
            PlanetAtlas atlas = mesh.atlas();
            Vector4f params = new Vector4f((float) current.circumference() / geometry.circumference(),
                    (float) geometry.radius(), atlas.seaLevel(), 1.0F);
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                    .writeTransform(RenderSystem.getModelViewMatrixCopy(), params);
            pass.pushDebugGroup(() -> "SphereWorld inner planet");
            pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, mesh.buffer().slice());
            pass.setIndexBuffer(QUAD_INDICES.getBuffer(mesh.indexCount()), QUAD_INDICES.type());
            pass.drawIndexed(mesh.indexCount(), 1, 0, 0, 0);
            pass.popDebugGroup();
        }
    }

    public static void clear() {
        AtlasMeshes.clear();
    }
}
