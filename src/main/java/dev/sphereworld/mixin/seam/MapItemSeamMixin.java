package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MapItem.class)
abstract class MapItemSeamMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double sphereworld$holderX(Entity player, Level level, Entity holder, MapItemSavedData data) {
        PlanetGeometry g = Planets.of(level);
        return g == null ? player.getX() : g.nearestImage(player.getX(), data.centerX);
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double sphereworld$holderZ(Entity player, Level level, Entity holder, MapItemSavedData data) {
        PlanetGeometry g = Planets.of(level);
        return g == null ? player.getZ() : g.nearestImage(player.getZ(), data.centerZ);
    }
}
