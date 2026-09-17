#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec4 lightMapColor;
layout(location = 4) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 wrappedUv = vec2(fract(texCoord0.x), texCoord0.y);
    vec4 sampled = texture(Sampler0, wrappedUv) * vertexColor * lightMapColor;
    fragColor = apply_fog(
            sampled,
            sphericalVertexDistance,
            cylindricalVertexDistance,
            FogEnvironmentalStart,
            FogEnvironmentalEnd,
            FogRenderDistanceStart,
            FogRenderDistanceEnd,
            FogColor
    );
}
