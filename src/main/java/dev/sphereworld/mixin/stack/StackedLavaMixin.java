package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class StackedLavaMixin {
    private StackedLavaMixin() {
    }

    @Mixin(LavaFluid.class)
    public abstract static class Lava {
        @ModifyReturnValue(method = "isFastLava", at = @At("RETURN"))
        private static boolean sphereworld$atPosition(boolean original, @Local(argsOnly = true) LevelReader level) {
            BlockPos pos = StackedWorld.FLUID_AT.get();
            return pos == null ? original : level.environmentAttributes().getValue(EnvironmentAttributes.FAST_LAVA, pos);
        }
    }

    @Mixin(FlowingFluid.class)
    public abstract static class Flowing {
        @Inject(method = "tick", at = @At("HEAD"))
        private void sphereworld$enter(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
            StackedWorld.FLUID_AT.set(pos.immutable());
        }

        @Inject(method = "tick", at = @At("RETURN"))
        private void sphereworld$leave(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
            StackedWorld.FLUID_AT.set(null);
        }
    }

    @Mixin(LiquidBlock.class)
    public abstract static class Block {
        @WrapOperation(method = {"onPlace", "updateShape", "neighborChanged"}, at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/level/material/FlowingFluid;getTickDelay(Lnet/minecraft/world/level/LevelReader;)I"))
        private int sphereworld$delayAt(FlowingFluid fluid, LevelReader level, Operation<Integer> original, @Local(argsOnly = true, ordinal = 0) BlockPos pos) {
            BlockPos previous = StackedWorld.FLUID_AT.get();
            StackedWorld.FLUID_AT.set(pos);
            try {
                return original.call(fluid, level);
            } finally {
                StackedWorld.FLUID_AT.set(previous);
            }
        }
    }

    @Mixin(Entity.class)
    public abstract static class Pushed {
        @ModifyExpressionValue(method = "updateFluidInteraction", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getDimensionValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;)Ljava/lang/Object;"))
        private Object sphereworld$lavaCurrentAt(Object value) {
            Entity self = (Entity) (Object) this;
            return value instanceof Boolean ? self.level().environmentAttributes().getValue(EnvironmentAttributes.FAST_LAVA, self.position()) : value;
        }
    }
}
