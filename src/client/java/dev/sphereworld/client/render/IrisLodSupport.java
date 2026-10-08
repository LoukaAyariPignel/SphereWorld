package dev.sphereworld.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.sphereworld.SphereWorld;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.RenderPipelines;
import dev.sphereworld.planet.PlanetGeometry;
import org.jspecify.annotations.Nullable;

public final class IrisLodSupport {
    public static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(SphereWorld.id("pipeline/planet_lod_pack"))
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER0)
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withColorTargetState(ColorTargetState.DEFAULT)
                    .withCull(false)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .build());

    private static @Nullable Object irisApi;
    private static @Nullable Method shaderPackInUse;
    private static boolean initialised;

    private IrisLodSupport() {
    }

    public static void init() {
        if (initialised) return;
        initialised = true;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisApi = api.getMethod("getInstance").invoke(null);
            shaderPackInUse = api.getMethod("isShaderPackInUse");
            Class<?> pipelines = Class.forName("net.irisshaders.iris.pipeline.IrisPipelines");
            Class<?> shaderKey = Class.forName("net.irisshaders.iris.pipeline.programs.ShaderKey");
            Object basicColor = shaderKey.getField("BASIC_COLOR").get(null);
            Object texturedColor = shaderKey.getField("TEXTURED_COLOR").get(null);
            pipelines.getMethod("assignPipeline", RenderPipeline.class, shaderKey).invoke(null, PIPELINE, texturedColor);

            pipelines.getMethod("assignPipeline", RenderPipeline.class, shaderKey).invoke(null, PlanetLodRenderer.PIPELINE, basicColor);
            SphereWorld.LOGGER.info("Iris detected: distant planet uses the shader pack's textured_color program");
        } catch (ClassNotFoundException e) {
            irisApi = null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            SphereWorld.LOGGER.warn("Could not hook the distant planet into Iris", e);
            irisApi = null;
        }
    }

    private static @Nullable Method destroyPipeline;
    private static @Nullable Object pipelineManager;

    public static void tick(@Nullable PlanetGeometry current) {
        if (current != null && irisApi != null) white();
        if (!shaderPackInUse() || java.util.Objects.equals(current, IrisShaderPatch.compiledFor())) return;
        try {
            if (destroyPipeline == null) {
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
                pipelineManager = iris.getMethod("getPipelineManager").invoke(null);
                destroyPipeline = pipelineManager.getClass().getMethod("destroyPipeline");
            }
            IrisShaderPatch.markCompiled(current);
            destroyPipeline.invoke(pipelineManager);
            SphereWorld.LOGGER.info("Rebuilding shader-pack programs for the current planet");
        } catch (ReflectiveOperationException | RuntimeException e) {
            SphereWorld.LOGGER.warn("Could not rebuild the Iris pipeline", e);
            IrisShaderPatch.markCompiled(current);
        }
    }

    private static @Nullable Method renderingShadowPass;

    public static boolean renderingShadowPass() {
        if (irisApi == null) return false;
        try {
            if (renderingShadowPass == null) renderingShadowPass = irisApi.getClass().getMethod("isRenderingShadowPass");
            return (boolean) renderingShadowPass.invoke(irisApi);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static net.minecraft.client.renderer.texture.@Nullable DynamicTexture white;

    public static net.minecraft.client.renderer.texture.@Nullable DynamicTexture whiteIfReady() {
        return white;
    }

    public static net.minecraft.client.renderer.texture.DynamicTexture white() {
        if (white == null) {
            com.mojang.blaze3d.platform.NativeImage image = new com.mojang.blaze3d.platform.NativeImage(2048, 1, false);
            image.fillRect(0, 0, 2048, 1, 0xFFFFFFFF);
            white = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "sphereworld planet white", image);
        }
        return white;
    }

    public static boolean shaderPackInUse() {
        if (irisApi == null || shaderPackInUse == null) return false;
        try {
            return (boolean) shaderPackInUse.invoke(irisApi);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
