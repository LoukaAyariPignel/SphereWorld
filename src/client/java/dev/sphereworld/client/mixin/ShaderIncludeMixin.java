package dev.sphereworld.client.mixin;

import com.mojang.renderpearl.api.pipeline.ShaderSource;
import dev.sphereworld.client.render.ShaderPatches;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ShaderSource.CachedIncludeSource.class)
abstract class ShaderIncludeMixin {
    @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true)
    private static String sphereworld$patchInclude(String source, Identifier id) {
        return ShaderPatches.patchInclude(id, source);
    }
}
