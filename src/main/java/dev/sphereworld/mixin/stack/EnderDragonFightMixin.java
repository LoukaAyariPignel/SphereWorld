package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mutable;
import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import java.util.function.Predicate;

@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin {
    @Shadow
    private BlockPos origin;

    @Shadow
    @Mutable
    private Predicate<Entity> validPlayer;

    @Inject(method = "init", at = @At("TAIL"))
    private void sphereworld$endLayerPlayers(ServerLevel level, long seed, BlockPos origin, CallbackInfo ci) {
        if (StackedWorld.is(level)) {
            this.validPlayer = this.validPlayer.and(entity -> entity.getY() >= StackBand.END.worldMinY());
        }
    }

    @ModifyExpressionValue(method = "spawnNewGateway()V", at = @At(value = "CONSTANT", args = "intValue=75"))
    private int sphereworld$gatewayHeight(int y) {
        return y + this.origin.getY();
    }

    @ModifyExpressionValue(method = "spawnExitPortal", at = @At(value = "CONSTANT", args = "intValue=63"))
    private int sphereworld$exitPortalFloor(int y) {
        return y + this.origin.getY();
    }
}
