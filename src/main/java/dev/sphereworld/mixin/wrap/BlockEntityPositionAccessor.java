package dev.sphereworld.mixin.wrap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockEntity.class)
public interface BlockEntityPositionAccessor {
    @Mutable
    @Accessor("worldPosition")
    void sphereworld$setWorldPosition(BlockPos pos);
}
