package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetPoi;
import dev.sphereworld.wrap.PlanetWrap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PoiManager.class)
abstract class PoiManagerMixin implements PlanetPoi {
    @Unique private static final ThreadLocal<Boolean> SPHEREWORLD_NESTED = ThreadLocal.withInitial(() -> false);
    @Unique private @Nullable PlanetGeometry sphereworld$geometry;

    @Override
    public void sphereworld$setGeometry(@Nullable PlanetGeometry geometry) {
        sphereworld$geometry = geometry;
    }

    @Override
    public @Nullable PlanetGeometry sphereworld$geometry() {
        return sphereworld$geometry;
    }

    @Shadow
    public abstract Stream<PoiRecord> getInSquare(Predicate<Holder<PoiType>> predicate, BlockPos center, int radius, PoiManager.Occupancy occupancy);

    @Shadow
    public abstract Stream<PoiRecord> getInRange(Predicate<Holder<PoiType>> predicate, BlockPos center, int radius, PoiManager.Occupancy occupancy);

    @ModifyVariable(method = {"add", "remove", "release", "exists", "getType", "getDebugPoiInfo"}, at = @At("HEAD"), argsOnly = true)
    private BlockPos sphereworld$canonicalPos(BlockPos pos) {
        PlanetGeometry g = sphereworld$geometry;
        return g == null ? pos : PlanetWrap.canonical(g, pos);
    }

    @ModifyVariable(method = "existsAtPosition", at = @At("HEAD"), argsOnly = true)
    private BlockPos sphereworld$canonicalLookup(BlockPos pos) {
        PlanetGeometry g = sphereworld$geometry;
        return g == null ? pos : PlanetWrap.canonical(g, pos);
    }

    @Inject(method = "getInSquare", at = @At("RETURN"), cancellable = true)
    private void sphereworld$squareAcrossSeam(Predicate<Holder<PoiType>> predicate, BlockPos center, int radius,
                                              PoiManager.Occupancy occupancy, CallbackInfoReturnable<Stream<PoiRecord>> cir) {
        sphereworld$images(cir, center, radius, image -> getInSquare(predicate, image, radius, occupancy));
    }

    @Inject(method = "getInRange", at = @At("RETURN"), cancellable = true)
    private void sphereworld$rangeAcrossSeam(Predicate<Holder<PoiType>> predicate, BlockPos center, int radius,
                                             PoiManager.Occupancy occupancy, CallbackInfoReturnable<Stream<PoiRecord>> cir) {
        sphereworld$images(cir, center, radius, image -> getInRange(predicate, image, radius, occupancy));
    }

    @Unique
    private void sphereworld$images(CallbackInfoReturnable<Stream<PoiRecord>> cir, BlockPos center, int radius,
                                    java.util.function.Function<BlockPos, Stream<PoiRecord>> search) {
        PlanetGeometry g = sphereworld$geometry;
        if (g == null || SPHEREWORLD_NESTED.get()) return;
        boolean crossesX = center.getX() - radius < g.minBlock() || center.getX() + radius >= g.maxBlock();
        boolean crossesZ = center.getZ() - radius < g.minBlock() || center.getZ() + radius >= g.maxBlock();
        if (!crossesX && !crossesZ) return;
        int c = g.circumference();
        java.util.List<Stream<PoiRecord>> streams = new java.util.ArrayList<>();
        streams.add(cir.getReturnValue());
        SPHEREWORLD_NESTED.set(true);
        try {
            for (int sx : new int[] {0, c, -c}) {
                for (int sz : new int[] {0, c, -c}) {
                    if (sx == 0 && sz == 0) continue;
                    BlockPos image = center.offset(sx, 0, sz);
                    if (image.getX() + radius < g.minBlock() || image.getX() - radius >= g.maxBlock()
                            || image.getZ() + radius < g.minBlock() || image.getZ() - radius >= g.maxBlock()) continue;

                    streams.add(search.apply(image).toList().stream());
                }
            }
        } finally {
            SPHEREWORLD_NESTED.set(false);
        }
        cir.setReturnValue(streams.stream().flatMap(s -> s).distinct());
    }
}
