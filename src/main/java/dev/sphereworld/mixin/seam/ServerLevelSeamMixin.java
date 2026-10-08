package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLevel.class)
abstract class ServerLevelSeamMixin {
    @Redirect(method = "destroyBlockProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getX()D"))
    private double sphereworld$progressX(ServerPlayer player, int id, BlockPos pos, int progress) {
        PlanetGeometry g = Planets.of((ServerLevel) (Object) this);
        return g == null ? player.getX() : g.nearestImage(player.getX(), pos.getX());
    }

    @Redirect(method = "destroyBlockProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getZ()D"))
    private double sphereworld$progressZ(ServerPlayer player, int id, BlockPos pos, int progress) {
        PlanetGeometry g = Planets.of((ServerLevel) (Object) this);
        return g == null ? player.getZ() : g.nearestImage(player.getZ(), pos.getZ());
    }

    @Redirect(method = "sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean sphereworld$particleRange(BlockPos playerPos, Position target, double range) {
        PlanetGeometry g = Planets.of((ServerLevel) (Object) this);
        if (g == null) return playerPos.closerToCenterThan(target, range);
        Vec3 near = new Vec3(g.nearestImage(target.x(), playerPos.getX() + 0.5), target.y(),
                g.nearestImage(target.z(), playerPos.getZ() + 0.5));
        return playerPos.closerToCenterThan(near, range);
    }
}
