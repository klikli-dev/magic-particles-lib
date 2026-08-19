// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.auranode;

import java.util.List;

// Captures the world-space quads of one aura node particle for a single frame.
// Strand and core quads carry their own blend mode, so the render state splits
// them into the additive and normal-blend passes.
record AuraNodeRenderData(List<AuraNodeQuad> quads, int light) {
    AuraNodeRenderData {
        quads = List.copyOf(quads);
    }
}