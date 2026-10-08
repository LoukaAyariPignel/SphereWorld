package dev.sphereworld.mixin.worldgen;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.worldgen.PeriodicDensity;
import dev.sphereworld.worldgen.PeriodicNoise;
import dev.sphereworld.worldgen.PeriodicPositionalRandom;
import dev.sphereworld.worldgen.PlanetMaterials;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.material.MaterialSystem;
import net.minecraft.world.level.levelgen.synth.Noise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MaterialSystem.class)
abstract class MaterialSystemMixin implements PlanetMaterials {
    @Shadow @Final @Mutable private Noise clayBandsOffsetNoise;
    @Shadow @Final @Mutable private Noise badlandsPillarNoise;
    @Shadow @Final @Mutable private Noise badlandsPillarRoofNoise;
    @Shadow @Final @Mutable private Noise badlandsSurfaceNoise;
    @Shadow @Final @Mutable private Noise icebergPillarNoise;
    @Shadow @Final @Mutable private Noise icebergPillarRoofNoise;
    @Shadow @Final @Mutable private Noise icebergSurfaceNoise;
    @Shadow @Final @Mutable private PositionalRandomFactory noiseRandom;
    @Shadow @Final @Mutable private Noise surfaceNoise;
    @Shadow @Final @Mutable private Noise surfaceSecondaryNoise;

    @Unique private boolean sphereworld$periodic;

    @Override
    public void sphereworld$makePeriodic(PlanetGeometry g) {
        if (sphereworld$periodic) return;
        sphereworld$periodic = true;
        int band = PeriodicDensity.bandWidth(g);
        clayBandsOffsetNoise = new PeriodicNoise(clayBandsOffsetNoise, g, 1.0, band);
        surfaceNoise = new PeriodicNoise(surfaceNoise, g, 1.0, band);
        surfaceSecondaryNoise = new PeriodicNoise(surfaceSecondaryNoise, g, 1.0, band);
        badlandsSurfaceNoise = new PeriodicNoise(badlandsSurfaceNoise, g, 1.0, band);
        badlandsPillarNoise = new PeriodicNoise(badlandsPillarNoise, g, 0.2, band);
        badlandsPillarRoofNoise = new PeriodicNoise(badlandsPillarRoofNoise, g, 0.75, band);
        icebergSurfaceNoise = new PeriodicNoise(icebergSurfaceNoise, g, 1.0, band);
        icebergPillarNoise = new PeriodicNoise(icebergPillarNoise, g, 1.28, band);
        icebergPillarRoofNoise = new PeriodicNoise(icebergPillarRoofNoise, g, 1.17, band);
        noiseRandom = new PeriodicPositionalRandom(noiseRandom, g.circumference());
    }
}
