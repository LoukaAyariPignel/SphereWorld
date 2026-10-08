package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PortalForcer.class)
abstract class StackedPortalForcerMixin {
    @WrapOperation(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getMinY()I"))
    private int sphereworld$bandMin(ServerLevel level, Operation<Integer> original) {
        StackBand band = StackedWorld.PORTAL_BAND.get();
        return band == null ? original.call(level) : band.worldMinY();
    }

    @WrapOperation(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getMaxY()I"))
    private int sphereworld$bandMax(ServerLevel level, Operation<Integer> original) {
        StackBand band = StackedWorld.PORTAL_BAND.get();
        return band == null ? original.call(level) : band.worldMaxY();
    }

    @WrapOperation(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getLogicalHeight()I"))
    private int sphereworld$bandHeight(ServerLevel level, Operation<Integer> original) {
        StackBand band = StackedWorld.PORTAL_BAND.get();
        return band == null ? original.call(level) : band.nativeHeight();
    }

    @WrapOperation(method = "createPortal", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private int sphereworld$bandSurface(ServerLevel level, Heightmap.Types type, int x, int z, Operation<Integer> original) {
        int height = original.call(level, type, x, z);
        StackBand band = StackedWorld.PORTAL_BAND.get();
        return band == null ? height : StackedWorld.bandSurface(level, band, x, z, height);
    }

    @ModifyConstant(method = "createPortal", constant = @Constant(intValue = 70))
    private int sphereworld$bandFallback(int y) {
        StackBand band = StackedWorld.PORTAL_BAND.get();
        return band == null ? y : y + band.offset();
    }
}
