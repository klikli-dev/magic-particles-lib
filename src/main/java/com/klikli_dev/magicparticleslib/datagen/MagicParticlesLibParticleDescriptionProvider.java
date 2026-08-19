// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.datagen;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import com.klikli_dev.magicparticleslib.premade.particle.auranode.AuraNodePreset;
import com.klikli_dev.magicparticleslib.premade.particle.auranode.AuraNodePresets;
import com.klikli_dev.magicparticleslib.registry.ParticleTypeRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.data.ParticleDescriptionProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MagicParticlesLibParticleDescriptionProvider extends ParticleDescriptionProvider {
    public MagicParticlesLibParticleDescriptionProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addDescriptions() {
        this.spriteSet(ParticleTypeRegistry.GLOW.get(), Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "particle_glow"));
        this.spriteSet(
                ParticleTypeRegistry.NITOR.get(),
                Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "nitor_flame"),
                Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "nitor_core"),
                Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "nitor_core_dark")
        );

        List<Identifier> auraNodeSprites = new ArrayList<>();
        Set<Identifier> addedStrips = new HashSet<>();
        for (AuraNodePreset preset : AuraNodePresets.registry()) {
            this.addStrip(auraNodeSprites, addedStrips, preset.texture());
            this.addStrip(auraNodeSprites, addedStrips, preset.strandTexture());
        }
        this.spriteSet(ParticleTypeRegistry.AURA_NODE.get(), auraNodeSprites);
    }

    private void addStrip(List<Identifier> sprites, Set<Identifier> addedStrips, Identifier strip) {
        if (!addedStrips.add(strip)) {
            return;
        }
        for (int frame = 0; frame < AuraNodePreset.FRAMES_PER_STRIP; frame++) {
            sprites.add(AuraNodePreset.frameTexture(strip, frame));
        }
    }
}
