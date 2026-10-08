package dev.sphereworld.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedAmbience;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public final class StackedSkyMixin {
    private StackedSkyMixin() {
    }

    @Mixin(SkyRenderer.class)
    public abstract static class Sky {
        @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/level/dimension/DimensionType;skybox()Lnet/minecraft/world/level/dimension/DimensionType$Skybox;"))
        private DimensionType.Skybox sphereworld$endLayerSky(DimensionType.Skybox skybox, @Local(argsOnly = true) ClientLevel level,
                @Local(argsOnly = true) Camera camera) {
            return StackedAmbience.bandAt(level, camera.position()) == StackBand.END ? DimensionType.Skybox.END : skybox;
        }
    }

    @Mixin(AtmosphericFogEnvironment.class)
    public abstract static class Fog {
        @WrapOperation(method = "getBaseColor", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;"))
        private Object sphereworld$netherFog(EnvironmentAttributeProbe probe, EnvironmentAttribute<?> attribute, float partialTicks, Operation<Object> original,
                @Local(argsOnly = true) ClientLevel level, @Local(argsOnly = true) Camera camera) {
            if ((attribute == EnvironmentAttributes.SKY_COLOR || attribute == EnvironmentAttributes.SUNRISE_SUNSET_COLOR)
                    && StackedAmbience.bandAt(level, camera.position()) == StackBand.NETHER) {
                return attribute.defaultValue();
            }
            return original.call(probe, attribute, partialTicks);
        }
    }
}
