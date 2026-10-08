package dev.sphereworld.mixin.wrap;

import dev.sphereworld.planet.PlanetGeometry;
import dev.sphereworld.planet.Planets;
import dev.sphereworld.wrap.PlanetWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerMixin {
    @Shadow public ServerPlayer player;

    @ModifyVariable(method = "handlePlayerPositionChange", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double sphereworld$moveX(double x) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? x : g.nearestImage(x, player.getX());
    }

    @ModifyVariable(method = "handlePlayerPositionChange", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double sphereworld$moveZ(double z) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? z : g.nearestImage(z, player.getZ());
    }

    @ModifyVariable(method = "handleMoveVehicle", at = @At(value = "STORE"), ordinal = 0)
    private Vec3 sphereworld$vehicleTarget(Vec3 target) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? target : PlanetWrap.nearestImage(g, target, player.getRootVehicle().position());
    }

    @Redirect(method = "handleUseItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundUseItemOnPacket;hitResult()Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult sphereworld$useItemOn(ServerboundUseItemOnPacket packet) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? packet.hitResult() : PlanetWrap.nearestImage(g, packet.hitResult(), player.position());
    }

    @Redirect(method = "handlePlayerAction", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;getPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos sphereworld$playerAction(ServerboundPlayerActionPacket packet) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? packet.getPos() : PlanetWrap.nearestImage(g, packet.getPos(), player.position());
    }

    @Shadow private double firstGoodX;
    @Shadow private double firstGoodZ;
    @Shadow private double lastGoodX;
    @Shadow private double lastGoodZ;
    @Shadow private double vehicleFirstGoodX;
    @Shadow private double vehicleFirstGoodZ;
    @Shadow private double vehicleLastGoodX;
    @Shadow private double vehicleLastGoodZ;

    @Inject(method = "handlePlayerPositionChange", at = @At("HEAD"))
    private void sphereworld$goodPositionsNearPlayer(CallbackInfo ci) {
        PlanetGeometry g = Planets.of(player.level());
        if (g == null) return;
        firstGoodX = g.nearestImage(firstGoodX, player.getX());
        firstGoodZ = g.nearestImage(firstGoodZ, player.getZ());
        lastGoodX = g.nearestImage(lastGoodX, player.getX());
        lastGoodZ = g.nearestImage(lastGoodZ, player.getZ());
    }

    @ModifyVariable(method = "handlePlayerPositionChange", index = 11, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private double sphereworld$targetXAfterMove(double targetX) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? targetX : g.nearestImage(targetX, player.getX());
    }

    @ModifyVariable(method = "handlePlayerPositionChange", index = 15, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private double sphereworld$targetZAfterMove(double targetZ) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? targetZ : g.nearestImage(targetZ, player.getZ());
    }

    @ModifyVariable(method = "handlePlayerPositionChange", index = 19, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private double sphereworld$startXAfterMove(double startX) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? startX : g.nearestImage(startX, player.getX());
    }

    @ModifyVariable(method = "handlePlayerPositionChange", index = 23, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private double sphereworld$startZAfterMove(double startZ) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? startZ : g.nearestImage(startZ, player.getZ());
    }

    @Inject(method = "handleMoveVehicle", at = @At("HEAD"))
    private void sphereworld$vehicleGoodPositionsNearVehicle(CallbackInfo ci) {
        PlanetGeometry g = Planets.of(player.level());
        if (g == null) return;
        var vehicle = player.getRootVehicle();
        vehicleFirstGoodX = g.nearestImage(vehicleFirstGoodX, vehicle.getX());
        vehicleFirstGoodZ = g.nearestImage(vehicleFirstGoodZ, vehicle.getZ());
        vehicleLastGoodX = g.nearestImage(vehicleLastGoodX, vehicle.getX());
        vehicleLastGoodZ = g.nearestImage(vehicleLastGoodZ, vehicle.getZ());
    }

    @ModifyVariable(method = "handleMoveVehicle", index = 12, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private double sphereworld$vehicleTargetXAfterMove(double targetX) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? targetX : g.nearestImage(targetX, player.getRootVehicle().getX());
    }

    @ModifyVariable(method = "handleMoveVehicle", index = 16, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private double sphereworld$vehicleTargetZAfterMove(double targetZ) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? targetZ : g.nearestImage(targetZ, player.getRootVehicle().getZ());
    }

    @ModifyVariable(method = "handleMoveVehicle", index = 6, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private double sphereworld$vehicleOldXAfterMove(double oldX) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? oldX : g.nearestImage(oldX, player.getRootVehicle().getX());
    }

    @ModifyVariable(method = "handleMoveVehicle", index = 10, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private double sphereworld$vehicleOldZAfterMove(double oldZ) {
        PlanetGeometry g = Planets.of(player.level());
        return g == null ? oldZ : g.nearestImage(oldZ, player.getRootVehicle().getZ());
    }
}
