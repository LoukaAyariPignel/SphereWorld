package dev.sphereworld.mixin.worldgen;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.stack.StackTravel;
import dev.sphereworld.worldgen.PeriodicDensity;
import dev.sphereworld.worldgen.PlanetDensityCompiler;
import dev.sphereworld.worldgen.PeriodicPositionalRandom;
import dev.sphereworld.worldgen.PlanetRandomState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctionCompiler;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RandomState.class)
abstract class RandomStateMixin implements PlanetRandomState {
    @Shadow @Final private DensityFunctionCompiler densityFunctionCompiler;

    @Unique private @Nullable PlanetGeometry sphereworld$planet;
    @Unique private StackTravel.@Nullable Boundaries sphereworld$boundaries;

    @Override
    public void sphereworld$setBoundaries(StackTravel.@Nullable Boundaries boundaries) {
        sphereworld$boundaries = boundaries;
    }

    @Override
    public StackTravel.@Nullable Boundaries sphereworld$boundaries() {
        return sphereworld$boundaries;
    }

    @Shadow @Final private net.minecraft.world.level.levelgen.NoiseRouter router;

    @Override
    public void sphereworld$attachPlanet(PlanetGeometry geometry, dev.sphereworld.worldgen.ClimateWindow window) {
        sphereworld$planet = geometry;
        ((dev.sphereworld.worldgen.PlanetMaterials) ((net.minecraft.world.level.levelgen.RandomState) (Object) this).surfaceSystem())
                .sphereworld$makePeriodic(geometry);
        ((PlanetDensityCompiler) densityFunctionCompiler).sphereworld$setPlanetRule(PeriodicDensity.rule(
                geometry, window, dev.sphereworld.worldgen.BiomeGuarantee.climateFunctions(router)));
    }

    @Override
    public @Nullable PlanetGeometry sphereworld$planet() {
        return sphereworld$planet;
    }

    @Inject(method = "getOrCreateRandomFactory", at = @At("RETURN"), cancellable = true)
    private void sphereworld$periodicRandom(Identifier name, CallbackInfoReturnable<PositionalRandomFactory> cir) {
        PlanetGeometry planet = sphereworld$planet;
        if (planet != null) {
            boolean aquifer = name.getPath().equals("aquifer");
            int period = aquifer ? planet.circumference() / 16 : planet.circumference();
            cir.setReturnValue(new PeriodicPositionalRandom(cir.getReturnValue(), period));
        }
    }
}
