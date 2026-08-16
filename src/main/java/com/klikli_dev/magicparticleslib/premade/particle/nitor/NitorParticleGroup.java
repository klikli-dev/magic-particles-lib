// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.nitor;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;

import java.util.ArrayList;

public class NitorParticleGroup extends ParticleGroup<NitorParticle> {
    public NitorParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public NitorRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        ArrayList<NitorRenderData> entries = new ArrayList<>(this.particles.size());
        for (NitorParticle particle : this.particles) {
            NitorRenderData entry = particle.renderData(partialTickTime);
            if (entry == null) {
                continue;
            }

            if (entry.flameQuads().isEmpty() && entry.coreQuads().isEmpty()) {
                continue;
            }

            entries.add(entry);
        }

        return new NitorRenderState(entries);
    }
}