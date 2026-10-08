package dev.sphereworld.mixin.seam;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
abstract class PlayerRangeMixin {
    @ModifyVariable(method = {"isWithinEntityInteractionRange(Lnet/minecraft/world/phys/AABB;D)Z",
            "isWithinAttackRange(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/AABB;D)Z"},
            at = @At("HEAD"), argsOnly = true)
    private AABB sphereworld$nearestBox(AABB box) {
        Player self = (Player) (Object) this;
        PlanetGeometry g = PlanetWrap.server(self.level());
        if (g == null) return box;
        double dx = g.nearestImage(box.getCenter().x, self.getX()) - box.getCenter().x;
        double dz = g.nearestImage(box.getCenter().z, self.getZ()) - box.getCenter().z;
        return dx == 0 && dz == 0 ? box : box.move(dx, 0, dz);
    }

    @ModifyVariable(method = "isWithinBlockInteractionRange", at = @At("HEAD"), argsOnly = true)
    private BlockPos sphereworld$nearestBlock(BlockPos pos) {
        Player self = (Player) (Object) this;
        PlanetGeometry g = PlanetWrap.server(self.level());
        return g == null ? pos : PlanetWrap.nearestImage(g, pos, self.position());
    }
}
