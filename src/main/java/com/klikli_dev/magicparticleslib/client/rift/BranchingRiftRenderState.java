// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.client.rift;

import com.klikli_dev.magicparticleslib.premade.rift.BranchingRiftShape;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class BranchingRiftRenderState extends EntityRenderState {
    public BranchingRiftShape shape = BranchingRiftShape.EMPTY;
    public float visualIntensity;
}
