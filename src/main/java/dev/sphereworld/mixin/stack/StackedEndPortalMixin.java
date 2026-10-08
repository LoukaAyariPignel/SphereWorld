package dev.sphereworld.mixin.stack;

import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndPortalBlock.class)
abstract class StackedEndPortalMixin {
    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void sphereworld$verticalShortcut(ServerLevel level, Entity entity, BlockPos portalEntryPos,
                                              CallbackInfoReturnable<TeleportTransition> cir) {
        if (!StackedWorld.is(level)) return;
        if (StackBand.at(portalEntryPos.getY()) == StackBand.END) {
            if (entity instanceof ServerPlayer player) {
                cir.setReturnValue(player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
            } else {
                BlockPos spawn = level.getRespawnData().pos();
                cir.setReturnValue(new TeleportTransition(level, Vec3.atBottomCenterOf(entity.adjustSpawnLocation(level, spawn)), Vec3.ZERO,
                        0.0F, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION), TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)));
            }
            return;
        }
        BlockPos platform = StackBand.END.toWorld(ServerLevel.END_SPAWN_POINT);
        EndPlatformFeature.createEndPlatform(level, platform.below(), true);
        Vec3 arrival = Vec3.atBottomCenterOf(platform);
        if (entity instanceof ServerPlayer) arrival = arrival.subtract(0.0, 1.0, 0.0);
        cir.setReturnValue(new TeleportTransition(level, arrival, Vec3.ZERO, Direction.WEST.toYRot(), 0.0F,
                Relative.union(Relative.DELTA, Set.of(Relative.X_ROT)), TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)));
    }
}
