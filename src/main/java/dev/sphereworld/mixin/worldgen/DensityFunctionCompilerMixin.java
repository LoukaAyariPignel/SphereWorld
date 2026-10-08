package dev.sphereworld.mixin.worldgen;

import dev.sphereworld.worldgen.PlanetDensityCompiler;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctionCompiler;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DensityFunctionCompiler.class)
abstract class DensityFunctionCompilerMixin implements PlanetDensityCompiler {
    @Unique private volatile @Nullable DfRewriteRule sphereworld$planetRule;

    @Override
    public void sphereworld$setPlanetRule(DfRewriteRule rule) {
        sphereworld$planetRule = rule;
    }

    @ModifyVariable(method = "optimizeAndCompile", at = @At("HEAD"), argsOnly = true)
    private DensityFunction sphereworld$makePeriodic(DensityFunction function) {
        DfRewriteRule rule = sphereworld$planetRule;
        return rule == null ? function : rule.rewrite(function);
    }
}
