package dev.sphereworld.client.mixin;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DebugEntryPosition.class)
abstract class DebugEntryPositionMixin {
    @Redirect(method = "display", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double sphereworld$x(Entity entity) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        return g == null ? entity.getX() : g.canonical(entity.getX());
    }

    @Redirect(method = "display", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double sphereworld$z(Entity entity) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        return g == null ? entity.getZ() : g.canonical(entity.getZ());
    }

    @Redirect(method = "display", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos sphereworld$block(Entity entity) {
        PlanetGeometry g = Planets.of(Minecraft.getInstance().level);
        BlockPos pos = entity.blockPosition();
        return g == null ? pos : new BlockPos(g.canonical(pos.getX()), pos.getY(), g.canonical(pos.getZ()));
    }
}
