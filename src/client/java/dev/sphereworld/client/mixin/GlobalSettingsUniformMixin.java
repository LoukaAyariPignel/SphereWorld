package dev.sphereworld.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.Std140Builder;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GlobalSettingsUniform.class)
abstract class GlobalSettingsUniformMixin {
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/buffers/Std140SizeCalculator;get()I"))
    private static int sphereworld$growBlock(int size) {
        return size + 16;
    }

    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putInt(I)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 1))
    private Std140Builder sphereworld$appendPlanet(Std140Builder builder) {
        Minecraft client = Minecraft.getInstance();
        PlanetGeometry g = Planets.of(client.level);
        if (g == null) return builder.putVec4(1.0F, 0.0F, 0.0F, 0.0F);

        float loaded = Math.max(16.0F, client.options.getEffectiveRenderDistance() * 16.0F);
        return builder.putVec4((float) g.radius(), g.surfaceY(), loaded, g.circumference());
    }
}
