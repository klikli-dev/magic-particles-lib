// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib;

import com.klikli_dev.magicparticleslib.client.rift.BranchingRiftRenderer;
import com.klikli_dev.magicparticleslib.client.rift.RiftRenderTypes;
import com.klikli_dev.magicparticleslib.client.rift.RiftRenderer;
import com.klikli_dev.magicparticleslib.example.command.SpawnGlowTrailProjectileClientCommand;
import com.klikli_dev.magicparticleslib.premade.projectile.glowtrail.GlowTrailProjectileRenderer;
import com.klikli_dev.magicparticleslib.registry.EntityTypes;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleProvider;
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
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@Mod(value = MagicParticlesLib.MODID, dist = Dist.CLIENT)
public class MagicParticlesLibClient {
    public MagicParticlesLibClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(MagicParticlesLibClient::registerRenderPipelines);
        modEventBus.addListener(MagicParticlesLibClient::registerEntityRenderers);
        modEventBus.addListener(MagicParticlesLibClient::registerParticleProviders);
        NeoForge.EVENT_BUS.addListener(MagicParticlesLibClient::registerClientCommands);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        RiftRenderTypes.register(event);
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        SpawnGlowTrailProjectileClientCommand.register(event.getDispatcher());
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.GLOW_TRAIL_PROJECTILE.get(), GlowTrailProjectileRenderer::new);
        event.registerEntityRenderer(EntityTypes.RIFT.get(), RiftRenderer::new);
        event.registerEntityRenderer(EntityTypes.BRANCHING_RIFT.get(), BranchingRiftRenderer::new);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleTypes.GLOW.get(), GlowParticleProvider::new);
    }
}
