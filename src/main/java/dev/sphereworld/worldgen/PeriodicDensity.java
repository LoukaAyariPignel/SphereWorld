package dev.sphereworld.worldgen;

import com.mojang.serialization.MapCodec;
import dev.sphereworld.planet.PlanetGeometry;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.ScopedDensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.generator.DistanceToPointFunction;
import net.minecraft.world.level.levelgen.densityfunction.generator.EndIslandFunction;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;
import net.minecraft.world.level.levelgen.densityfunction.generator.ShiftNoiseFunction;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

public final class PeriodicDensity {
    private PeriodicDensity() {
    }

    public static DfRewriteRule rule(PlanetGeometry geometry) {
        return rule(geometry, ClimateWindow.VANILLA, java.util.Set.of());
    }

    public static DfRewriteRule rule(PlanetGeometry geometry, ClimateWindow window, java.util.Set<DensityFunction> climateFns) {
        return new Rule(geometry, bandWidth(geometry), window.offsetX(), window.offsetZ(), 1,
                Math.max(1, Math.round(window.climateScale())), java.util.Set.copyOf(climateFns));
    }

    public static int bandWidth(PlanetGeometry geometry) {
        return Math.max(64, Math.min(768, geometry.circumference() / 6));
    }

    private record Rule(PlanetGeometry geometry, int band, int offsetX, int offsetZ, int scale, int climateScale,
                        java.util.Set<DensityFunction> climateFns) implements DfRewriteRule {
        @Override
        public DensityFunction rewrite(DensityFunction function) {
            if (scale == 1 && climateScale != 1 && climateFns.contains(function)) {
                return new Rule(geometry, band, offsetX, offsetZ, climateScale, climateScale, climateFns).rewrite(
                        DfRewriteRule.INLINE_REFERENCE.rewrite(function));
            }
            function = DfRewriteRule.INLINE_REFERENCE.rewrite(function);
            if (function instanceof Periodic || function instanceof CanonicalCoordinates
                    || function instanceof PeriodicEndIslands) {
                return function;
            }
            if (function instanceof NoiseFunction || function instanceof ShiftNoiseFunction
                    || function instanceof BlendedNoise) {
                return new Periodic(function, geometry, band, scale, offsetX, offsetZ);
            }
            if (function instanceof EndIslandFunction) {
                return new PeriodicEndIslands(geometry);
            }
            if (function instanceof DistanceToPointFunction) {
                return new CanonicalCoordinates(function, geometry);
            }
            return function.rewriteChildren(this);
        }
    }

