package dev.sphereworld.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sphereworld.client.ClientPlanet;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

public final class CoordinateDisplayMixin {
    private CoordinateDisplayMixin() {
    }

    @Mixin(targets = "net.minecraft.client.gui.components.debug.DebugEntryLookingAt$DebugEntryLookingAtState")
    abstract static class LookingAt {
        @WrapOperation(method = "extractInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getX()I"))
        private int sphereworld$x(BlockPos pos, Operation<Integer> original) {
            return ClientPlanet.shownBlock(original.call(pos));
        }

        @WrapOperation(method = "extractInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getZ()I"))
        private int sphereworld$z(BlockPos pos, Operation<Integer> original) {
            return ClientPlanet.shownBlock(original.call(pos));
        }
    }

    @Mixin(KeyboardHandler.class)
    abstract static class CopiedCommands {
        @WrapOperation(method = "handleDebugKeys", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
        private double sphereworld$x(LocalPlayer player, Operation<Double> original) {
            return ClientPlanet.shownX(original.call(player));
        }

        @WrapOperation(method = "handleDebugKeys", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
        private double sphereworld$z(LocalPlayer player, Operation<Double> original) {
            return ClientPlanet.shownX(original.call(player));
        }

        @ModifyVariable(method = "copyCreateBlockCommand", at = @At("HEAD"), argsOnly = true)
        private BlockPos sphereworld$block(BlockPos pos) {
            return ClientPlanet.shown(pos);
        }

        @ModifyVariable(method = "copyCreateEntityCommand", at = @At("HEAD"), argsOnly = true)
        private Vec3 sphereworld$entity(Vec3 pos) {
            return ClientPlanet.shown(pos);
        }
    }

    @Pseudo
    @Mixin(targets = "me.flashyreese.mods.sodiumextra.client.gui.SodiumExtraHud", remap = false)
    abstract static class SodiumExtraHud {
        @WrapOperation(method = "onStartTick", require = 0, at = @At(value = "INVOKE",
                target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
        private Vec3 sphereworld$position(LocalPlayer player, Operation<Vec3> original) {
            return ClientPlanet.shown(original.call(player));
        }
    }

    @Pseudo
    @Mixin(targets = "me.flashyreese.mods.sodiumextra.client.gui.SodiumExtraDebugEntryCoords", remap = false)
    abstract static class SodiumExtraDebugCoords {
        @WrapOperation(method = "display", require = 0, at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;"))
        private Vec3 sphereworld$position(Entity entity, Operation<Vec3> original) {
            return ClientPlanet.shown(original.call(entity));
        }
    }
}
