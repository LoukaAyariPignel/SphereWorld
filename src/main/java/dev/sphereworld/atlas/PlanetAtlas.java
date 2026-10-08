package dev.sphereworld.atlas;

import dev.sphereworld.planet.PlanetGeometry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public record PlanetAtlas(Identifier dimension, int circumference, int size, int seaLevel, short[] heights, int[] colors, int[] biomes) {
    public static final short VOID = Short.MIN_VALUE;

    private static final int FINEST_CELL = 8;
    private static final int MAX_SIZE = 512;

    public static final StreamCodec<FriendlyByteBuf, PlanetAtlas> CODEC = StreamCodec.of(PlanetAtlas::write, PlanetAtlas::read);

    public int cellSize() {
        return circumference / size;
    }

    public int index(int blockX, int blockZ) {
        int half = circumference / 2;
        int col = Math.floorMod(blockX + half, circumference) / cellSize();
        int row = Math.floorMod(blockZ + half, circumference) / cellSize();
        return row * size + col;
    }

    public int cellCentreX(int index) {
        return -circumference / 2 + (index % size) * cellSize() + cellSize() / 2;
    }

    public int cellCentreZ(int index) {
        return -circumference / 2 + (index / size) * cellSize() + cellSize() / 2;
    }

    public PlanetAtlas copy() {
        return new PlanetAtlas(dimension, circumference, size, seaLevel, heights.clone(), colors.clone(), biomes.clone());
    }

    public static int sizeFor(PlanetGeometry geometry) {
        int c = geometry.circumference();
        for (int cell = FINEST_CELL; cell <= c; cell++) {
            if (c % cell == 0 && c / cell <= MAX_SIZE) return c / cell;
        }
        return 1;
    }

    public static PlanetAtlas compute(ServerLevel level, PlanetGeometry geometry) {
        return AtlasBuilder.build(level, geometry, 0);
    }

    private static void write(FriendlyByteBuf buf, PlanetAtlas atlas) {
        buf.writeIdentifier(atlas.dimension);
        buf.writeVarInt(atlas.circumference);
        buf.writeVarInt(atlas.size);
        buf.writeVarInt(atlas.seaLevel);
        for (short h : atlas.heights) buf.writeShort(h);
        for (int c : atlas.colors) buf.writeMedium(c);
        for (int b : atlas.biomes) buf.writeVarInt(b);
    }

    private static PlanetAtlas read(FriendlyByteBuf buf) {
        Identifier dimension = buf.readIdentifier();
        int circumference = buf.readVarInt();
        int size = buf.readVarInt();
        int seaLevel = buf.readVarInt();
        if (size <= 0 || size > 2048) throw new IllegalArgumentException("Bad atlas size " + size);
        short[] heights = new short[size * size];
        int[] colors = new int[size * size];
        int[] biomes = new int[size * size];
        for (int i = 0; i < heights.length; i++) heights[i] = buf.readShort();
        for (int i = 0; i < colors.length; i++) colors[i] = buf.readUnsignedMedium();
        for (int i = 0; i < biomes.length; i++) biomes[i] = buf.readVarInt();
        return new PlanetAtlas(dimension, circumference, size, seaLevel, heights, colors, biomes);
    }
}
