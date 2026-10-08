package dev.sphereworld.client.mixin.compat;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.LongConsumer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.cortex.voxy.client.core.rendering.RenderDistanceTracker", remap = false)
abstract class VoxyDistanceTrackerMixin {
    @Unique private static final int MAX_WHOLE_PLANET = 16384;
    @Unique private static MethodHandle sphereworld$sectionId;

    @Shadow @Final private LongConsumer addTopLevelNode;
    @Shadow @Final private int minSec;
    @Shadow @Final private int maxSec;

    @Unique private boolean sphereworld$wholePlanetAdded;

    @Unique
    private static PlanetGeometry sphereworld$smallPlanet() {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        return g != null && g.circumference() <= MAX_WHOLE_PLANET ? g : null;
    }

    @Inject(method = "setCenterAndProcess", at = @At("HEAD"), cancellable = true, require = 0)
    private void sphereworld$wholePlanet(double x, double z, CallbackInfoReturnable<Boolean> cir) {
        PlanetGeometry g = sphereworld$smallPlanet();
        if (g == null) return;
        if (sphereworld$wholePlanetAdded) {
            cir.setReturnValue(false);
            return;
        }
        sphereworld$wholePlanetAdded = true;
        try {
            if (sphereworld$sectionId == null) {
                Class<?> engine = Class.forName("me.cortex.voxy.common.world.WorldEngine");
                sphereworld$sectionId = MethodHandles.publicLookup().findStatic(engine, "getWorldSectionId",
                        MethodType.methodType(long.class, int.class, int.class, int.class, int.class));
            }
            int first = Math.floorDiv(g.minBlock(), 512);
            int last = Math.floorDiv(g.minBlock() + g.circumference() - 1, 512);
            for (int nx = first; nx <= last; nx++) {
                for (int nz = first; nz <= last; nz++) {
                    for (int y = minSec; y <= maxSec; y++) {
                        addTopLevelNode.accept((long) sphereworld$sectionId.invokeExact(4, nx, y, nz));
                    }
                }
            }
            SphereWorld.LOGGER.info("Voxy: loaded the whole planet ({}x{} top-level columns)", last - first + 1, last - first + 1);
        } catch (Throwable e) {
            SphereWorld.LOGGER.warn("Voxy: could not load the whole planet, keeping its ring", e);
            return;
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "setRenderDistance", at = @At("HEAD"), cancellable = true, require = 0)
    private void sphereworld$keepWholePlanet(int renderDistance, CallbackInfo ci) {
        if (sphereworld$smallPlanet() != null) ci.cancel();
    }
}
