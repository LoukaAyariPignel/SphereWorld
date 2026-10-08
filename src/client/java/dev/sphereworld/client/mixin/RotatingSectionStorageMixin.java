package dev.sphereworld.client.mixin;

import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RotatingSectionStorage.class)
abstract class RotatingSectionStorageMixin<T extends RotatingSectionStorage.Value> {
    @Shadow @Final private int radius;
    @Shadow @Final private int minY;
    @Shadow @Final private int maxY;
    @Shadow private SectionPos centerSectionPos;

    @Shadow
    public abstract @Nullable T getValue(int sectionX, int sectionY, int sectionZ);

    @Inject(method = "repositionCenter", at = @At("HEAD"), cancellable = true)
    private void sphereworld$repositionEnteringColumns(SectionPos center, CallbackInfoReturnable<Boolean> cir) {
        SectionPos old = centerSectionPos;
        if (old.x() == Integer.MIN_VALUE || center.equals(old)) return;
        int size = radius * 2 + 1;
        if (Math.abs(center.x() - old.x()) >= size || Math.abs(center.z() - old.z()) >= size) return;
        centerSectionPos = center;
        for (int x = center.x() - radius; x <= center.x() + radius; x++) {
            boolean enteringX = Math.abs(x - old.x()) > radius;
            for (int z = center.z() - radius; z <= center.z() + radius; z++) {
                if (!enteringX && Math.abs(z - old.z()) <= radius) continue;
                for (int y = minY; y <= maxY; y++) {
                    T value = getValue(x, y, z);
                    long node = SectionPos.asLong(x, y, z);
                    if (value != null && value.getSectionNode() != node) value.setSectionNode(node);
                }
            }
        }
        cir.setReturnValue(true);
    }
}
