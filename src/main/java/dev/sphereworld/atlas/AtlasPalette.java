package dev.sphereworld.atlas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;

public final class AtlasPalette {
    private AtlasPalette() {
    }

    public record Sample(short height, int color) {
    }

    public static Sample estimate(Holder<Biome> biome, int top, int seaLevel, double x, double z) {
        if (top < seaLevel) {
            if (biome.is(Biomes.FROZEN_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN) || biome.is(Biomes.FROZEN_RIVER)) {
                return new Sample((short) seaLevel, MapColor.ICE.col);
            }
            return new Sample((short) seaLevel, water(biome.value(), seaLevel - top));
        }
        return new Sample((short) top, estimateLand(biome, top, x, z) & 0xFFFFFF);
    }

    private static int estimateLand(Holder<Biome> holder, int top, double x, double z) {
        Biome biome = holder.value();
        if (holder.is(BiomeTags.IS_BADLANDS)) return MapColor.COLOR_ORANGE.col;
        if (holder.is(Biomes.DESERT) || holder.is(Biomes.BEACH)) return MapColor.SAND.col;
        if (holder.is(Biomes.MUSHROOM_FIELDS)) return MapColor.COLOR_PURPLE.col;
        if (holder.is(Biomes.STONY_PEAKS) || holder.is(Biomes.STONY_SHORE) || holder.is(Biomes.WINDSWEPT_GRAVELLY_HILLS)) {
            return MapColor.STONE.col;
        }
        if (holder.is(Biomes.FROZEN_PEAKS) || holder.is(Biomes.JAGGED_PEAKS) || holder.is(Biomes.ICE_SPIKES)) {
            return MapColor.SNOW.col;
        }
        if (biome.coldEnoughToSnow(BlockPos.containing(x, top, z), 63)) return MapColor.SNOW.col;
        if (holder.is(Biomes.MANGROVE_SWAMP)) return darken(FoliageColor.FOLIAGE_MANGROVE & 0xFFFFFF, 0.85F);
        if (holder.is(Biomes.DARK_FOREST) || holder.is(Biomes.PALE_GARDEN)) return darken(foliage(biome), 0.75F);
        if (holder.is(BiomeTags.IS_FOREST) || holder.is(BiomeTags.IS_JUNGLE) || holder.is(BiomeTags.IS_TAIGA)) {
            return darken(holder.is(BiomeTags.IS_TAIGA) ? FoliageColor.FOLIAGE_EVERGREEN & 0xFFFFFF : foliage(biome), 0.85F);
        }
        if (holder.is(BiomeTags.IS_END)) return MapColor.SAND.col;
        return grass(biome, x, z);
    }

    public static Sample sample(LevelChunk chunk, int blockX, int blockZ) {
        return sample(chunk, blockX, blockZ, Integer.MAX_VALUE);
    }

