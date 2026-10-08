package dev.sphereworld.client.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.Std140Builder;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager$GlobalUniforms", remap = false)
abstract class SodiumGlobalUniformsMixin {
    @ModifyExpressionValue(method = "write", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putInt(I)Lcom/mojang/blaze3d/buffers/Std140Builder;"))
    private Std140Builder sphereworld$appendPlanet(Std140Builder builder) {
        Minecraft client = Minecraft.getInstance();
        PlanetGeometry g = Planets.of(client.level);
        if (g == null) {
            return builder.putVec4(1.0F, 0.0F, 0.0F, 0.0F).putVec4(0.0F, 0.0F, 0.0F, 0.0F);
        }
        float loaded = Math.max(16.0F, client.options.getEffectiveRenderDistance() * 16.0F);
        float cameraY = (float) client.gameRenderer.mainCamera().position().y;
        return builder.putVec4((float) g.radius(), g.surfaceY(), loaded, g.circumference())
                .putVec4(cameraY, 0.0F, 0.0F, 0.0F);
    }
}
