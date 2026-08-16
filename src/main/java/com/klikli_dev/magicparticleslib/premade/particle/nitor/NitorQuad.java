// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.nitor;

// A single camera-facing quad in world space, positioned and sized by the particle.
// The billboard rotation is applied in the render state where the camera is available.
record NitorQuad(float x, float y, float z, float scale, float u0, float u1, float v0, float v1, int color) {
}