    record Periodic(DensityFunction input, PlanetGeometry geometry, int band, int scale, int offsetX, int offsetZ)
            implements DensityFunction {
        @Override
        public DensitySampler compileSampler(CompileContext context) {
            return new PeriodicSampler(input.compileSampler(context), geometry, band, input.range(), scale, offsetX, offsetZ);
        }

        @Override
        public DensityFunction rewriteChildren(DfRewriteRule rule) {
            DensityFunction rewritten = rule.rewrite(input);
            return rewritten == input ? this : new Periodic(rewritten, geometry, band, scale, offsetX, offsetZ);
        }

        @Override
        public Interval range() {
            return input.range();
        }

        @Override
        public int domainAxes() {
            return input.domainAxes();
        }

        @Override
        public MapCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException("Runtime-only planet density wrapper");
        }
    }

    record CanonicalCoordinates(DensityFunction input, PlanetGeometry geometry) implements DensityFunction {
        @Override
        public DensitySampler compileSampler(CompileContext context) {
            DensitySampler inner = input.compileSampler(context);
            PlanetGeometry g = geometry;
            return new DensitySampler() {
                @Override
                public void sampleVolume(SamplerContext ctx, DensityBuffer out, DensityVolume volume) {
                    DensitySampler.sampleVolumeNaive(ctx, out, volume, this);
                }

                @Override
                public float sampleValue(SamplerContext ctx, int x, int y, int z) {
                    return inner.sampleValue(ctx, g.canonical(x), y, g.canonical(z));
                }
            };
        }

        @Override
        public DensityFunction rewriteChildren(DfRewriteRule rule) {
            return this;
        }

        @Override
        public Interval range() {
            return input.range();
        }

        @Override
        public int domainAxes() {
            return input.domainAxes();
        }

        @Override
        public MapCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException("Runtime-only planet density wrapper");
        }
    }

    static final class PeriodicSampler implements DensitySampler {
        private final DensitySampler inner;
        private final int c;
        private final int h;
        private final int bandStart;
        private final float band;
        private final float min;
        private final float max;

        private final int scale;
        private final int offsetX;
        private final int offsetZ;

        PeriodicSampler(DensitySampler inner, PlanetGeometry geometry, int band, Interval range, int scale, int offsetX, int offsetZ) {
            this.inner = inner;
            this.scale = scale;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
            this.c = geometry.circumference();
            this.h = geometry.half();
            this.bandStart = h - band;
            this.band = band;
            this.min = range.min();
            this.max = range.max();
        }

        private int tx(int x) {
            return x * scale + offsetX;
        }

        private int tz(int z) {
            return z * scale + offsetZ;
        }

        private int canonical(int v) {
            return Math.floorMod(v + h, c) - h;
        }

        private float weight(int canonical) {
            if (canonical < bandStart) return 1.0F;
            float t = (h - canonical) / band;
            return t * t * (3.0F - 2.0F * t);
        }

        private float clamp(float value) {
            return value < min ? min : (value > max ? max : value);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            int x = canonical(blockX);
            int z = canonical(blockZ);
            float wx = weight(x);
            float wz = weight(z);
            if (wx == 1.0F && wz == 1.0F) {
                return inner.sampleValue(context, tx(x), blockY, tz(z));
            }
            float w = wx * wz;
            float sum = w * inner.sampleValue(context, tx(x), blockY, tz(z));
            float norm = w * w;
            if (wx < 1.0F) {
                w = (1.0F - wx) * wz;
                sum += w * inner.sampleValue(context, tx(x - c), blockY, tz(z));
                norm += w * w;
            }
            if (wz < 1.0F) {
                w = wx * (1.0F - wz);
                sum += w * inner.sampleValue(context, tx(x), blockY, tz(z - c));
                norm += w * w;
            }
            if (wx < 1.0F && wz < 1.0F) {
                w = (1.0F - wx) * (1.0F - wz);
                sum += w * inner.sampleValue(context, tx(x - c), blockY, tz(z - c));
                norm += w * w;
            }
            return clamp(sum / (float) Math.sqrt(norm));
        }

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer out, DensityVolume volume) {
            int shiftX = canonical(volume.minBlockX()) - volume.minBlockX();
            int shiftZ = canonical(volume.minBlockZ()) - volume.minBlockZ();
            int maxX = volume.maxBlockX() + shiftX;
            int maxZ = volume.maxBlockZ() + shiftZ;
            if (maxX >= h || maxZ >= h) {
                DensitySampler.sampleVolumeNaive(context, out, volume, this);
                return;
            }
            boolean bandX = maxX >= bandStart;
            boolean bandZ = maxZ >= bandStart;
            DensityVolume base = shiftX == 0 && shiftZ == 0 ? volume : shifted(volume, shiftX, shiftZ);
            inner.sampleVolume(context, out, noiseSpace(base, 0, 0));
            if (!bandX && !bandZ) return;

            ScopedDensityBuffer imageX = null;
            ScopedDensityBuffer imageZ = null;
            ScopedDensityBuffer imageXZ = null;
            try {
                if (bandX) {
                    imageX = context.acquireBuffer(base);
                    inner.sampleVolume(context, imageX, noiseSpace(base, -c, 0));
                }
                if (bandZ) {
                    imageZ = context.acquireBuffer(base);
                    inner.sampleVolume(context, imageZ, noiseSpace(base, 0, -c));
                }
                if (bandX && bandZ) {
                    imageXZ = context.acquireBuffer(base);
                    inner.sampleVolume(context, imageXZ, noiseSpace(base, -c, -c));
                }

                int sizeY = base.sizeY();
                for (int iz = 0; iz < base.sizeZ(); iz++) {
                    float wz = weight(base.blockZ(iz));
                    for (int ix = 0; ix < base.sizeX(); ix++) {
                        float wx = weight(base.blockX(ix));
                        if (wx == 1.0F && wz == 1.0F) continue;
                        float w00 = wx * wz;
                        float w10 = (1.0F - wx) * wz;
                        float w01 = wx * (1.0F - wz);
                        float w11 = (1.0F - wx) * (1.0F - wz);
                        float inv = 1.0F / (float) Math.sqrt(w00 * w00 + w10 * w10 + w01 * w01 + w11 * w11);
                        int index = base.indexUnchecked(ix, 0, iz);
                        for (int iy = 0; iy < sizeY; iy++, index++) {
                            float sum = w00 * out.get(index);
                            if (w10 != 0.0F) sum += w10 * imageX.get(index);
                            if (w01 != 0.0F) sum += w01 * imageZ.get(index);
                            if (w11 != 0.0F) sum += w11 * imageXZ.get(index);
                            out.set(index, clamp(sum * inv));
                        }
                    }
                }
            } finally {
                if (imageX != null) imageX.close();
                if (imageZ != null) imageZ.close();
                if (imageXZ != null) imageXZ.close();
            }
        }

        private DensityVolume noiseSpace(DensityVolume v, int dx, int dz) {
            if (scale == 1 && offsetX == 0 && offsetZ == 0) return dx == 0 && dz == 0 ? v : shifted(v, dx, dz);
            return new DensityVolume(v.sizeX(), v.sizeY(), v.sizeZ(),
                    tx(v.minBlockX() + dx), v.minBlockY(), tz(v.minBlockZ() + dz),
                    v.stepBlockX() * scale, v.stepBlockY(), v.stepBlockZ() * scale);
        }

        private static DensityVolume shifted(DensityVolume v, int dx, int dz) {
            return new DensityVolume(v.sizeX(), v.sizeY(), v.sizeZ(),
                    v.minBlockX() + dx, v.minBlockY(), v.minBlockZ() + dz,
                    v.stepBlockX(), v.stepBlockY(), v.stepBlockZ());
        }
    }
}
