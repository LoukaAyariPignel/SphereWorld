package dev.sphereworld.net;

import dev.sphereworld.SphereWorld;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlanetDetailPayload(Identifier dimension, int level, int centreX, int centreZ, int cell, int grid,
                                  short[] heights, int[] colors) implements CustomPacketPayload {
    public static final Type<PlanetDetailPayload> TYPE = new Type<>(SphereWorld.id("detail"));
    public static final int MAX_SIZE = 8 * 1024 * 1024;
    public static final StreamCodec<FriendlyByteBuf, PlanetDetailPayload> CODEC = StreamCodec.of(PlanetDetailPayload::write, PlanetDetailPayload::read);

    private static void write(FriendlyByteBuf buf, PlanetDetailPayload payload) {
        buf.writeIdentifier(payload.dimension);
        buf.writeVarInt(payload.level);
        buf.writeInt(payload.centreX);
        buf.writeInt(payload.centreZ);
        buf.writeVarInt(payload.cell);
        buf.writeVarInt(payload.grid);
        for (short h : payload.heights) buf.writeShort(h);
        for (int c : payload.colors) buf.writeMedium(c);
    }

    private static PlanetDetailPayload read(FriendlyByteBuf buf) {
        Identifier dimension = buf.readIdentifier();
        int level = buf.readVarInt();
        int centreX = buf.readInt();
        int centreZ = buf.readInt();
        int cell = buf.readVarInt();
        int grid = buf.readVarInt();
        if (grid <= 0 || grid > 1024) throw new IllegalArgumentException("Bad planet detail grid " + grid);
        short[] heights = new short[grid * grid];
        int[] colors = new int[grid * grid];
        for (int i = 0; i < heights.length; i++) heights[i] = buf.readShort();
        for (int i = 0; i < colors.length; i++) colors[i] = buf.readUnsignedMedium();
        return new PlanetDetailPayload(dimension, level, centreX, centreZ, cell, grid, heights, colors);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
