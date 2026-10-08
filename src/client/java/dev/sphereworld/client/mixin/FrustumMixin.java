package dev.sphereworld.client.mixin;

import dev.sphereworld.client.render.PlanetCurve;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
abstract class FrustumMixin {
    @Shadow @Final private FrustumIntersection intersection;
    @Shadow private double camX;
    @Shadow private double camY;
    @Shadow private double camZ;

    @org.spongepowered.asm.mixin.Unique
    private static final Vector3f SPHEREWORLD$CORNER = new Vector3f();

    @Inject(method = "cubeInFrustum(DDDDDD)I", at = @At("HEAD"), cancellable = true)
    private void sphereworld$curvedCube(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                                        CallbackInfoReturnable<Integer> cir) {
        Minecraft client = Minecraft.getInstance();
        PlanetGeometry g = Planets.of(client.level);
        if (g == null || !client.isSameThread()) return;
        float cameraRadius = PlanetCurve.cameraRadius(g, camY);
        float x0 = Float.POSITIVE_INFINITY, y0 = Float.POSITIVE_INFINITY, z0 = Float.POSITIVE_INFINITY;
        float x1 = Float.NEGATIVE_INFINITY, y1 = Float.NEGATIVE_INFINITY, z1 = Float.NEGATIVE_INFINITY;
        Vector3f corner = SPHEREWORLD$CORNER;
        for (int i = 0; i < 8; i++) {
            corner.set((float) (((i & 1) == 0 ? minX : maxX) - camX),
                    (float) (((i & 2) == 0 ? minY : maxY) - camY),
                    (float) (((i & 4) == 0 ? minZ : maxZ) - camZ));
            PlanetCurve.curve(g, cameraRadius, corner);
            x0 = Math.min(x0, corner.x);
            y0 = Math.min(y0, corner.y);
            z0 = Math.min(z0, corner.z);
            x1 = Math.max(x1, corner.x);
            y1 = Math.max(y1, corner.y);
            z1 = Math.max(z1, corner.z);
        }

        float margin = Math.max(x1 - x0, z1 - z0) * 0.05F + 1.0F;
        cir.setReturnValue(intersection.intersectAab(x0 - margin, y0 - margin, z0 - margin, x1 + margin, y1 + margin, z1 + margin));
    }
}
