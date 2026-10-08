package dev.sphereworld.client;

import dev.sphereworld.client.ClientPlanet;
import java.util.Set;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

public final class PacketPositions {
    private PacketPositions() {
    }

    public static PositionMoveRotation map(PositionMoveRotation change, Set<Relative> relatives) {
        Vec3 position = change.position();
        double x = relatives.contains(Relative.X) ? position.x : ClientPlanet.x(position.x);
        double z = relatives.contains(Relative.Z) ? position.z : ClientPlanet.z(position.z);
        if (x == position.x && z == position.z) return change;
        return new PositionMoveRotation(new Vec3(x, position.y, z), change.deltaMovement(), change.yRot(), change.xRot());
    }
}
