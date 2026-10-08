#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sphereworld_planet.glsl>

layout(location = 0) in vec3 Position;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(sphereworld_curve(Position, ProjMat), 1.0);
}
