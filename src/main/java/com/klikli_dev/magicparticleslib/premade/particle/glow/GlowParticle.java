// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.glow;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.jspecify.annotations.NonNull;

public class GlowParticle extends SingleQuadParticle {
    private static final SingleQuadParticle.Layer TRANSLUCENT_NO_DEPTH = createNoDepthLayer();

    private final SpriteSet sprites;
    private final float initialScale;
    private final float initialAlpha;
    private final boolean disableDepthTest;
    private final boolean shrinkWithAge;

    protected GlowParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            GlowParticleOptions options,
            SpriteSet sprites
    ) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        this.hasPhysics = false;
        this.sprites = sprites;
        this.disableDepthTest = options.disableDepthTest();
        this.shrinkWithAge = options.shrinkWithAge();
        this.initialScale = options.size();
        this.initialAlpha = options.alpha();
        this.lifetime = Math.max(1, options.age());
        this.quadSize = this.initialScale;
        this.alpha = this.initialAlpha;
        this.xd = xSpeed * 2.0F;
        this.yd = ySpeed * 2.0F;
        this.zd = zSpeed * 2.0F;
        this.setColor(options.red(), options.green(), options.blue());
        this.setSpriteFromAge(sprites);
    }

    private static SingleQuadParticle.Layer createNoDepthLayer() {
        RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "particle_translucent_no_depth"))
                .withVertexShader("core/particle")
                .withFragmentShader("core/particle")
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .build();

        return new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, pipeline);
    }

    @Override
    public SingleQuadParticle.@NonNull Layer getLayer() {
        return this.disableDepthTest ? TRANSLUCENT_NO_DEPTH : SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.random.nextInt(6) == 0) {
            this.age++;
        }

        float lifeCoeff = (float) this.age / (float) this.lifetime;
        this.quadSize = this.shrinkWithAge ? this.initialScale * (1.0F - lifeCoeff) : this.initialScale;
        this.alpha = this.initialAlpha * (1.0F - lifeCoeff);
        this.oRoll = this.roll;
        this.roll += 1.0F;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public boolean isAlive() {
        return this.age < this.lifetime;
    }
}
