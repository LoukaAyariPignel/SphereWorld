package dev.sphereworld.wrap;

import dev.sphereworld.mixin.seam.LookControlAccessor;
import dev.sphereworld.mixin.seam.MoveControlAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public final class SeamShift {
    private SeamShift() {
    }

    public static void afterWrap(Entity entity, double dx, double dz) {
        entity.xo += dx;
        entity.zo += dz;
        entity.xOld += dx;
        entity.zOld += dz;
        if (!(entity instanceof Mob mob)) return;
        if (mob.getMoveControl() instanceof MoveControlAccessor move) {
            move.sphereworld$setWantedX(move.sphereworld$wantedX() + dx);
            move.sphereworld$setWantedZ(move.sphereworld$wantedZ() + dz);
        }
        if (mob.getLookControl() instanceof LookControlAccessor look) {
            look.sphereworld$setWantedX(look.sphereworld$wantedX() + dx);
            look.sphereworld$setWantedZ(look.sphereworld$wantedZ() + dz);
        }
        if (mob.getNavigation() != null && !mob.getNavigation().isDone()) {
            mob.getNavigation().recomputePath();
        }
    }
}
