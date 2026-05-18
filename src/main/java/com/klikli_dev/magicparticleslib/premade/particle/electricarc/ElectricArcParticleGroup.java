// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.electricarc;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;

import java.util.ArrayList;

public class ElectricArcParticleGroup extends ParticleGroup<ElectricArcParticle> {
    public ElectricArcParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ElectricArcRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        ArrayList<ElectricArcRenderData> entries = new ArrayList<>(this.particles.size());
        for (ElectricArcParticle particle : this.particles) {
            ElectricArcRenderData entry = particle.renderData(partialTickTime);
            if (entry == null) {
                continue;
            }

            if (entry.haloMesh().isEmpty() && entry.coreMesh().isEmpty()) {
                continue;
            }

            entries.add(entry);
        }

        return new ElectricArcRenderState(entries);
    }
}
