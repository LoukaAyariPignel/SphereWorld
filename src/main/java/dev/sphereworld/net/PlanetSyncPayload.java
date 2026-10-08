package dev.sphereworld.net;

import dev.sphereworld.SphereWorld;
import dev.sphereworld.planet.PlanetConfig;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.PlanetServer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public record PlanetSyncPayload(List<Entry> planets, List<Identifier> stack) implements CustomPacketPayload {
    public static final Type<PlanetSyncPayload> TYPE = new Type<>(SphereWorld.id("planets"));

    public record Entry(Identifier dimension, int circumference, int surfaceY) {
        public static final StreamCodec<FriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Entry::dimension,
                ByteBufCodecs.VAR_INT, Entry::circumference,
                ByteBufCodecs.VAR_INT, Entry::surfaceY,
                Entry::new);
    }

    public static final StreamCodec<FriendlyByteBuf, PlanetSyncPayload> CODEC = StreamCodec.composite(
            Entry.CODEC.apply(ByteBufCodecs.list()), PlanetSyncPayload::planets,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), PlanetSyncPayload::stack,
            PlanetSyncPayload::new);

    public static PlanetSyncPayload of(MinecraftServer server) {
        PlanetConfig config = PlanetServer.config(server);
        List<Entry> entries = new ArrayList<>();
        List<Identifier> stack = new ArrayList<>();
        if (config != null) {
            for (ResourceKey<Level> dimension : config.planets().keySet()) {
                PlanetGeometry geometry = PlanetServer.geometry(server, dimension);
                if (geometry != null) {
                    entries.add(new Entry(dimension.identifier(), geometry.circumference(), geometry.surfaceY()));
                }
            }
            config.stack().forEach(key -> stack.add(key.identifier()));
        }
        return new PlanetSyncPayload(entries, stack);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
