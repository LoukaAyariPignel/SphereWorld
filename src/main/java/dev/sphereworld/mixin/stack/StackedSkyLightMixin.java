package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sphereworld.worldgen.stacked.StackBand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSkyLightSources.class)
public abstract class StackedSkyLightMixin {
    @Unique
    private boolean sphereworld$stacked;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sphereworld$detect(LevelHeightAccessor level, CallbackInfo ci) {
        this.sphereworld$stacked = level.getMinY() == StackBand.WORLD_MIN_Y && level.getHeight() == StackBand.WORLD_HEIGHT;
    }

    @ModifyVariable(method = "findLowestSourceY", at = @At("HEAD"), argsOnly = true, index = 2)
    private int sphereworld$startBelowEnd(int topSectionIndex, ChunkAccess chunk) {
        if (!this.sphereworld$stacked) return topSectionIndex;
        return Math.min(topSectionIndex, chunk.getSectionIndex(StackBand.OVERWORLD.worldMaxY()));
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void sphereworld$ignoreEndLayer(BlockGetter level, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (this.sphereworld$stacked && y > StackBand.OVERWORLD.worldMaxY()) cir.setReturnValue(false);
    }

    @WrapOperation(method = {"update", "findLowestSourceBelow"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState sphereworld$endLayerIsOpen(BlockGetter level, BlockPos pos, Operation<BlockState> original) {
        if (this.sphereworld$stacked && pos.getY() > StackBand.OVERWORLD.worldMaxY()) return Blocks.AIR.defaultBlockState();
        return original.call(level, pos);
    }
}
