package dev.sphereworld.mixin.level;

import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerChunkCache.class)
public interface ChunkCacheInvoker {
    @Invoker("runDistanceManagerUpdates")
    boolean sphereworld$runDistanceManagerUpdates();

    @Accessor("ticketStorage")
    net.minecraft.world.level.TicketStorage sphereworld$ticketStorage();
}
