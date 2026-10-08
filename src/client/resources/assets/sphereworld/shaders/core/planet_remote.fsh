#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in float viewDistance;

layout(location = 0) out vec4 fragColor;

void main() {
    float haze = clamp(viewDistance / 4000.0, 0.0, 0.35);
    fragColor = vec4(mix(vertexColor.rgb, vec3(0.55, 0.7, 1.0), haze), 1.0);
}
