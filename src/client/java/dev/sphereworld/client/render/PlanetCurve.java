package dev.sphereworld.client.render;

import dev.sphereworld.planet.PlanetGeometry;
import org.joml.Vector3f;

public final class PlanetCurve {
    private PlanetCurve() {
    }

    public static float cameraRadius(PlanetGeometry g, double cameraY) {
        return (float) Math.max(g.radius() + cameraY - g.surfaceY(), 1.0);
    }

    public static Vector3f curve(PlanetGeometry g, float cameraRadius, Vector3f pos) {
        float dist = (float) Math.sqrt(pos.x * pos.x + pos.z * pos.z);
        if (dist < 1.0E-4F) return pos;
        float radius = (float) g.radius();
        float t = Math.clamp(pos.length() / 128.0F, 0.0F, 1.0F);
        float smooth = t * t * (3.0F - 2.0F * t);
        float effective = cameraRadius + (radius - cameraRadius) * smooth;
        float theta = Math.min(dist / effective, (float) Math.PI);
        float vertexRadius = cameraRadius + pos.y;
        float halfSin = (float) Math.sin(theta * 0.5);
        float horizontal = vertexRadius * (float) Math.sin(theta);
        float vertical = pos.y * (float) Math.cos(theta) - 2.0F * cameraRadius * halfSin * halfSin;
        return pos.set(pos.x / dist * horizontal, vertical, pos.z / dist * horizontal);
    }
}
