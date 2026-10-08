package dev.sphereworld.worldgen;

import com.mojang.serialization.MapCodec;
import dev.sphereworld.planet.PlanetGeometry;
import net.minecraft.util.Interval;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

record PeriodicEndIslands(PlanetGeometry geometry) implements DensityFunction {
    @Override
    public DensitySampler compileSampler(CompileContext context) {
        RandomSource islandRandom = context.createEndIslandRandom();
        islandRandom.consumeCount(17292);
        return new Sampler(new SimplexNoise(islandRandom, true), geometry);
    }

    @Override
    public DensityFunction rewriteChildren(DfRewriteRule rule) {
        return this;
    }

    @Override
    public Interval range() {
        return Interval.of(-0.84375F, 0.5625F);
    }

    @Override
    public int domainAxes() {
        return AXIS_X | AXIS_Z;
    }

    @Override
    public MapCodec<? extends DensityFunction> codec() {
        throw new UnsupportedOperationException("Runtime-only planet density wrapper");
    }

    private record Sampler(SimplexNoise islandNoise, PlanetGeometry geometry) implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer out, DensityVolume volume) {
            for (int z = 0; z < volume.sizeZ(); z++) {
                int blockZ = volume.blockZ(z);
                for (int x = 0; x < volume.sizeX(); x++) {
                    float value = sampleValue(context, volume.blockX(x), 0, blockZ);
                    out.setRange(volume.indexUnchecked(x, 0, z), volume.sizeY(), value);
                }
            }
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return (heightValue(geometry.canonical(blockX), geometry.canonical(blockZ)) - 8.0F) / 128.0F;
        }

        private float heightValue(int blockX, int blockZ) {
            int sectionX = Math.floorDiv(blockX, 8);
            int sectionZ = Math.floorDiv(blockZ, 8);
            int chunkX = Math.floorDiv(sectionX, 2);
            int chunkZ = Math.floorDiv(sectionZ, 2);
            int subX = Math.floorMod(sectionX, 2);
            int subZ = Math.floorMod(sectionZ, 2);
            float offset = -100.0F;
            for (int xo = -12; xo <= 12; xo++) {
                for (int zo = -12; zo <= 12; zo++) {
                    long cellX = geometry.canonicalChunk(chunkX + xo);
                    long cellZ = geometry.canonicalChunk(chunkZ + zo);
                    if (cellX * cellX + cellZ * cellZ > 4096L && islandNoise.get(cellX, cellZ) < -0.9F) {
                        float islandSize = (Mth.abs((float) cellX) * 3439.0F + Mth.abs((float) cellZ) * 147.0F) % 13.0F + 9.0F;
                        float xd = subX - xo * 2;
                        float zd = subZ - zo * 2;
                        float candidate = 100.0F - Mth.sqrt(xd * xd + zd * zd) * islandSize;
                        offset = Math.max(offset, Mth.clamp(candidate, -100.0F, 80.0F));
                    }
                }
            }
            return offset;
        }
    }
}
