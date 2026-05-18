// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib;

import com.klikli_dev.magicparticleslib.datagen.MagicParticlesLibParticleDescriptionProvider;
import com.klikli_dev.magicparticleslib.registry.EntityTypeRegistry;
import com.klikli_dev.magicparticleslib.registry.ParticleTypeRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(MagicParticlesLib.MODID)
public class MagicParticlesLib {
    public static final String MODID = "magicparticleslib";

    public MagicParticlesLib(IEventBus modEventBus, ModContainer modContainer) {
        EntityTypeRegistry.ENTITY_TYPES.register(modEventBus);
        ParticleTypeRegistry.PARTICLE_TYPES.register(modEventBus);
        modEventBus.addListener(MagicParticlesLib::gatherData);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(MagicParticlesLibParticleDescriptionProvider::new);
    }
}
