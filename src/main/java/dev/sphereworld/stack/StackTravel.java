package dev.sphereworld.stack;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.planet.PlanetConfig;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.PlanetServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class StackTravel {
    public static final int FALL_MARGIN = 8;
    private static final int COOLDOWN_TICKS = 20;

    private StackTravel() {
    }

    public static boolean tick(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity.isPassenger() || entity.isRemoved()) return false;
        double y = entity.getY();
        boolean down = y < level.getMinY() - FALL_MARGIN;
        boolean up = y >= topY(level) + 1;
        if (!down && !up) return false;
        if (entity.isOnPortalCooldown()) return false;

        MinecraftServer server = level.getServer();
        PlanetConfig config = PlanetServer.config(server);
        if (config == null) return false;
        ResourceKey<Level> targetKey = down ? config.below(level.dimension()) : config.above(level.dimension());
        if (targetKey == null) return false;
        ServerLevel target = server.getLevel(targetKey);
        if (target == null) return false;

        double scale = horizontalScale(server, level, target);
        double x = entity.getX() * scale;
        double z = entity.getZ() * scale;
        PlanetGeometry targetPlanet = PlanetServer.geometry(server, targetKey);
        if (targetPlanet != null) {
            x = targetPlanet.canonical(x);
            z = targetPlanet.canonical(z);
        }

        Vec3 arrival = down ? arriveFromAbove(target, x, z) : arriveFromBelow(target, x, z);
        Vec3 motion = down ? entity.getDeltaMovement() : Vec3.ZERO;
        double drop = down ? arrival.y - groundBelow(target, arrival) : 0;
        TeleportTransition transition = new TeleportTransition(target, arrival, motion, entity.getYRot(), entity.getXRot(),
                moved -> {
                    moved.resetFallDistance();
                    moved.setPortalCooldown(COOLDOWN_TICKS);
                    if (drop > 4 && moved instanceof LivingEntity living) {
                        int ticks = (int) Math.min(20 * 120, drop * 2.2 + 100);
                        living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0, false, false, true));
                    }
                });
        SphereWorld.LOGGER.info("Stack travel {} {} -> {} at {}", entity.getName().getString(),
                level.dimension().identifier(), targetKey.identifier(), arrival);
        return entity.teleport(transition) != null;
    }

    public static int topY(Level level) {
        return Math.min(level.getMaxY(), level.getMinY() + level.dimensionType().logicalHeight() - 1);
    }

    public static double horizontalScale(MinecraftServer server, ServerLevel from, ServerLevel to) {
        PlanetGeometry a = PlanetServer.geometry(server, from.dimension());
        PlanetGeometry b = PlanetServer.geometry(server, to.dimension());
        if (a != null && b != null) {
            return (double) b.circumference() / a.circumference();
        }
        return DimensionType.getTeleportationScale(from.dimensionType(), to.dimensionType());
    }

    private static Vec3 arriveFromAbove(ServerLevel target, double x, double z) {
        BlockPos column = BlockPos.containing(x, topY(target), z);
        target.getChunk(column.getX() >> 4, column.getZ() >> 4);
        int top = topY(target) - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(column.getX(), top, column.getZ());
        while (pos.getY() > target.getMinY() && !(isFree(target, pos) && isFree(target, pos.above()))) {
            pos.move(0, -1, 0);
        }
        return new Vec3(x, pos.getY(), z);
    }

    private static double groundBelow(ServerLevel target, Vec3 arrival) {
        BlockPos column = BlockPos.containing(arrival);
        int surface = target.getChunk(column.getX() >> 4, column.getZ() >> 4)
                .getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX() & 15, column.getZ() & 15);
        return Math.min(arrival.y, surface + 1);
    }

    private static Vec3 arriveFromBelow(ServerLevel target, double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        target.getChunk(bx >> 4, bz >> 4);
        int minY = target.getMinY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(bx, minY + 1, bz);
        for (int y = minY + 1; y < Math.min(minY + 48, target.getMaxY() - 2); y++) {
            pos.setY(y);
            if (isFree(target, pos) && isFree(target, pos.above()) && isSolid(target, pos.below())) {
                return new Vec3(bx + 0.5, y, bz + 0.5);
            }
        }

        boolean solidColumn = false;
        for (int y = minY; y < Math.min(minY + 48, target.getMaxY()); y++) {
            pos.setY(y);
            if (isSolid(target, pos)) {
                solidColumn = true;
                break;
            }
        }
        int feet = minY + 2;
        if (solidColumn) {
            for (int dy = 0; dy < 2; dy++) {
                target.setBlockAndUpdate(new BlockPos(bx, feet + dy, bz), Blocks.AIR.defaultBlockState());
            }
            BlockPos floor = new BlockPos(bx, feet - 1, bz);
            if (!isSolid(target, floor)) target.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        } else {
            BlockState platform = Blocks.OBSIDIAN.defaultBlockState();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    target.setBlockAndUpdate(new BlockPos(bx + dx, feet - 1, bz + dz), platform);
                }
            }
        }
        return new Vec3(bx + 0.5, feet, bz + 0.5);
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    private static boolean isSolid(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    public static @Nullable Boundaries boundaries(MinecraftServer server, ResourceKey<Level> dimension) {
        PlanetConfig config = PlanetServer.config(server);
        if (config == null || !config.openBoundaries() || config.layer(dimension) < 0) return null;
        boolean floor = config.below(dimension) != null;
        boolean roof = config.above(dimension) != null;
        return floor || roof ? new Boundaries(floor, roof) : null;
    }

    public record Boundaries(boolean floor, boolean roof) {
    }
}
