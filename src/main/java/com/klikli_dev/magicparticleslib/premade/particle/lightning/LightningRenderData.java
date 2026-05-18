// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.lightning;

import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;

record LightningRenderData(ExtrusionMesh haloMesh, ExtrusionMesh coreMesh, int light) {
}
