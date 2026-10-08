package dev.sphereworld.mixin.stack;

import dev.sphereworld.worldgen.stacked.StackedAmbience;
import java.util.List;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class StackedAmbienceMixin {
    private StackedAmbienceMixin() {
    }

    @Mixin(EnvironmentAttributeSystem.Builder.class)
    public abstract static class BuilderMixin {
        @Unique
        private List<EnvironmentAttributeSystem> sphereworld$bandSystems = List.of();

        @Inject(method = "addDefaultLayers", at = @At("TAIL"))
        private void sphereworld$bandLayers(Level level, CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> cir) {
            if (StackedAmbience.isStackedType(level)) {
                this.sphereworld$bandSystems = StackedAmbience.addLayers((EnvironmentAttributeSystem.Builder) (Object) this, level);
            }
        }

        @Inject(method = "build", at = @At("RETURN"))
        private void sphereworld$keepBandSystems(CallbackInfoReturnable<EnvironmentAttributeSystem> cir) {
            if (!this.sphereworld$bandSystems.isEmpty()) {
                ((StackedAmbience.Holder) cir.getReturnValue()).sphereworld$setBandSystems(this.sphereworld$bandSystems);
            }
        }
    }

    @Mixin(EnvironmentAttributeSystem.class)
    public abstract static class SystemMixin implements StackedAmbience.Holder {
        @Unique
        private List<EnvironmentAttributeSystem> sphereworld$bandSystems = List.of();

        @Override
        public void sphereworld$setBandSystems(List<EnvironmentAttributeSystem> systems) {
            this.sphereworld$bandSystems = systems;
        }

        @Inject(method = "invalidateTickCache", at = @At("TAIL"))
        private void sphereworld$invalidateBands(CallbackInfo ci) {
            for (EnvironmentAttributeSystem system : this.sphereworld$bandSystems) {
                system.invalidateTickCache();
            }
        }
    }
}
