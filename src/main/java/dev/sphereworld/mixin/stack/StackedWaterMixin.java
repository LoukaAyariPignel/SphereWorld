package dev.sphereworld.mixin.stack;

import dev.sphereworld.worldgen.stacked.StackedAmbience;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
abstract class StackedWaterMixin {
    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void sphereworld$evaporate(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState target, CallbackInfo ci) {
        if (!target.is(FluidTags.WATER) || !(level instanceof Level real) || !StackedAmbience.isStackedType(real)) return;
        if (!real.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) return;
        real.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
        ci.cancel();
    }
}
