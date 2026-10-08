package dev.sphereworld.mixin.stack;

import dev.sphereworld.worldgen.stacked.StackBand;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class StackedDragonFightMixin {
    @Shadow
    private @Nullable EnderDragonFight dragonFight;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sphereworld$endLayerFight(CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (this.dragonFight != null || !StackedWorld.is(self)) return;
        this.dragonFight = self.getDataStorage().computeIfAbsent(EnderDragonFight.TYPE);
        this.dragonFight.init(self, self.getSeed(), new BlockPos(0, StackBand.END.offset(), 0));
    }
}
