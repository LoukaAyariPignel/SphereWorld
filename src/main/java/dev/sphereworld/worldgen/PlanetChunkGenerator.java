package dev.sphereworld.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.sphereworld.planet.PlanetConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public class PlanetChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<PlanetChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(PlanetChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(PlanetChunkGenerator::generatorSettings),
                    PlanetConfig.CODEC.fieldOf("sphereworld").forGetter(PlanetChunkGenerator::planetConfig))
            .apply(i, i.stable(PlanetChunkGenerator::new)));

    private final PlanetConfig planetConfig;

    public PlanetChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, PlanetConfig planetConfig) {
        super(biomeSource, settings);
        this.planetConfig = planetConfig;
    }

    public PlanetConfig planetConfig() {
        return planetConfig;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
}
