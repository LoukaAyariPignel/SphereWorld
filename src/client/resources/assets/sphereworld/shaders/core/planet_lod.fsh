#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in float flatDistance;
layout(location = 2) in float viewDistance;
layout(location = 3) in vec2 planetXZ;

layout(location = 0) out vec4 fragColor;

void main() {
    if (flatDistance < SphereWorldPlanet.z - 8.0) {
        discard;
    }

    vec4 lod = TextureMat[0];
    if (lod.z > 0.0) {
        vec2 d = planetXZ - lod.xy;
        d -= SphereWorldPlanet.w * floor(d / SphereWorldPlanet.w + 0.5);
        if (max(abs(d.x), abs(d.y)) < lod.z) {
            discard;
        }
    }
    if (lod.w > 0.0) {
        vec2 d = planetXZ - TextureMat[1].xy;
        d -= SphereWorldPlanet.w * floor(d / SphereWorldPlanet.w + 0.5);
        if (max(abs(d.x), abs(d.y)) > lod.w) {
            discard;
        }
    }

    float haze = clamp(viewDistance / (SphereWorldPlanet.w * 0.45), 0.0, 1.0);
    haze = 0.15 + 0.55 * haze;
    vec3 color = mix(vertexColor.rgb, FogColor.rgb, haze * FogColor.a);
    fragColor = vec4(color, 1.0);
}
