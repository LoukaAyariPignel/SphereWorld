#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sphereworld_planet.glsl>
#include <minecraft:sample_lightmap.glsl>

uniform sampler2D Sampler2;

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out float flatDistance;
layout(location = 2) out float viewDistance;
layout(location = 3) out vec2 planetXZ;

void main() {
    vec3 camera = vec3(CameraBlockPos) - CameraOffset;
    float circumference = SphereWorldPlanet.w;

    vec2 rel = Position.xz - camera.xz;
    rel -= circumference * floor(rel / circumference + 0.5);
    vec3 pos = vec3(rel.x + UV0.x, Position.y - camera.y, rel.y + UV0.y);
    planetXZ = Position.xz + UV0;

    flatDistance = fract(SphereWorldPlanet.z) > 0.25 ? length(pos.xz) : length(pos);
    vec3 curved = sphereworld_curve(pos, ProjMat);
    viewDistance = length(curved);
    gl_Position = ProjMat * ModelViewMat * vec4(curved, 1.0);

    vertexColor = Color * ColorModulator * sample_lightmap(Sampler2, ivec2(0, 240));
}
