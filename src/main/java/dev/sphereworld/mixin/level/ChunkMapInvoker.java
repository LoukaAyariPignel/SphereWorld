package dev.sphereworld.mixin.level;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapInvoker {
    @Invoker("getVisibleChunkIfPresent")
    ChunkHolder sphereworld$getVisibleChunkIfPresent(long key);
}
