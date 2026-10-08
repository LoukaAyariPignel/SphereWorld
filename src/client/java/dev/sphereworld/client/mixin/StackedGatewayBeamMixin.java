package dev.sphereworld.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedAmbience;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.TheEndGatewayRenderer;
import net.minecraft.client.renderer.blockentity.state.EndGatewayRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TheEndGatewayRenderer.class)
abstract class StackedGatewayBeamMixin {
    @WrapOperation(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/EndGatewayRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BeaconRenderer;submitBeaconBeam(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/resources/Identifier;FFIIIFF)V"))
    private void sphereworld$beamStopsAtEndFloor(PoseStack poseStack, SubmitNodeCollector collector, Identifier texture, float scale, float animationTime,
            int beamStart, int height, int color, float solidRadius, float glowRadius, Operation<Void> original, EndGatewayRenderState state) {
        int y = state.blockPos.getY();
        if (beamStart < 0 && StackBand.END.containsWorldY(y) && StackedAmbience.isStackedType(Minecraft.getInstance().level)) {
            int down = Math.min(-beamStart, y - StackBand.END.worldMinY());
            height = height + beamStart + down;
            beamStart = -down;
        }
        original.call(poseStack, collector, texture, scale, animationTime, beamStart, height, color, solidRadius, glowRadius);
    }
}
