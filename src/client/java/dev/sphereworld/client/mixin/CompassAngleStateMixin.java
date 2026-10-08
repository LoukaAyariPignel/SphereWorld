package dev.sphereworld.client.mixin;

import dev.sphereworld.client.ClientPlanet;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CompassAngleState.class)
abstract class CompassAngleStateMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "isValidCompassTargetPos", at = @At("RETURN"), cancellable = true)
    private static void sphereworld$sameLayer(net.minecraft.world.entity.ItemOwner owner, net.minecraft.core.GlobalPos target,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        var band = dev.sphereworld.worldgen.stacked.StackedAmbience.bandAt(owner.level(), owner.position());
        if (band != null && band != dev.sphereworld.worldgen.stacked.StackBand.at(target.pos().getY())) cir.setReturnValue(false);
    }

    @ModifyVariable(method = "getAngleFromEntityToPos", at = @At("HEAD"), argsOnly = true)
    private static BlockPos sphereworld$nearestTarget(BlockPos target) {
        return ClientPlanet.pos(target);
    }
}
