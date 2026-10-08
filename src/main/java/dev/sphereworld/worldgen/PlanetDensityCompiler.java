package dev.sphereworld.worldgen;

import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;

public interface PlanetDensityCompiler {
    void sphereworld$setPlanetRule(DfRewriteRule rule);
}
