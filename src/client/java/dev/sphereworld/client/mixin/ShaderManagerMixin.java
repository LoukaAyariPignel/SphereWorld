package dev.sphereworld.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import dev.sphereworld.client.render.ShaderPatches;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderManager.class)
abstract class ShaderManagerMixin {
    @ModifyExpressionValue(method = "loadShader", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"))
    private static String sphereworld$patchSource(String contents, @Local(argsOnly = true) Identifier location,
                                                  @Local(argsOnly = true) ShaderType type) {
        if (type != ShaderType.VERTEX) return contents;
        return ShaderPatches.patchShader(type.idConverter().fileToId(location), contents);
    }
}
