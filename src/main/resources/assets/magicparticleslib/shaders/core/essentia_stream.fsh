#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // V runs along the whole stream and can exceed 1 on long trails or while draining.
    // The texture is tileable, so wrapping with fract() keeps the sampler wrap mode out of the pipeline setup.
    vec2 wrappedUv = fract(texCoord0);
    vec4 sampled = texture(Sampler0, wrappedUv) * vertexColor;
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
