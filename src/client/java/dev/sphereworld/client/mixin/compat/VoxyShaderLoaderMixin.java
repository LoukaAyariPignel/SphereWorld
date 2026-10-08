package dev.sphereworld.client.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.client.compat.voxy.VoxyShaderPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "me.cortex.voxy.client.core.gl.shader.ShaderLoader", remap = false)
abstract class VoxyShaderLoaderMixin {
    @ModifyReturnValue(method = "parse", at = @At("RETURN"), require = 0)
    private static String sphereworld$planet(String source, @Local(argsOnly = true) String id) {
        return VoxyShaderPatch.patch(id, source);
    }
}
