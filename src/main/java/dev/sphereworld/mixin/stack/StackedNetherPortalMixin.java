package dev.sphereworld.mixin.stack;

import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherPortalBlock.class)
abstract class StackedNetherPortalMixin {
    @Shadow
    private TeleportTransition getExitPortal(ServerLevel newLevel, Entity entity, BlockPos portalEntryPos, BlockPos approximateExitPos,
                                             boolean toNether, WorldBorder worldBorder) {
        throw new AssertionError();
    }

    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void sphereworld$verticalShortcut(ServerLevel level, Entity entity, BlockPos portalEntryPos,
                                              CallbackInfoReturnable<TeleportTransition> cir) {
        if (!StackedWorld.is(level)) return;
        StackBand from = StackBand.at(portalEntryPos.getY());
        StackBand to = from == StackBand.NETHER ? StackBand.OVERWORLD : from == StackBand.OVERWORLD ? StackBand.NETHER : null;
        if (to == null) {
            cir.setReturnValue(null);
            return;
        }
        BlockPos exit = StackedWorld.sameNativeHeight(entity.blockPosition(), from, to);
        StackedWorld.PORTAL_BAND.set(to);
        try {
            cir.setReturnValue(getExitPortal(level, entity, portalEntryPos, exit, to == StackBand.NETHER, level.getWorldBorder()));
        } finally {
            StackedWorld.PORTAL_BAND.remove();
        }
    }
}
