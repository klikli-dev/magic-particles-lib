// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.lightning;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;

import java.util.ArrayList;

public class LightningParticleGroup extends ParticleGroup<LightningParticle> {
    public LightningParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public LightningRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        ArrayList<LightningRenderData> entries = new ArrayList<>(this.particles.size());
        for (LightningParticle particle : this.particles) {
            LightningRenderData entry = particle.renderData(partialTickTime);
            if (entry == null) {
                continue;
            }

            if (entry.haloMesh().isEmpty() && entry.coreMesh().isEmpty()) {
                continue;
            }

            entries.add(entry);
        }

        return new LightningRenderState(entries);
    }
}
