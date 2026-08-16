// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.registry;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.function.Function;

public final class RenderTypeRegistry {
    public static final ParticleRenderType ELECTRIC_ARC_GROUP = new ParticleRenderType(MagicParticlesLib.MODID + ":electric_arc", "electric_arc");
    public static final ParticleRenderType LIGHTNING_GROUP = new ParticleRenderType(MagicParticlesLib.MODID + ":lightning", "lightning");
    public static final ParticleRenderType NITOR_GROUP = new ParticleRenderType(MagicParticlesLib.MODID + ":nitor", "nitor");
    private static final Identifier RIFT_SHADER_ID = Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "core/rift");
    private static final Identifier ELECTRIC_ARC_SHADER_ID = Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "core/electric_arc");
    private static final Identifier PORTAL_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/end_portal/end_portal.png");
    private static final Identifier ELECTRIC_ARC_TEXTURE = Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "textures/effect/electric_arc.png");
    private static final Identifier LIGHTNING_TEXTURE = Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "textures/effect/lightning_core.png");
    private static final BlendFunction ALPHA_WEIGHTED_ADDITIVE = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE);

    private static final RenderPipeline RIFT_HALO_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "pipeline/rift_halo"))
            .withVertexShader(RIFT_SHADER_ID)
            .withFragmentShader(RIFT_SHADER_ID)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    private static final RenderPipeline RIFT_PORTAL_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "pipeline/rift"))
            .withVertexShader(RIFT_SHADER_ID)
            .withFragmentShader(RIFT_SHADER_ID)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .build();

    private static final RenderPipeline ELECTRIC_ARC_HALO_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "pipeline/electric_arc_halo"))
            .withVertexShader(ELECTRIC_ARC_SHADER_ID)
            .withFragmentShader(ELECTRIC_ARC_SHADER_ID)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(ALPHA_WEIGHTED_ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    private static final RenderPipeline ELECTRIC_ARC_CORE_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "pipeline/electric_arc_core"))
            .withVertexShader(ELECTRIC_ARC_SHADER_ID)
            .withFragmentShader(ELECTRIC_ARC_SHADER_ID)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(ALPHA_WEIGHTED_ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    private static final RenderPipeline LIGHTNING_HALO_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "pipeline/lightning_halo"))
            .withVertexShader(ELECTRIC_ARC_SHADER_ID)
            .withFragmentShader(ELECTRIC_ARC_SHADER_ID)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(ALPHA_WEIGHTED_ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    private static final RenderPipeline LIGHTNING_CORE_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "pipeline/lightning_core"))
            .withVertexShader(ELECTRIC_ARC_SHADER_ID)
            .withFragmentShader(ELECTRIC_ARC_SHADER_ID)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(ALPHA_WEIGHTED_ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    private static final RenderPipeline NITOR_FLAME_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "pipeline/nitor_flame"))
            .withVertexShader(Identifier.withDefaultNamespace("core/particle"))
            .withFragmentShader(Identifier.withDefaultNamespace("core/particle"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(ALPHA_WEIGHTED_ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN, false))
            .build();

    private static final RenderPipeline NITOR_CORE_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "pipeline/nitor_core"))
            .withVertexShader(Identifier.withDefaultNamespace("core/particle"))
            .withFragmentShader(Identifier.withDefaultNamespace("core/particle"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN, false))
            .build();

    private static final Function<Identifier, RenderType> RIFT_HALO = Util.memoize(RenderTypeRegistry::createRiftHalo);
    private static final Function<Identifier, RenderType> RIFT_PORTAL = Util.memoize(RenderTypeRegistry::createRiftPortal);
    private static final Function<Identifier, RenderType> ELECTRIC_ARC_HALO = Util.memoize(RenderTypeRegistry::createElectricArcHalo);
    private static final Function<Identifier, RenderType> ELECTRIC_ARC_CORE = Util.memoize(RenderTypeRegistry::createElectricArcCore);
    private static final Function<Identifier, RenderType> LIGHTNING_HALO = Util.memoize(RenderTypeRegistry::createLightningHalo);
    private static final Function<Identifier, RenderType> LIGHTNING_CORE = Util.memoize(RenderTypeRegistry::createLightningCore);
    private static final Function<Identifier, RenderType> NITOR_FLAME = Util.memoize(RenderTypeRegistry::createNitorFlame);
    private static final Function<Identifier, RenderType> NITOR_CORE = Util.memoize(RenderTypeRegistry::createNitorCore);

    private RenderTypeRegistry() {
    }

    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(RIFT_HALO_PIPELINE);
        event.registerPipeline(RIFT_PORTAL_PIPELINE);
        event.registerPipeline(ELECTRIC_ARC_HALO_PIPELINE);
        event.registerPipeline(ELECTRIC_ARC_CORE_PIPELINE);
        event.registerPipeline(LIGHTNING_HALO_PIPELINE);
        event.registerPipeline(LIGHTNING_CORE_PIPELINE);
        event.registerPipeline(NITOR_FLAME_PIPELINE);
        event.registerPipeline(NITOR_CORE_PIPELINE);
    }

    public static RenderType riftHalo() {
        return RIFT_HALO.apply(PORTAL_TEXTURE);
    }

    public static RenderType riftPortal() {
        return RIFT_PORTAL.apply(PORTAL_TEXTURE);
    }

    public static RenderType electricArcHalo() {
        return ELECTRIC_ARC_HALO.apply(ELECTRIC_ARC_TEXTURE);
    }

    public static RenderType electricArcCore() {
        return ELECTRIC_ARC_CORE.apply(ELECTRIC_ARC_TEXTURE);
    }

    public static RenderType lightningHalo() {
        return LIGHTNING_HALO.apply(LIGHTNING_TEXTURE);
    }

    public static RenderType lightningCore() {
        return LIGHTNING_CORE.apply(LIGHTNING_TEXTURE);
    }

    public static RenderType nitorFlame() {
        return NITOR_FLAME.apply(TextureAtlas.LOCATION_PARTICLES);
    }

    public static RenderType nitorCore() {
        return NITOR_CORE.apply(TextureAtlas.LOCATION_PARTICLES);
    }

    private static RenderType createRiftHalo(Identifier texture) {
        RenderSetup state = RenderSetup.builder(RIFT_HALO_PIPELINE)
                .withTexture("Sampler0", texture)
                .createRenderSetup();
        return RenderType.create(MagicParticlesLib.MODID + "_rift_halo", state);
    }

    private static RenderType createRiftPortal(Identifier texture) {
        RenderSetup state = RenderSetup.builder(RIFT_PORTAL_PIPELINE)
                .withTexture("Sampler0", texture)
                .createRenderSetup();
        return RenderType.create(MagicParticlesLib.MODID + "_rift", state);
    }

    private static RenderType createElectricArcHalo(Identifier texture) {
        RenderSetup state = RenderSetup.builder(ELECTRIC_ARC_HALO_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .createRenderSetup();
        return RenderType.create(MagicParticlesLib.MODID + "_electric_arc_halo", state);
    }

    private static RenderType createElectricArcCore(Identifier texture) {
        RenderSetup state = RenderSetup.builder(ELECTRIC_ARC_CORE_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .createRenderSetup();
        return RenderType.create(MagicParticlesLib.MODID + "_electric_arc_core", state);
    }

    private static RenderType createLightningHalo(Identifier texture) {
        RenderSetup state = RenderSetup.builder(LIGHTNING_HALO_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .createRenderSetup();
        return RenderType.create(MagicParticlesLib.MODID + "_lightning_halo", state);
    }

    private static RenderType createLightningCore(Identifier texture) {
        RenderSetup state = RenderSetup.builder(LIGHTNING_CORE_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .createRenderSetup();
        return RenderType.create(MagicParticlesLib.MODID + "_lightning_core", state);
    }

    private static RenderType createNitorFlame(Identifier texture) {
        RenderSetup state = RenderSetup.builder(NITOR_FLAME_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .createRenderSetup();
        return RenderType.create(MagicParticlesLib.MODID + "_nitor_flame", state);
    }

    private static RenderType createNitorCore(Identifier texture) {
        RenderSetup state = RenderSetup.builder(NITOR_CORE_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .createRenderSetup();
        return RenderType.create(MagicParticlesLib.MODID + "_nitor_core", state);
    }
}
