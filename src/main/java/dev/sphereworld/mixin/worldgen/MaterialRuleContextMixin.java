package dev.sphereworld.mixin.worldgen;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.worldgen.PeriodicDensity;
import dev.sphereworld.worldgen.PeriodicNoise;
import dev.sphereworld.worldgen.PlanetRandomState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MaterialRuleContext.class)
abstract class MaterialRuleContextMixin {
    @Redirect(method = {"createNoiseSampler2d", "createNoiseSampler3d"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/RandomState;getOrCreateNoise(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/level/levelgen/synth/Noise;"))
    private Noise sphereworld$periodicNoise(RandomState state, ResourceKey<NormalNoise> key) {
        Noise noise = state.getOrCreateNoise(key);
        PlanetGeometry g = ((PlanetRandomState) (Object) state).sphereworld$planet();
        return g == null ? noise : new PeriodicNoise(noise, g, 1.0, PeriodicDensity.bandWidth(g));
    }
}
