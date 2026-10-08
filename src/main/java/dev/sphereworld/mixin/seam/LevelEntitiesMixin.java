package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
abstract class LevelEntitiesMixin {
    @Unique private static final ThreadLocal<Boolean> SPHEREWORLD_NESTED = ThreadLocal.withInitial(() -> false);

    @Shadow
    public abstract List<Entity> getEntities(@Nullable Entity except, AABB bb, Predicate<? super Entity> selector);

    @Shadow
    public abstract <T extends Entity> void getEntities(EntityTypeTest<Entity, T> type, AABB bb, Predicate<? super T> selector,
                                                        List<? super T> output, int maxResults);

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"))
    private void sphereworld$acrossSeam(@Nullable Entity except, AABB bb, Predicate<? super Entity> selector,
                                        CallbackInfoReturnable<List<Entity>> cir) {
        PlanetGeometry g = PlanetWrap.server((Level) (Object) this);
        if (g == null || SPHEREWORLD_NESTED.get() || !sphereworld$crosses(g, bb)) return;
        List<Entity> output = cir.getReturnValue();
        SPHEREWORLD_NESTED.set(true);
        try {
            for (AABB image : sphereworld$images(g, bb)) {
                for (Entity entity : getEntities(except, image, selector)) {
                    if (!output.contains(entity)) output.add(entity);
                }
            }
        } finally {
            SPHEREWORLD_NESTED.set(false);
        }
    }

    @Inject(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V",
            at = @At("RETURN"))
    private <T extends Entity> void sphereworld$typedAcrossSeam(EntityTypeTest<Entity, T> type, AABB bb, Predicate<? super T> selector,
                                                               List<? super T> output, int maxResults, CallbackInfo ci) {
        PlanetGeometry g = PlanetWrap.server((Level) (Object) this);
        if (g == null || SPHEREWORLD_NESTED.get() || !sphereworld$crosses(g, bb) || output.size() >= maxResults) return;
        SPHEREWORLD_NESTED.set(true);
        try {
            for (AABB image : sphereworld$images(g, bb)) {
                java.util.List<T> found = new java.util.ArrayList<>();
                getEntities(type, image, selector, found, maxResults - output.size());
                for (T entity : found) {
                    if (!output.contains(entity) && output.size() < maxResults) output.add(entity);
                }
            }
        } finally {
            SPHEREWORLD_NESTED.set(false);
        }
    }

    @Unique
    private static boolean sphereworld$crosses(PlanetGeometry g, AABB bb) {
        return bb.minX < g.minBlock() || bb.maxX > g.maxBlock() || bb.minZ < g.minBlock() || bb.maxZ > g.maxBlock();
    }

    @Unique
    private static List<AABB> sphereworld$images(PlanetGeometry g, AABB bb) {
        double c = g.circumference();
        List<AABB> images = new java.util.ArrayList<>(3);
        double[] shiftsX = {0, bb.minX < g.minBlock() ? c : 0, bb.maxX > g.maxBlock() ? -c : 0};
        double[] shiftsZ = {0, bb.minZ < g.minBlock() ? c : 0, bb.maxZ > g.maxBlock() ? -c : 0};
        for (double sx : shiftsX) {
            for (double sz : shiftsZ) {
                if (sx == 0 && sz == 0) continue;
                AABB moved = bb.move(sx, 0, sz);
                if (moved.maxX > g.minBlock() && moved.minX < g.maxBlock() && moved.maxZ > g.minBlock() && moved.minZ < g.maxBlock()
                        && !images.contains(moved)) {
                    images.add(moved);
                }
            }
        }
        return images;
    }
}
