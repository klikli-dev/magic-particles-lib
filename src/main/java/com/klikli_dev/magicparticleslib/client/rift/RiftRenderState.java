// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.client.rift;

import com.klikli_dev.magicparticleslib.premade.rift.RiftShape;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class RiftRenderState extends EntityRenderState {
    public RiftShape shape = RiftShape.EMPTY;
    public float visualIntensity;
}
