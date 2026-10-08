#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sphereworld_planet.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out float viewDistance;

void main() {
    vec3 camera = vec3(CameraBlockPos) - CameraOffset;
    float circumference = SphereWorldPlanet.w;
    float scale = ColorModulator.x;

    vec2 rel = Position.xz * scale - camera.xz;
    rel -= circumference * floor(rel / circumference + 0.5);
    rel += UV0 * scale;
    float dist = length(rel);
    float theta = min(dist / SphereWorldPlanet.x, 3.14159265);
    float vertexRadius = ColorModulator.y + (Position.y - ColorModulator.z);
    float cameraRadius = SphereWorldPlanet.x + camera.y - SphereWorldPlanet.y;
    vec2 dir = dist > 0.0001 ? rel / dist : vec2(1.0, 0.0);
    vec3 local = vec3(dir.x * vertexRadius * sin(theta), vertexRadius * cos(theta) - cameraRadius, dir.y * vertexRadius * sin(theta));
    viewDistance = length(local);
    gl_Position = ProjMat * ModelViewMat * vec4(local, 1.0);
    vertexColor = Color;
}
