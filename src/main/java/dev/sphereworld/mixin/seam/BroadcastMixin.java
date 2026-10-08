package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
abstract class BroadcastMixin {
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private List<ServerPlayer> players;

    @Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
    private void sphereworld$periodicBroadcast(@Nullable Player except, double x, double y, double z, double range,
                                               ResourceKey<Level> dimension, Packet<?> packet, CallbackInfo ci) {
        PlanetGeometry g = Planets.of(server.getLevel(dimension));
        if (g == null) return;
        ci.cancel();
        for (ServerPlayer player : players) {
            if (player == except || player.level().dimension() != dimension) continue;
            double dx = g.delta(player.getX(), x);
            double dy = y - player.getY();
            double dz = g.delta(player.getZ(), z);
            if (dx * dx + dy * dy + dz * dz < range * range) player.connection.send(packet);
        }
    }
}
