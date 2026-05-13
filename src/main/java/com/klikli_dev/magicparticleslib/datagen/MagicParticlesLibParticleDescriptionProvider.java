// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.datagen;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import com.klikli_dev.magicparticleslib.registry.ParticleTypes;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.data.ParticleDescriptionProvider;

public class MagicParticlesLibParticleDescriptionProvider extends ParticleDescriptionProvider {
    public MagicParticlesLibParticleDescriptionProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addDescriptions() {
        this.spriteSet(ParticleTypes.GLOW.get(), Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "particle_glow"));
    }
}
