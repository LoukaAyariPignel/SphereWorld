package dev.sphereworld.mixin.seam;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OldMinecartBehavior.class)
abstract class MinecartSeamMixin {
    private static final String FLOOR = "Lnet/minecraft/util/Mth;floor(D)I";

    @ModifyExpressionValue(method = "moveAlongTrack", at = {
            @At(value = "INVOKE", target = FLOOR, ordinal = 0),
            @At(value = "INVOKE", target = FLOOR, ordinal = 2),
            @At(value = "INVOKE", target = FLOOR, ordinal = 4)})
    private int sphereworld$columnX(int x, @Local(argsOnly = true) ServerLevel level, @Local BlockPos pos) {
        PlanetGeometry g = PlanetWrap.server(level);
        return g == null ? x : pos.getX() + g.canonical(x - pos.getX());
    }

    @ModifyExpressionValue(method = "moveAlongTrack", at = {
            @At(value = "INVOKE", target = FLOOR, ordinal = 1),
            @At(value = "INVOKE", target = FLOOR, ordinal = 3),
            @At(value = "INVOKE", target = FLOOR, ordinal = 5)})
    private int sphereworld$columnZ(int z, @Local(argsOnly = true) ServerLevel level, @Local BlockPos pos) {
        PlanetGeometry g = PlanetWrap.server(level);
        return g == null ? z : pos.getZ() + g.canonical(z - pos.getZ());
    }
}
