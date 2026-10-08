package dev.sphereworld.client.mixin.compat;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "me.cortex.voxy.common.world.service.VoxelIngestService", remap = false)
abstract class VoxyIngestMixin {
    private static final String SECTION_INIT = "Lme/cortex/voxy/common/world/service/VoxelIngestService$IngestSection;<init>(IIILme/cortex/voxy/common/world/WorldEngine;Lnet/minecraft/world/level/chunk/LevelChunkSection;Lnet/minecraft/world/level/chunk/DataLayer;Lnet/minecraft/world/level/chunk/DataLayer;)V";

    @ModifyArg(method = {"enqueueIngest", "rawIngest0"}, at = @At(value = "INVOKE", target = SECTION_INIT), index = 0, require = 0)
    private int sphereworld$canonicalX(int chunkX) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        return g == null ? chunkX : g.canonicalChunk(chunkX);
    }

    @ModifyArg(method = {"enqueueIngest", "rawIngest0"}, at = @At(value = "INVOKE", target = SECTION_INIT), index = 2, require = 0)
    private int sphereworld$canonicalZ(int chunkZ) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        return g == null ? chunkZ : g.canonicalChunk(chunkZ);
    }
}
