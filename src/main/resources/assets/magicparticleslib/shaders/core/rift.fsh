// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

#version 330

#moj_import <fog.glsl>
#moj_import <dynamictransforms.glsl>
#moj_import <globals.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec3 modelViewPosition;

out vec4 fragColor;

const float TAU = 6.28318530717958647692;
const float PI = 3.14159265358979323846;
const int LAYER_COUNT = 16;
const vec3 BASE_COLOR = vec3(0.0412, 0.0338, 0.0589);
const float LAYER_MASK_FADE_START = 0.15;
const float LAYER_MASK_FADE_END = 0.48;
const float LAYER_ZOOM_SCALE = 0.5;
const float LAYER_ZOOM_BIAS = 2.75;
const float LAYER_VERTICAL_SCALE = 0.6;
const float LAYER_SCROLL_SPEED = 0.00006;
const float LAYER_OPACITY_BASE = 0.05;
const float LAYER_OPACITY_SCALE = 0.65;
const float GAME_TIME_TO_DAY_TICKS = 24000.0;

// Builds a rotation matrix from an axis/angle pair so each layer can warp the
// sampled direction differently.
mat3 axisAngleMatrix(vec3 axis, float angle) {
    vec3 unitAxis = normalize(axis);
    float sine = sin(angle);
    float cosine = cos(angle);
    float inverseCosine = 1.0 - cosine;

    return mat3(
            inverseCosine * unitAxis.x * unitAxis.x + cosine,
            inverseCosine * unitAxis.x * unitAxis.y + unitAxis.z * sine,
            inverseCosine * unitAxis.x * unitAxis.z - unitAxis.y * sine,
            inverseCosine * unitAxis.x * unitAxis.y - unitAxis.z * sine,
            inverseCosine * unitAxis.y * unitAxis.y + cosine,
            inverseCosine * unitAxis.y * unitAxis.z + unitAxis.x * sine,
            inverseCosine * unitAxis.x * unitAxis.z + unitAxis.y * sine,
            inverseCosine * unitAxis.y * unitAxis.z - unitAxis.x * sine,
            inverseCosine * unitAxis.z * unitAxis.z + cosine
    );
}

// Converts the fragment position into a normalized direction that stays aligned
// to the camera's yaw/pitch, so the rift texture appears anchored in view space.
vec3 cameraAlignedDirection(vec3 viewSpacePosition) {
    vec3 cameraForward = normalize(-vec3(ModelViewMat[0][2], ModelViewMat[1][2], ModelViewMat[2][2]));
    float yaw = atan(cameraForward.x, cameraForward.z);
    float pitch = asin(clamp(cameraForward.y, -1.0, 1.0));

    mat3 pitchTransform = mat3(
            1.0, 0.0, 0.0,
            0.0, cos(-pitch), sin(-pitch),
            0.0, -sin(-pitch), cos(-pitch)
    );
    mat3 yawTransform = mat3(
            cos(yaw), 0.0, -sin(yaw),
            0.0, 1.0, 0.0,
            sin(yaw), 0.0, cos(yaw)
    );

    return normalize(yawTransform * pitchTransform * normalize(-viewSpacePosition));
}

vec3 hash3(float seed) {
    // Stable pseudo-random values per layer. These constants are hash seeds, so
    // changing them alters layer orientation/color distribution unpredictably.
    vec3 value = vec3(seed, seed + 19.19, seed + 47.47);
    return fract(sin(value * vec3(12.9898, 78.233, 37.719)) * 43758.5453);
}

// Chooses a normalized rotation axis for one layer from its index and random seed.
vec3 layerAxis(float layerNumber, vec3 randomSample) {
    vec3 candidate = vec3(
            sin(layerNumber * 1.109 + randomSample.x * TAU),
            cos(layerNumber * 0.791 + randomSample.y * TAU),
            sin(layerNumber * 0.563 + randomSample.z * TAU) * cos(layerNumber * 0.287)
    );
    return normalize(candidate);
}

