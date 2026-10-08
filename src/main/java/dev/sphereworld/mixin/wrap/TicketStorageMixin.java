package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetTickets;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.server.level.Ticket;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TicketStorage.class)
abstract class TicketStorageMixin implements PlanetTickets {
    @Shadow @Final private Long2ObjectOpenHashMap<List<Ticket>> tickets;
    @Shadow @Final private Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets;

    @Shadow
    private void updateForcedChunks() {
        throw new AssertionError();
    }

    @Unique private @Nullable PlanetGeometry sphereworld$geometry;

    @Override
    public void sphereworld$setGeometry(@Nullable PlanetGeometry geometry) {
        sphereworld$geometry = geometry;
        if (geometry == null) return;
        sphereworld$rekey(tickets, geometry);
        sphereworld$rekey(deactivatedTickets, geometry);
        updateForcedChunks();
    }

    @Unique
    private static void sphereworld$rekey(Long2ObjectOpenHashMap<List<Ticket>> map, PlanetGeometry g) {
        Long2ObjectOpenHashMap<List<Ticket>> moved = new Long2ObjectOpenHashMap<>();
        var iterator = map.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            long canonical = sphereworld$canonical(g, entry.getLongKey());
            if (canonical != entry.getLongKey()) {
                moved.computeIfAbsent(canonical, k -> new ArrayList<>()).addAll(entry.getValue());
                iterator.remove();
            }
        }
        moved.forEach((key, list) -> map.computeIfAbsent((long) key, k -> new ArrayList<>()).addAll(list));
    }

    @Unique
    private static long sphereworld$canonical(PlanetGeometry g, long key) {
        ChunkPos pos = ChunkPos.unpack(key);
        int x = g.canonicalChunk(pos.x());
        int z = g.canonicalChunk(pos.z());
        return x == pos.x() && z == pos.z() ? key : ChunkPos.pack(x, z);
    }

    @ModifyVariable(method = {
            "addTicket(JLnet/minecraft/server/level/Ticket;)Z",
            "removeTicket(JLnet/minecraft/server/level/Ticket;)Z",
            "getTickets(J)Ljava/util/List;",
            "getTicketLevelAt(JZ)I"},
            at = @At("HEAD"), argsOnly = true)
    private long sphereworld$canonicalKey(long key) {
        PlanetGeometry g = sphereworld$geometry;
        return g == null ? key : sphereworld$canonical(g, key);
    }
}
