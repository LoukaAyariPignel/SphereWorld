package dev.sphereworld.mixin.world;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import dev.sphereworld.wrap.PlanetWrap;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalForcer.class)
abstract class PortalForcerMixin {
    @Shadow @Final private ServerLevel level;

    @Inject(method = "findClosestPortalPosition", at = @At("HEAD"), cancellable = true)
    private void sphereworld$periodicSearch(BlockPos target, boolean toNether, WorldBorder border,
                                            CallbackInfoReturnable<Optional<BlockPos>> cir) {
        PlanetGeometry g = Planets.of(level);
        if (g == null) return;
        PoiManager poi = level.getPoiManager();
        int radius = toNether ? 16 : 128;
        BlockPos center = PlanetWrap.canonical(g, target);
        Stream<BlockPos> found = Stream.empty();
        int c = g.circumference();
        for (int sx : new int[] {0, c, -c}) {
            for (int sz : new int[] {0, c, -c}) {
                BlockPos image = center.offset(sx, 0, sz);
                if (image.getX() + radius < g.minBlock() || image.getX() - radius >= g.maxBlock()
                        || image.getZ() + radius < g.minBlock() || image.getZ() - radius >= g.maxBlock()) continue;
                poi.ensureLoadedAndValid(level, image, radius);
                found = Stream.concat(found, poi.getInSquare(type -> type.is(PoiTypes.NETHER_PORTAL), image, radius, PoiManager.Occupancy.ANY)
                        .map(PoiRecord::getPos));
            }
        }
        Vec3 reference = Vec3.atCenterOf(center);
        dev.sphereworld.worldgen.stacked.StackBand band = dev.sphereworld.worldgen.stacked.StackedWorld.PORTAL_BAND.get();
        cir.setReturnValue(found

                .filter(pos -> band == null || band.containsWorldY(pos.getY()))
                .filter(pos -> level.getBlockState(pos).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
                .map(pos -> PlanetWrap.nearestImage(g, pos, reference))
                .min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(center)).thenComparingInt(Vec3i::getY)));
    }
}
