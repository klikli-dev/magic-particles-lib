// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.auranode;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;

import java.util.ArrayList;

public class AuraNodeParticleGroup extends ParticleGroup<AuraNodeParticle> {
    public AuraNodeParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public AuraNodeRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        ArrayList<AuraNodeRenderData> entries = new ArrayList<>(this.particles.size());
        for (AuraNodeParticle particle : this.particles) {
            AuraNodeRenderData entry = particle.renderData(camera, partialTickTime);
            if (entry == null) {
                continue;
            }

            if (entry.quads().isEmpty()) {
                continue;
            }

            entries.add(entry);
        }

        return new AuraNodeRenderState(entries);
    }
}