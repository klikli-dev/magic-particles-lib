// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib;

import com.klikli_dev.magicparticleslib.premade.glow.GlowParticleProvider;
import com.klikli_dev.magicparticleslib.registry.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@Mod(value = MagicParticlesLib.MODID, dist = Dist.CLIENT)
public class MagicParticlesLibClient {
    public MagicParticlesLibClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(MagicParticlesLibClient::registerParticleProviders);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleTypes.GLOW.get(), GlowParticleProvider::new);
    }
}
