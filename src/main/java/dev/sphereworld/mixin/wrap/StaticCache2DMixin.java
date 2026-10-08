package dev.sphereworld.mixin.wrap;

import dev.sphereworld.wrap.PeriodicCache;
import net.minecraft.util.StaticCache2D;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StaticCache2D.class)
abstract class StaticCache2DMixin implements PeriodicCache {
    @Shadow @Final private int minX;
    @Shadow @Final private int minZ;
    @Shadow @Final private int sizeX;
    @Shadow @Final private int sizeZ;

    @Unique private int sphereworld$period;

    @Override
    public int sphereworld$period() {
        return sphereworld$period;
    }

    @Override
    public void sphereworld$setPeriod(int period) {
        sphereworld$period = period;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sphereworld$capturePeriod(CallbackInfo ci) {
        sphereworld$period = PeriodicCache.CONSTRUCTING.get();
    }

    @Inject(method = "map", at = @At("RETURN"))
    private void sphereworld$inheritPeriod(CallbackInfoReturnable<StaticCache2D<?>> cir) {
        ((PeriodicCache) cir.getReturnValue()).sphereworld$setPeriod(sphereworld$period);
    }

    @ModifyVariable(method = {"get", "contains"}, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sphereworld$imageX(int x) {
        return sphereworld$image(x, minX, sizeX);
    }

    @ModifyVariable(method = {"get", "contains"}, at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int sphereworld$imageZ(int z) {
        return sphereworld$image(z, minZ, sizeZ);
    }

    @Unique
    private int sphereworld$image(int value, int min, int size) {
        int period = sphereworld$period;
        if (period <= 0 || (value >= min && value < min + size)) return value;
        int center = min + size / 2;
        return center + Math.floorMod(value - center + period / 2, period) - period / 2;
    }
}
