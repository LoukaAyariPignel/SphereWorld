package dev.sphereworld.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedAmbience;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
abstract class StackedMusicMixin {
    @ModifyExpressionValue(method = "getSituationalMusic", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;dimension()Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<Level> sphereworld$endLayer(ResourceKey<Level> dimension) {
        Minecraft client = (Minecraft) (Object) this;
        return client.player != null && StackedAmbience.bandAt(client.player.level(), client.player.position()) == StackBand.END
                ? Level.END : dimension;
    }
}
