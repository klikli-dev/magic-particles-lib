// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.nitor;

import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NitorParticle extends Particle {
    private static final int BODY_MOTE_COUNT = 6;
    private static final int MIDDLE_MOTE_COUNT = 5;
    private static final int RISING_MOTE_COUNT = 8;
    // Sprite set layout (must match the generated particle description):
    // index 0 is the flame texture, 1 the light core, 2 the dark core.
    private static final int FLAME_SPRITE = 0;
    private static final int LIGHT_CORE_SPRITE = 1;
    private static final int DARK_CORE_SPRITE = 2;
    private static final int LAST_SPRITE = DARK_CORE_SPRITE;

    private final SpriteSet sprites;
    private final float size;
    private final float speed;
    private final float intensity;
    private final NitorCoreType coreType;
    private final float phaseOffset;
    // Particle has no rCol/gCol/bCol (those live on SingleQuadParticle), so store them here.
    private final float red;
    private final float green;
    private final float blue;

    public NitorParticle(ClientLevel level, double x, double y, double z, NitorParticleOptions options, SpriteSet sprites) {
        // Zero motion: the nitor is a stationary, fully procedural effect.
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.hasPhysics = false;
        this.lifetime = Math.max(1, options.lifetime());
        this.size = Mth.clamp(options.size(), 0.05F, 16.0F);
        this.speed = Mth.clamp(options.speed(), 0.05F, 8.0F);
        this.intensity = Mth.clamp(options.intensity(), 0.0F, 4.0F);
        this.coreType = options.core();
        long positionHash = Double.doubleToLongBits(x) * 31L
                + Double.doubleToLongBits(y) * 17L
                + Double.doubleToLongBits(z);
        this.phaseOffset = (float) Math.floorMod(positionHash, 4096L) / 4096.0F * Mth.TWO_PI;
        this.red = ARGB.red(options.color()) / 255.0F;
        this.green = ARGB.green(options.color()) / 255.0F;
        this.blue = ARGB.blue(options.color()) / 255.0F;
    }

    @Override
    public ParticleRenderType getGroup() {
        return RenderTypeRegistry.NITOR_GROUP;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void tick() {
        // Preserve previous position fields even though the particle is stationary.
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.age++;
    }

    // Rebuilds the world-space quads for the current frame. The billboard rotation is applied
    // later in NitorRenderState where the camera orientation is available.
    public @Nullable NitorRenderData renderData(float partialTick) {
        if (!this.isAlive()) {
            return null;
        }

        double time = (this.level.getGameTime() + partialTick) * this.speed;
        float baseX = (float) this.x;
        float baseY = (float) this.y;
        float baseZ = (float) this.z;

        List<NitorQuad> flameQuads = new ArrayList<>(BODY_MOTE_COUNT + MIDDLE_MOTE_COUNT + RISING_MOTE_COUNT);
        List<NitorQuad> coreQuads = new ArrayList<>(1);

        // All flame motes share the single flame texture, the core picks light or dark.
        TextureAtlasSprite flameSprite = this.sprites.get(FLAME_SPRITE, LAST_SPRITE);
        TextureAtlasSprite coreSprite = this.sprites.get(
                this.coreType == NitorCoreType.LIGHT ? LIGHT_CORE_SPRITE : DARK_CORE_SPRITE, LAST_SPRITE);

        float heartbeat = 0.96F + 0.04F * Mth.sin((float) (time * 0.21D + this.phaseOffset));
        int darkCoreColor = ARGB.colorFromFloat(Mth.clamp(0.95F * this.intensity, 0.0F, 1.0F), 1.0F, 1.0F, 1.0F);
        coreQuads.add(this.addQuad(baseX, baseY, baseZ,
                this.size * 0.19F * heartbeat, coreSprite, darkCoreColor));

        for (int index = 0; index < BODY_MOTE_COUNT; index++) {
            float seed = this.phaseOffset + index * 2.73F;
            float pulse = Mth.sin((float) (time * (0.12D + index * 0.006D) + seed));
            float sway = Mth.sin((float) (time * 0.07D + seed * 1.31F));
            float x = baseX + Mth.sin(seed * 1.17F) * this.size * 0.065F
                    + sway * this.size * 0.018F;
            float y = baseY + this.size * (-0.035F + index * 0.022F + pulse * 0.012F);
            float z = baseZ + Mth.cos(seed * 0.83F) * this.size * 0.05F;
            float scale = this.size * (0.4275F + pulse * 0.027F - index * 0.0135F);
            int color = ARGB.colorFromFloat(
                    Mth.clamp(0.42F * this.intensity, 0.0F, 1.0F),
                    this.red,
                    this.green,
                    this.blue);
            flameQuads.add(this.addQuad(x, y, z, scale,
                    flameSprite, color));
        }

        for (int index = 0; index < MIDDLE_MOTE_COUNT; index++) {
            float seed = this.phaseOffset + index * 1.91F + 0.73F;
            float pulse = Mth.sin((float) (time * (0.10D + index * 0.005D) + seed));
            float x = baseX + Mth.sin(seed * 1.29F) * this.size * 0.075F
                    + pulse * this.size * 0.012F;
            float y = baseY + this.size * (0.18F + index * 0.032F + pulse * 0.015F);
            float z = baseZ + Mth.cos(seed * 0.77F) * this.size * 0.05F;
            float scale = this.size * (0.205F - index * 0.011F + pulse * 0.01F);
            int color = ARGB.colorFromFloat(
                    Mth.clamp(0.38F * this.intensity, 0.0F, 1.0F),
                    this.red,
                    this.green,
                    this.blue);
            flameQuads.add(this.addQuad(x, y, z, scale,
                    flameSprite, color));
        }

        for (int index = 0; index < RISING_MOTE_COUNT; index++) {
            float pathSeed = this.phaseOffset + index * 2.41F;
            float progress = Mth.frac((float) (time * 0.058D) + index / (float) RISING_MOTE_COUNT);
            float rise = 1.0F - (float) Math.pow(1.0F - progress, 1.45D);
            float startX = Mth.sin(pathSeed * 1.37F) * this.size * 0.035F;
            float startZ = Mth.cos(pathSeed * 0.91F) * this.size * 0.028F;
            float driftX = Mth.sin(pathSeed * 2.13F) * progress * this.size * 0.05F;
            float driftZ = Mth.cos(pathSeed * 1.73F) * progress * this.size * 0.04F;
            float meander = Mth.sin(pathSeed + progress * 5.3F) - Mth.sin(pathSeed);
            float x = baseX + startX + driftX + meander * this.size * 0.018F;
            float y = baseY + this.size * (0.10F + rise * 0.38F);
            float z = baseZ + startZ + driftZ - meander * this.size * 0.012F;
            float scale = this.size * Mth.lerp(progress, 0.14F, 0.045F);
            float fadeIn = Mth.clamp(progress / 0.08F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((1.0F - progress) / 0.28F, 0.0F, 1.0F);
            int color = ARGB.colorFromFloat(
                    Mth.clamp(0.55F * fadeIn * fadeOut * this.intensity, 0.0F, 1.0F),
                    this.red,
                    this.green,
                    this.blue);
            flameQuads.add(this.addQuad(x, y, z, scale,
                    flameSprite, color));
        }

        return new NitorRenderData(flameQuads, coreQuads, LightCoordsUtil.FULL_BRIGHT);
    }

    private NitorQuad addQuad(float x, float y, float z, float scale, TextureAtlasSprite sprite, int color) {
        return new NitorQuad(x, y, z, scale,
                sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), color);
    }
}