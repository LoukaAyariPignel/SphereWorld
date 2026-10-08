package dev.sphereworld.worldgen.stacked;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

final class NetherCaves {
    static final int TOP = -41;
    static final int BOTTOM = -100;
    private static final int FADE = -88;
    private static final int CELL_Y = 8;
    private static final int FIRST_CORNER_Y = -104;
    private static final int CORNERS_Y = (TOP + 1 - FIRST_CORNER_Y) / CELL_Y + 1;
    private static final float SOLID = 0.1171875F;

    private static final String SHAPE = """
            {"type": "minecraft:max",
             "left": {"type": "minecraft:min",
              "left": {"type": "minecraft:min",
               "left": {"type": "minecraft:add",
                "left": {"type": "minecraft:mul", "left": {"type": "minecraft:square", "input": {"type": "minecraft:noise", "noise": "minecraft:cave_layer", "xz_scale": 1.0, "y_scale": 8.0}}, "right": 4.0},
                "right": {"type": "minecraft:clamp", "input": {"type": "minecraft:add", "left": {"type": "minecraft:noise", "noise": "minecraft:cave_cheese", "xz_scale": 1.0, "y_scale": 0.6666666666666666}, "right": 0.27}, "min": -1.0, "max": 1.0}},
               "right": "minecraft:overworld/caves/entrances"},
              "right": {"type": "minecraft:add", "left": "minecraft:overworld/caves/spaghetti_2d", "right": "minecraft:overworld/caves/spaghetti_roughness_function"}},
             "right": {"type": "minecraft:range_choice", "input": "minecraft:overworld/caves/pillars", "min_inclusive": -1000000.0, "max_exclusive": 0.03,
              "when_in_range": -1000000.0, "when_out_of_range": "minecraft:overworld/caves/pillars"}}
            """;

    private NetherCaves() {
    }

    static DensityFunction shape(HolderLookup.Provider registries) {
        return DensityFunction.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), JsonParser.parseString(SHAPE)).getOrThrow();
    }

    static int carve(ChunkAccess chunk, RandomState randomState, DensityFunction shape, Beardifier beardifier) {
        DensitySampler sampler = randomState.getSampler(shape);
        ChunkPos pos = chunk.getPos();
        int x0 = pos.getMinBlockX();
        int z0 = pos.getMinBlockZ();
        float[] corners = new float[5 * CORNERS_Y * 5];
        for (int cx = 0; cx < 5; cx++) {
            for (int cz = 0; cz < 5; cz++) {
                for (int cy = 0; cy < CORNERS_Y; cy++) {
                    int y = FIRST_CORNER_Y + cy * CELL_Y;
                    float value = sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, x0 + cx * 4, y, z0 + cz * 4);
                    float alpha = Mth.clamp((float) (y - BOTTOM) / (FADE - BOTTOM), 0.0F, 1.0F);
                    corners[(cx * 5 + cz) * CORNERS_Y + cy] = 0.64F * Mth.lerp(alpha, SOLID, value);
                }
            }
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();
        int carved = 0;
        for (int x = 0; x < 16; x++) {
            int cx = x >> 2;
            float fx = (x & 3) / 4.0F;
            for (int z = 0; z < 16; z++) {
                int cz = z >> 2;
                float fz = (z & 3) / 4.0F;
                for (int y = TOP; y >= BOTTOM; y--) {
                    int cy = (y - FIRST_CORNER_Y) / CELL_Y;
                    float fy = (float) ((y - FIRST_CORNER_Y) % CELL_Y) / CELL_Y;
                    float density = squeeze(Mth.lerp3(fx, fy, fz,
                            corner(corners, cx, cy, cz), corner(corners, cx + 1, cy, cz),
                            corner(corners, cx, cy + 1, cz), corner(corners, cx + 1, cy + 1, cz),
                            corner(corners, cx, cy, cz + 1), corner(corners, cx + 1, cy, cz + 1),
                            corner(corners, cx, cy + 1, cz + 1), corner(corners, cx + 1, cy + 1, cz + 1)));
                    if (density >= 0.0F) continue;
                    cursor.set(x0 + x, y, z0 + z);
                    BlockState state = chunk.getBlockState(cursor);
                    if (state.isAir() || !state.getFluidState().isEmpty() || state.is(Blocks.BEDROCK)) continue;
                    if (touchesFluid(chunk, cursor, near, x, z)) continue;
                    if (density + beardifier.sampleValue(SamplerContext.EMPTY_UNCACHED, cursor.getX(), y, cursor.getZ()) >= 0.0F) continue;
                    chunk.setBlockState(cursor, air);
                    carved++;
                }
            }
        }
        return carved;
    }

    static void seal(WorldGenLevel region, ChunkAccess chunk, StackedChunkGenerator generator) {
        ChunkPos pos = chunk.getPos();
        int x0 = pos.getMinBlockX();
        int z0 = pos.getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 16; i++) {
            sealColumn(region, chunk, generator, cursor, near, x0, z0 + i, -1, 0);
            sealColumn(region, chunk, generator, cursor, near, x0 + 15, z0 + i, 1, 0);
            sealColumn(region, chunk, generator, cursor, near, x0 + i, z0, 0, -1);
            sealColumn(region, chunk, generator, cursor, near, x0 + i, z0 + 15, 0, 1);
        }
    }

    private static void sealColumn(WorldGenLevel region, ChunkAccess chunk, StackedChunkGenerator generator, BlockPos.MutableBlockPos cursor,
                                   BlockPos.MutableBlockPos near, int x, int z, int dx, int dz) {
        for (int y = TOP; y >= BOTTOM; y--) {
            if (!chunk.getBlockState(cursor.set(x, y, z)).isAir()) continue;
            if (region.getFluidState(near.set(x + dx, y, z + dz)).isEmpty()) continue;
            BlockState rock = y >= StackBand.OVERWORLD.worldMinY() ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
            generator.setBlockInBand(chunk, cursor, rock);
        }
    }

    private static float corner(float[] corners, int cx, int cy, int cz) {
        return corners[(cx * 5 + cz) * CORNERS_Y + cy];
    }

    private static float squeeze(float value) {
        float clamped = Mth.clamp(value, -1.0F, 1.0F);
        return clamped / 2.0F - clamped * clamped * clamped / 24.0F;
    }

    private static boolean touchesFluid(ChunkAccess chunk, BlockPos cursor, BlockPos.MutableBlockPos near, int localX, int localZ) {
        int x = cursor.getX();
        int y = cursor.getY();
        int z = cursor.getZ();
        if (!chunk.getFluidState(near.set(x, y + 1, z)).isEmpty()) return true;
        if (localX > 0 && !chunk.getFluidState(near.set(x - 1, y, z)).isEmpty()) return true;
        if (localX < 15 && !chunk.getFluidState(near.set(x + 1, y, z)).isEmpty()) return true;
        if (localZ > 0 && !chunk.getFluidState(near.set(x, y, z - 1)).isEmpty()) return true;
        return localZ < 15 && !chunk.getFluidState(near.set(x, y, z + 1)).isEmpty();
    }
}
