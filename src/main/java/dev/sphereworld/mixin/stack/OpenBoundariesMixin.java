package dev.sphereworld.mixin.stack;

import dev.sphereworld.stack.StackTravel;
import dev.sphereworld.worldgen.PlanetRandomState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseBasedChunkGenerator.class)
abstract class OpenBoundariesMixin {
    @Unique private static final int FLOOR_DEPTH = 8;

    @Inject(method = "buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Ljava/util/Set;Lnet/minecraft/world/level/levelgen/material/rule/MaterialRule;)V",
            at = @At("TAIL"))
    private void sphereworld$openBoundaries(ChunkAccess chunk, NoiseChunk noiseChunk, RandomState randomState,
                                            net.minecraft.world.level.biome.BiomeManager biomeManager,
                                            java.util.Set<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> possibleBiomes,
                                            net.minecraft.world.level.levelgen.material.rule.MaterialRule materialRule,
                                            CallbackInfo ci) {
        StackTravel.Boundaries boundaries = ((PlanetRandomState) (Object) randomState).sphereworld$boundaries();
        if (boundaries == null) return;
        int minY = chunk.getMinY();
        for (int index = 0; index < chunk.getSectionsCount(); index++) {
            LevelChunkSection section = chunk.getSection(index);
            if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(Blocks.BEDROCK))) continue;
            int bottom = chunk.getSectionYFromSectionIndex(index) << 4;
            for (int ly = 0; ly < 16; ly++) {
                int y = bottom + ly;
                boolean floor = y < minY + FLOOR_DEPTH;
                if (floor ? !boundaries.floor() : !boundaries.roof()) continue;
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        if (!section.getBlockState(lx, ly, lz).is(Blocks.BEDROCK)) continue;
                        section.setBlockState(lx, ly, lz, sphereworld$neighbourRock(chunk, lx, y, lz, floor), false);
                    }
                }
            }
        }
    }

    @Unique
    private static BlockState sphereworld$neighbourRock(ChunkAccess chunk, int lx, int y, int lz, boolean floor) {
        int step = floor ? 1 : -1;
        var pos = new net.minecraft.core.BlockPos.MutableBlockPos(chunk.getPos().getBlockX(lx), y, chunk.getPos().getBlockZ(lz));
        for (int i = 1; i <= 12; i++) {
            pos.setY(y + i * step);
            if (pos.getY() < chunk.getMinY() || pos.getY() > chunk.getMaxY()) break;
            BlockState state = chunk.getBlockState(pos);
            if (!state.is(Blocks.BEDROCK) && !state.isAir() && state.getFluidState().isEmpty()) return state;
        }
        return Blocks.STONE.defaultBlockState();
    }
}
