package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MapItemSavedData.class)
abstract class MapSeamMixin {
    @Shadow @Final public int centerX;
    @Shadow @Final public int centerZ;

    @ModifyVariable(method = "addDecoration", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double sphereworld$markerX(double x, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) LevelAccessor level) {
        PlanetGeometry g = level instanceof Level l ? Planets.of(l) : null;
        return g == null ? x : g.nearestImage(x, centerX);
    }

    @ModifyVariable(method = "addDecoration", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double sphereworld$markerZ(double z, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) LevelAccessor level) {
        PlanetGeometry g = level instanceof Level l ? Planets.of(l) : null;
        return g == null ? z : g.nearestImage(z, centerZ);
    }
}
