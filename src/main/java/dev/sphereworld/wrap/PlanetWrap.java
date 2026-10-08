package dev.sphereworld.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class PlanetWrap {
    public static final ThreadLocal<@Nullable PlanetGeometry> CONSTRUCTING = new ThreadLocal<>();

    private PlanetWrap() {
    }

    public static @Nullable PlanetGeometry server(@Nullable Level level) {
        return level == null || level.isClientSide() ? null : Planets.of(level);
    }

    public static BlockPos canonical(PlanetGeometry g, BlockPos pos) {
        int x = g.canonical(pos.getX());
        int z = g.canonical(pos.getZ());
        return x == pos.getX() && z == pos.getZ() ? pos : new BlockPos(x, pos.getY(), z);
    }

    public static BlockPos nearestImage(PlanetGeometry g, BlockPos pos, Vec3 reference) {
        int x = g.nearestImage(pos.getX(), (int) Math.floor(reference.x));
        int z = g.nearestImage(pos.getZ(), (int) Math.floor(reference.z));
        return x == pos.getX() && z == pos.getZ() ? pos : new BlockPos(x, pos.getY(), z);
    }

    public static Vec3 nearestImage(PlanetGeometry g, Vec3 pos, Vec3 reference) {
        double x = g.nearestImage(pos.x, reference.x);
        double z = g.nearestImage(pos.z, reference.z);
        return x == pos.x && z == pos.z ? pos : new Vec3(x, pos.y, z);
    }

    public static BlockHitResult nearestImage(PlanetGeometry g, BlockHitResult hit, Vec3 reference) {
        BlockPos pos = nearestImage(g, hit.getBlockPos(), reference);
        if (pos == hit.getBlockPos()) return hit;
        Vec3 offset = new Vec3(pos.getX() - hit.getBlockPos().getX(), 0, pos.getZ() - hit.getBlockPos().getZ());
        return new BlockHitResult(hit.getLocation().add(offset), hit.getDirection(), pos, hit.isInside(), hit.isWorldBorderHit());
    }
}
