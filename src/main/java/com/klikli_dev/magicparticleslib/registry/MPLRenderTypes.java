// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.registry;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public final class MPLRenderTypes {
    private static final Method RENDER_TYPE_CREATE = renderTypeCreateMethod();
    private static final Identifier SHADER_ID = Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "core/rift");
    private static final Identifier PORTAL_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/end_portal/end_portal.png");

    private static final RenderPipeline HALO_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "pipeline/rift_halo"))
            .withVertexShader(SHADER_ID)
            .withFragmentShader(SHADER_ID)
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

    private static final RenderPipeline PORTAL_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "pipeline/rift"))
            .withVertexShader(SHADER_ID)
            .withFragmentShader(SHADER_ID)
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build();

    private static final Function<Identifier, RenderType> HALO = Util.memoize(MPLRenderTypes::createHalo);
    private static final Function<Identifier, RenderType> PORTAL = Util.memoize(MPLRenderTypes::createPortal);

    private MPLRenderTypes() {
    }

    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(HALO_PIPELINE);
        event.registerPipeline(PORTAL_PIPELINE);
    }

    public static RenderType halo() {
        return HALO.apply(PORTAL_TEXTURE);
    }

    public static RenderType portal() {
        return PORTAL.apply(PORTAL_TEXTURE);
    }

    private static RenderType createHalo(Identifier texture) {
        RenderSetup state = RenderSetup.builder(HALO_PIPELINE)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .withTexture("Sampler0", texture)
                .createRenderSetup();
        return createRenderType(MagicParticlesLib.MODID + "_rift_halo", state);
    }

    private static RenderType createPortal(Identifier texture) {
        RenderSetup state = RenderSetup.builder(PORTAL_PIPELINE)
                .bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
                .withTexture("Sampler0", texture)
                .sortOnUpload()
                .createRenderSetup();
        return createRenderType(MagicParticlesLib.MODID + "_rift", state);
    }

    private static RenderType createRenderType(String name, RenderSetup state) {
        try {
            return (RenderType) RENDER_TYPE_CREATE.invoke(null, name, state);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to create RenderType '" + name + "'", exception);
        }
    }

    private static Method renderTypeCreateMethod() {
        try {
            Method method = RenderType.class.getDeclaredMethod("create", String.class, RenderSetup.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Failed to access RenderType#create", exception);
        }
    }
}
