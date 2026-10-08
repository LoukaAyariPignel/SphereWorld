package dev.sphereworld.client.mixin.compat;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.client.render.IrisShaderPatch;
import java.util.LinkedHashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher", remap = false)
abstract class IrisTransformPatcherMixin {
    @Inject(method = {"patchVanilla", "patchSodium"}, at = @At("RETURN"), cancellable = true, require = 0)
    private static void sphereworld$curvePack(CallbackInfoReturnable<Map<Object, String>> cir,
                                              @com.llamalad7.mixinextras.sugar.Local(argsOnly = true, ordinal = 0) String name) {
        sphereworld$apply(cir, name);
    }

    @Inject(method = "patchComposite", at = @At("RETURN"), cancellable = true, require = 0)
    private static void sphereworld$fogComposite(CallbackInfoReturnable<Map<Object, String>> cir) {
        sphereworld$fog(cir);
    }

    private static void sphereworld$fog(CallbackInfoReturnable<Map<Object, String>> cir) {
        Map<Object, String> sources = cir.getReturnValue();
        if (sources == null) return;
        Map<Object, String> copy = null;
        for (Map.Entry<Object, String> entry : sources.entrySet()) {
            String patched = IrisShaderPatch.patchFog(entry.getValue());
            if (patched == null) continue;
            if (copy == null) copy = new LinkedHashMap<>(sources);
            copy.put(entry.getKey(), patched);
        }
        if (copy != null) cir.setReturnValue(copy);
    }

    private static void sphereworld$apply(CallbackInfoReturnable<Map<Object, String>> cir, String name) {
        try {
            sphereworld$curve(cir, name);
        } finally {
            sphereworld$fog(cir);
        }
    }

    private static void sphereworld$curve(CallbackInfoReturnable<Map<Object, String>> cir, String name) {
        Map<Object, String> sources = cir.getReturnValue();
        if (sources == null || name == null) return;
        Object geometryKey = null;
        Object tessKey = null;
        Object vertexKey = null;
        for (Object key : sources.keySet()) {
            String type = String.valueOf(key);
            if (type.equals("GEOMETRY") && sources.get(key) != null) geometryKey = key;
            if (type.equals("TESS_EVAL") && sources.get(key) != null) tessKey = key;
            if (type.equals("VERTEX")) vertexKey = key;
        }
        if (geometryKey != null) {
            SphereWorld.LOGGER.debug("Shader program {} uses a geometry stage; leaving it flat", name);
            return;
        }
        Object target = tessKey != null ? tessKey : vertexKey;
        if (target == null) return;
        String patched = IrisShaderPatch.patch(name, sources.get(target));
        if (patched == null) return;
        Map<Object, String> copy = new LinkedHashMap<>(sources);
        copy.put(target, patched);
        cir.setReturnValue(copy);
    }
}
