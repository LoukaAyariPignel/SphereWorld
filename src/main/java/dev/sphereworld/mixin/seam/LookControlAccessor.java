package dev.sphereworld.mixin.seam;

import net.minecraft.world.entity.ai.control.LookControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LookControl.class)
public interface LookControlAccessor {
    @Accessor("wantedX") double sphereworld$wantedX();
    @Accessor("wantedX") void sphereworld$setWantedX(double value);
    @Accessor("wantedZ") double sphereworld$wantedZ();
    @Accessor("wantedZ") void sphereworld$setWantedZ(double value);
}
