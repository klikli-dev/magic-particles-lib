// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib;

import com.klikli_dev.magicparticleslib.example.command.FollowProjectileClientCommand;
import com.klikli_dev.magicparticleslib.premade.projectile.FollowProjectileRenderer;
import com.klikli_dev.magicparticleslib.registry.EntityTypes;
import com.klikli_dev.magicparticleslib.premade.glow.GlowParticleProvider;
import com.klikli_dev.magicparticleslib.registry.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@Mod(value = MagicParticlesLib.MODID, dist = Dist.CLIENT)
public class MagicParticlesLibClient {
    public MagicParticlesLibClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(MagicParticlesLibClient::registerEntityRenderers);
        modEventBus.addListener(MagicParticlesLibClient::registerParticleProviders);
        NeoForge.EVENT_BUS.addListener(MagicParticlesLibClient::registerClientCommands);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        FollowProjectileClientCommand.register(event.getDispatcher());
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.FOLLOW_PROJECTILE.get(), FollowProjectileRenderer::new);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleTypes.GLOW.get(), GlowParticleProvider::new);
    }
}
