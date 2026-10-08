package dev.sphereworld.worldgen;

import dev.sphereworld.planet.PlanetGeometry;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.synth.Noise;

public record PeriodicNoise(Noise inner, PlanetGeometry geometry, double scale, int band) implements Noise {
    @Override
    public Interval range() {
        return inner.range();
    }

    @Override
    public float get(double x, double y) {
        return inner.get(x, y);
    }

    @Override
    public float get(double x, double y, double z) {
        double bx = geometry.canonical(x / scale);
        double bz = geometry.canonical(z / scale);
        float wx = weight(bx);
        float wz = weight(bz);
        double c = geometry.circumference();
        if (wx == 1.0F && wz == 1.0F) return inner.get(bx * scale, y, bz * scale);
        float w = wx * wz;
        float sum = w * inner.get(bx * scale, y, bz * scale);
        float norm = w * w;
        if (wx < 1.0F) {
            w = (1.0F - wx) * wz;
            sum += w * inner.get((bx - c) * scale, y, bz * scale);
            norm += w * w;
        }
        if (wz < 1.0F) {
            w = wx * (1.0F - wz);
            sum += w * inner.get(bx * scale, y, (bz - c) * scale);
            norm += w * w;
        }
        if (wx < 1.0F && wz < 1.0F) {
            w = (1.0F - wx) * (1.0F - wz);
            sum += w * inner.get((bx - c) * scale, y, (bz - c) * scale);
            norm += w * w;
        }
        Interval range = inner.range();
        return Math.clamp(sum / (float) Math.sqrt(norm), range.min(), range.max());
    }

    private float weight(double canonical) {
        double start = geometry.half() - band;
        if (canonical < start) return 1.0F;
        float t = (float) ((geometry.half() - canonical) / band);
        return t * t * (3.0F - 2.0F * t);
    }
}
