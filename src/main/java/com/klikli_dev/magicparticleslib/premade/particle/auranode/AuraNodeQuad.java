// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.auranode;

// A single camera-facing quad in world space, positioned and sized by the particle.
// The billboard rotation is applied in the render state where the camera is available;
// the per-layer `rotation` spins the quad around the billboard's view axis before that.
// `additive` selects the additive or the normal-blend render pass for the layer.
record AuraNodeQuad(float x, float y, float z, float edge, float u0, float u1, float v0, float v1, int color, float rotation, boolean additive) {
}