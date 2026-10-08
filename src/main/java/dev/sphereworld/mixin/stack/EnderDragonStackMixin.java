package dev.sphereworld.mixin.stack;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.sphereworld.worldgen.stacked.StackedWorld;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnderDragon.class)
public abstract class EnderDragonStackMixin {
    @ModifyExpressionValue(method = "findClosestNode()I", at = @At(value = "CONSTANT", args = "intValue=73"))
    private int sphereworld$nodeFloor(int y) {
        return StackedWorld.endY(((Entity) (Object) this).level(), y);
    }
}
