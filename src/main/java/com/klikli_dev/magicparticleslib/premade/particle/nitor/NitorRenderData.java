// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.nitor;

import java.util.List;

// Captures the world-space quads of one nitor particle for a single frame.
// Flame quads render with the additive pipeline, core quads with the translucent one.
record NitorRenderData(List<NitorQuad> flameQuads, List<NitorQuad> coreQuads, int light) {
}