// Generates the target color for one layer. The ramp multipliers and divisors set
// how quickly hues vary between layers and across the RGB channels.
vec3 layerColor(float layerNumber, vec3 randomSample) {
    vec3 ramp = vec3(
            mod(layerNumber * 12.0 + floor(randomSample.x * 31.0), 31.0) / 31.0,
            mod(layerNumber * 19.0 + floor(randomSample.y * 37.0), 37.0) / 37.0,
            mod(layerNumber * 10.0 + floor(randomSample.z * 19.0), 19.0) / 19.0
    );
    return vec3(
            0.19 + ramp.x * 0.40,
            0.13 + ramp.y * 0.22,
            0.34 + ramp.z * 0.44
    );
}

// Maps a 3D direction onto the portal texture using spherical coordinates.
vec2 sphericalUv(vec3 direction) {
    return vec2(
            atan(direction.x, direction.z) / TAU + 0.5,
            asin(clamp(direction.y, -1.0, 1.0)) / PI + 0.5
    );
}

// Softly fades out the top and bottom of the spherical projection so the effect
// concentrates around the center band of the rift.
float layerMask(float latitude) {
    return 1.0 - smoothstep(LAYER_MASK_FADE_START, LAYER_MASK_FADE_END, abs(latitude - 0.5));
}

// Builds the final rift color by stacking multiple rotated, zoomed texture samples.
// Each iteration contributes a little more color, with nearer layers carrying more weight.
vec3 accumulateRiftColor(vec3 direction, float time) {
    vec3 accumulatedColor = BASE_COLOR;

    for (int layerIndex = 0; layerIndex < LAYER_COUNT; layerIndex++) {
        float layerNumber = float(layerIndex + 1);
        float remainingLayers = float(LAYER_COUNT - layerIndex);
        vec3 layerRandom = hash3(layerNumber);

        // Rotate the view direction differently for each layer so repeated samples
        // do not visibly line up.
        mat3 layerRotation = axisAngleMatrix(layerAxis(layerNumber, layerRandom), layerRandom.z * TAU);
        vec3 warpedDirection = normalize(layerRotation * direction);
        vec2 uv = sphericalUv(warpedDirection);

        // Higher remainingLayers means broader, more distant-looking texture detail.
        float zoom = remainingLayers * LAYER_ZOOM_SCALE + LAYER_ZOOM_BIAS;
        vec2 layerSampleUv = vec2(
                (uv.x - 0.5) * zoom + 0.5,
                ((uv.y - 0.5) * zoom + 0.5) * LAYER_VERTICAL_SCALE + time * LAYER_SCROLL_SPEED
        );

        // Sample the texture, mask it vertically, then blend toward this layer's color.
        float intensity = texture(Sampler0, layerSampleUv).r;
        float opacity = clamp(intensity * (LAYER_OPACITY_BASE + LAYER_OPACITY_SCALE / remainingLayers) * layerMask(uv.y), 0.0, 1.0);
        accumulatedColor = mix(accumulatedColor, layerColor(layerNumber, layerRandom), opacity);
    }

    return accumulatedColor;
}

void main() {
    // 1) Build a camera-aligned direction for this fragment.
    // 2) Convert normalized game time into day ticks for slow scrolling.
    // 3) Accumulate layered color and tint it with the incoming vertex color.
    vec3 color = accumulateRiftColor(cameraAlignedDirection(modelViewPosition), GameTime * GAME_TIME_TO_DAY_TICKS);
    vec4 shaded = vec4(color, 1.0) * vertexColor;

    // 4) Apply Minecraft fog last so the portal fades consistently with the world.
    fragColor = apply_fog(
            shaded,
            sphericalVertexDistance,
            cylindricalVertexDistance,
            FogEnvironmentalStart,
            FogEnvironmentalEnd,
            FogRenderDistanceStart,
            FogRenderDistanceEnd,
            FogColor
    );
}
