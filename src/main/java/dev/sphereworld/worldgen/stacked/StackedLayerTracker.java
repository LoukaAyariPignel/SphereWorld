package dev.sphereworld.worldgen.stacked;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class StackedLayerTracker {
    private record State(StackBand band, @Nullable Vec3 enteredNether) {
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private StackedLayerTracker() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(StackedLayerTracker::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.getPlayer().getUUID()));
    }

    public static ResourceKey<Level> dimensionOf(StackBand band) {
        return band == StackBand.NETHER ? Level.NETHER : band == StackBand.END ? Level.END : Level.OVERWORLD;
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!StackedWorld.is(player.level()) || player.isSpectator()) continue;
            StackBand band = StackBand.at(Mth.floor(player.getY()));
            State state = STATES.get(player.getUUID());
            if (state == null) {
                STATES.put(player.getUUID(), new State(band, band == StackBand.NETHER ? player.position() : null));
                continue;
            }
            if (state.band() == band) continue;
            ResourceKey<Level> from = dimensionOf(state.band());
            ResourceKey<Level> to = dimensionOf(band);
            CriteriaTriggers.CHANGED_DIMENSION.trigger(player, from, to);
            Vec3 entered = null;
            if (band == StackBand.NETHER) {
                entered = player.position();
            } else if (state.band() == StackBand.NETHER && band == StackBand.OVERWORLD && state.enteredNether() != null) {
                CriteriaTriggers.NETHER_TRAVEL.trigger(player, scaledStart(player, state.enteredNether()));
            }
            STATES.put(player.getUUID(), new State(band, entered));
        }
    }

    private static Vec3 scaledStart(ServerPlayer player, Vec3 enteredNether) {
        PlanetGeometry geometry = Planets.of(player.level());
        double dx = geometry == null ? player.getX() - enteredNether.x : geometry.delta(enteredNether.x, player.getX());
        double dz = geometry == null ? player.getZ() - enteredNether.z : geometry.delta(enteredNether.z, player.getZ());
        return new Vec3(player.getX() - dx * 8.0, enteredNether.y, player.getZ() - dz * 8.0);
    }
}
