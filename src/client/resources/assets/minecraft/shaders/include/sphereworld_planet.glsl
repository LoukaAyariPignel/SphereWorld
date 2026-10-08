#ifndef SPHEREWORLD_PLANET_GLSL
#define SPHEREWORLD_PLANET_GLSL

#include <minecraft:globals.glsl>

vec3 sphereworld_curve(vec3 pos, mat4 proj) {
    if (SphereWorldPlanet.z < 0.5 || proj[2][3] == 0.0) {
        return pos;
    }
    float dist = length(pos.xz);
    if (dist < 0.0001) {
        return pos;
    }
    float radius = SphereWorldPlanet.x;
    float cameraY = float(CameraBlockPos.y) - CameraOffset.y;
    float cameraRadius = max(radius + cameraY - SphereWorldPlanet.y, 1.0);
    float effective = mix(cameraRadius, radius, smoothstep(0.0, 128.0, length(pos)));
    float theta = min(dist / effective, 3.14159265);
    float vertexRadius = cameraRadius + pos.y;
    float s = sin(theta);
    float halfSin = sin(theta * 0.5);
    float horizontal = vertexRadius * s;

    float vertical = pos.y * cos(theta) - 2.0 * cameraRadius * halfSin * halfSin;
    vec2 dir = pos.xz / dist;
    return vec3(dir.x * horizontal, vertical, dir.y * horizontal);
}

#endif
