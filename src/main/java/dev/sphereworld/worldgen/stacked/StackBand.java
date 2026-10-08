package dev.sphereworld.worldgen.stacked;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.LevelHeightAccessor;

public record StackBand(String name, int nativeMinY, int nativeHeight, int offset) {
    public static final StackBand NETHER = new StackBand("nether", 0, 128, -192);
    public static final StackBand OVERWORLD = new StackBand("overworld", -64, 384, 0);
    public static final StackBand END = new StackBand("end", 0, 256, 320);

    public static final int WORLD_MIN_Y = NETHER.worldMinY();
    public static final int WORLD_HEIGHT = END.worldMaxY() + 1 - WORLD_MIN_Y;

    public int worldMinY() {
        return nativeMinY + offset;
    }

    public int worldMaxY() {
        return nativeMinY + offset + nativeHeight - 1;
    }

    public boolean containsWorldY(int y) {
        return y >= worldMinY() && y <= worldMaxY();
    }

    public int sectionOffset() {
        return offset >> 4;
    }

    public int quartOffset() {
        return QuartPos.fromBlock(offset);
    }

    public int worldQuart(int nativeQuartY) {
        int min = QuartPos.fromBlock(nativeMinY);
        int max = QuartPos.fromBlock(nativeMinY + nativeHeight - 1);
        return Math.clamp(nativeQuartY, min, max) + quartOffset();
    }

    public BlockPos toWorld(BlockPos nativePos) {
        return offset == 0 ? nativePos : nativePos.above(offset);
    }

    public BlockPos toNative(BlockPos worldPos) {
        return offset == 0 ? worldPos : worldPos.below(offset);
    }

    public LevelHeightAccessor heightAccessor() {
        return LevelHeightAccessor.create(nativeMinY, nativeHeight);
    }

    public int firstWorldSectionIndex() {
        return (worldMinY() - WORLD_MIN_Y) >> 4;
    }

    public int sectionCount() {
        return nativeHeight >> 4;
    }

    public static StackBand at(int worldY) {
        if (worldY < OVERWORLD.worldMinY()) return NETHER;
        if (worldY > OVERWORLD.worldMaxY()) return END;
        return OVERWORLD;
    }
}