    public static Sample sample(LevelChunk chunk, int blockX, int blockZ, int surfaceTop) {
        int lx = blockX & 15;
        int lz = blockZ & 15;
        int minY = chunk.getMinY();
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(blockX, y, blockZ);
        if (y > surfaceTop) {
            y = surfaceTop;
            pos.setY(y);
            while (y > minY && chunk.getBlockState(pos).isAir()) pos.setY(--y);
        }
        if (y < minY) return null;
        BlockState state = chunk.getBlockState(pos);

        for (int i = 0; i < 8 && y > minY && state.getMapColor(chunk, pos) == MapColor.NONE && state.getFluidState().isEmpty(); i++) {
            pos.setY(--y);
            state = chunk.getBlockState(pos);
        }
        Holder<Biome> biome = chunk.getNoiseBiome(QuartPos.fromBlock(blockX), QuartPos.fromBlock(y), QuartPos.fromBlock(blockZ));
        short height = (short) (y + 1);
        FluidState fluid = state.getFluidState();
        if (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER)) {
            int floor = y;
            BlockPos.MutableBlockPos below = pos.mutable();
            while (floor > minY && !chunk.getBlockState(below.setY(floor)).getFluidState().isEmpty()) floor--;
            return new Sample(height, water(biome.value(), y - floor));
        }
        return new Sample(height, tint(state, chunk, pos, biome.value(), blockX, blockZ) & 0xFFFFFF);
    }

    private static int tint(BlockState state, LevelChunk chunk, BlockPos pos, Biome biome, double x, double z) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.SUGAR_CANE)) {
            return grass(biome, x, z);
        }
        if (state.is(Blocks.SPRUCE_LEAVES)) return darken(FoliageColor.FOLIAGE_EVERGREEN & 0xFFFFFF, 0.85F);
        if (state.is(Blocks.BIRCH_LEAVES)) return darken(FoliageColor.FOLIAGE_BIRCH & 0xFFFFFF, 0.85F);
        if (state.is(Blocks.MANGROVE_LEAVES)) return darken(FoliageColor.FOLIAGE_MANGROVE & 0xFFFFFF, 0.85F);
        if (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.JUNGLE_LEAVES) || state.is(Blocks.ACACIA_LEAVES)
                || state.is(Blocks.DARK_OAK_LEAVES) || state.is(Blocks.VINE)) {
            return darken(foliage(biome), 0.85F);
        }
        MapColor color = state.getMapColor(chunk, pos);
        if (color == MapColor.NONE) return state.is(BlockTags.LEAVES) ? darken(foliage(biome), 0.85F) : grass(biome, x, z);
        return color.col;
    }

    private static final int GRASS_HOT_WET = 0x47CD33, GRASS_HOT_DRY = 0xBFB755, GRASS_COLD = 0x80B497;
    private static final int FOLIAGE_HOT_WET = 0x1ABF00, FOLIAGE_HOT_DRY = 0xAEA42A, FOLIAGE_COLD = 0x60A17B;

    static int grass(Biome biome, double x, double z) {
        int base = biome.getSpecialEffects().grassColorOverride()
                .orElseGet(() -> colorMap(biome, GRASS_HOT_WET, GRASS_HOT_DRY, GRASS_COLD));
        return biome.getSpecialEffects().grassColorModifier().modifyColor(x, z, base) & 0xFFFFFF;
    }

    static int foliage(Biome biome) {
        return biome.getSpecialEffects().foliageColorOverride()
                .orElseGet(() -> colorMap(biome, FOLIAGE_HOT_WET, FOLIAGE_HOT_DRY, FOLIAGE_COLD)) & 0xFFFFFF;
    }

    private static int colorMap(Biome biome, int hotWet, int hotDry, int cold) {
        Biome.ClimateSettings climate = biome.climateSettings;
        double temp = Math.clamp(climate.temperature(), 0.0F, 1.0F);
        double rain = Math.clamp(climate.downfall(), 0.0F, 1.0F) * temp;
        double wCold = 1.0 - temp;
        double wHotWet = rain;
        double wHotDry = 1.0 - wCold - wHotWet;
        int r = (int) Math.round(((hotWet >> 16) & 255) * wHotWet + ((hotDry >> 16) & 255) * wHotDry + ((cold >> 16) & 255) * wCold);
        int g = (int) Math.round(((hotWet >> 8) & 255) * wHotWet + ((hotDry >> 8) & 255) * wHotDry + ((cold >> 8) & 255) * wCold);
        int b = (int) Math.round((hotWet & 255) * wHotWet + (hotDry & 255) * wHotDry + (cold & 255) * wCold);
        return (r << 16) | (g << 8) | b;
    }

    private static int water(Biome biome, int depth) {
        float shade = 0.9F - 0.45F * Math.min(1.0F, Math.max(0, depth) / 30.0F);
        return darken(biome.getWaterColor() & 0xFFFFFF, shade);
    }

    static int darken(int rgb, float factor) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * factor));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * factor));
        int b = Math.min(255, (int) ((rgb & 255) * factor));
        return (r << 16) | (g << 8) | b;
    }
}
