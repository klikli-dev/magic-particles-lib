// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.electricarc;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.util.RandomSource;

public class ElectricArcParticleProvider implements ParticleProvider<ElectricArcParticleOptions> {
    @Override
    public Particle createParticle(ElectricArcParticleOptions options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
        return new ElectricArcParticle(level, x, y, z, options);
    }
}
