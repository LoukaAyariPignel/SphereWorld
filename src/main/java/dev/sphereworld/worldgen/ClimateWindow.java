package dev.sphereworld.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ClimateWindow(int offsetX, int offsetZ, float climateScale) {
    public static final ClimateWindow VANILLA = new ClimateWindow(0, 0, 1.0F);

    public static final Codec<ClimateWindow> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.fieldOf("offset_x").forGetter(ClimateWindow::offsetX),
                    Codec.INT.fieldOf("offset_z").forGetter(ClimateWindow::offsetZ),
                    Codec.FLOAT.fieldOf("climate_scale").forGetter(ClimateWindow::climateScale))
            .apply(i, ClimateWindow::new));
}
