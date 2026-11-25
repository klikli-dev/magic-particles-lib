// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.essentiastream;

import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;

import java.util.ArrayList;

public class EssentiaStreamParticleGroup extends ParticleGroup<EssentiaStreamParticle> {
    public EssentiaStreamParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public EssentiaStreamRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        ArrayList<EssentiaStreamRenderState.Entry> entries = new ArrayList<>(this.particles.size());
        for (EssentiaStreamParticle particle : this.particles) {
            // Streams are culled by their bounds, which cover the whole route, so off-screen streams cost nothing.
            if (!frustum.isVisible(particle.getBoundingBox())) {
                continue;
            }

            ExtrusionMesh mesh = particle.renderData(partialTickTime);
            if (mesh == null || mesh.isEmpty()) {
                continue;
            }

            entries.add(new EssentiaStreamRenderState.Entry(mesh, particle.renderOrigin()));
        }

        return new EssentiaStreamRenderState(entries);
    }
}
