// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib;

import com.klikli_dev.magicparticleslib.premade.entity.branchingrift.BranchingRiftRenderer;
import com.klikli_dev.magicparticleslib.example.command.SpawnElectricArcClientCommand;
import com.klikli_dev.magicparticleslib.example.command.SpawnGlowTrailProjectileClientCommand;
import com.klikli_dev.magicparticleslib.example.command.SpawnLightningClientCommand;
import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleGroup;
import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleProvider;
import com.klikli_dev.magicparticleslib.premade.entity.rift.RiftRenderer;
import com.klikli_dev.magicparticleslib.premade.projectile.glowtrail.GlowTrailProjectileRenderer;
import com.klikli_dev.magicparticleslib.premade.particle.lightning.LightningParticleGroup;
import com.klikli_dev.magicparticleslib.premade.particle.lightning.LightningParticleProvider;
import com.klikli_dev.magicparticleslib.registry.EntityTypeRegistry;
import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleProvider;
import com.klikli_dev.magicparticleslib.registry.ParticleTypeRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@Mod(value = MagicParticlesLib.MODID, dist = Dist.CLIENT)
public class MagicParticlesLibClient {
    public MagicParticlesLibClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(MagicParticlesLibClient::registerRenderPipelines);
        modEventBus.addListener(MagicParticlesLibClient::registerEntityRenderers);
        modEventBus.addListener(MagicParticlesLibClient::registerParticleProviders);
        modEventBus.addListener(MagicParticlesLibClient::registerParticleGroups);
        NeoForge.EVENT_BUS.addListener(MagicParticlesLibClient::registerClientCommands);
        NeoForge.EVENT_BUS.addListener(SpawnElectricArcClientCommand::onClientTick);
        NeoForge.EVENT_BUS.addListener(SpawnLightningClientCommand::onClientTick);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        RenderTypeRegistry.register(event);
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        SpawnElectricArcClientCommand.register(event.getDispatcher());
        SpawnLightningClientCommand.register(event.getDispatcher());
        SpawnGlowTrailProjectileClientCommand.register(event.getDispatcher());
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypeRegistry.GLOW_TRAIL_PROJECTILE.get(), GlowTrailProjectileRenderer::new);
        event.registerEntityRenderer(EntityTypeRegistry.RIFT.get(), RiftRenderer::new);
        event.registerEntityRenderer(EntityTypeRegistry.BRANCHING_RIFT.get(), BranchingRiftRenderer::new);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleTypeRegistry.GLOW.get(), GlowParticleProvider::new);
        event.registerSpecial(ParticleTypeRegistry.ELECTRIC_ARC.get(), new ElectricArcParticleProvider());
        event.registerSpecial(ParticleTypeRegistry.LIGHTNING.get(), new LightningParticleProvider());
    }

    private static void registerParticleGroups(RegisterParticleGroupsEvent event) {
        event.register(RenderTypeRegistry.ELECTRIC_ARC_GROUP, ElectricArcParticleGroup::new);
        event.register(RenderTypeRegistry.LIGHTNING_GROUP, LightningParticleGroup::new);
    }
}
