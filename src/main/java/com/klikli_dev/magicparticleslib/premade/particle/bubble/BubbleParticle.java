// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.bubble;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.LightCoordsUtil;
import org.jspecify.annotations.NonNull;

/**
 * A bubble-like droplet: it floats up slowly, shrinks to half over its life, and pops in its last ticks.
 * <p>
 * The pop is a short flare in size while the bubble fades out, so it needs no extra frames.
 */
public class BubbleParticle extends SingleQuadParticle {
    // Upward acceleration per tick, and the friction that caps how fast the bubble rises.
    private static final float BUOYANCY = 0.01F;
    private static final float FRICTION = 0.9F;
    // The quad is this many tenths of a block per unit of scale.
    private static final float SIZE_PER_SCALE = 0.1F;
    // Size lost over the whole life, as a fraction of the starting size.
    private static final float SHRINK_FRACTION = 0.5F;
    // The pop covers this many final ticks: it flares up by POP_FLARE and fades out.
    private static final int POP_TICKS = 2;
    private static final float POP_FLARE = 0.5F;

    private final SpriteSet sprites;
    private final float initialSize;
    private final float initialAlpha;

    protected BubbleParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            BubbleParticleOptions options,
            SpriteSet sprites
    ) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = FRICTION;
        this.lifetime = Math.max(1, options.age());
        this.initialSize = options.scale() * SIZE_PER_SCALE;
        this.initialAlpha = options.alpha();
        this.quadSize = this.initialSize;
        this.alpha = this.initialAlpha;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.setColor(options.red(), options.green(), options.blue());
        this.setSpriteFromAge(sprites);
    }

    @Override
    public SingleQuadParticle.@NonNull Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void tick() {
        this.yd += BUOYANCY;
        super.tick();

        float life = (float) this.age / (float) this.lifetime;
        float remaining = this.lifetime - this.age;
        // Zero before the pop, then rising to one on the final tick.
        float pop = remaining <= POP_TICKS ? (POP_TICKS + 1.0F - remaining) / POP_TICKS : 0.0F;
        this.quadSize = this.initialSize * (1.0F - SHRINK_FRACTION * life) * (1.0F + POP_FLARE * pop);
        this.alpha = this.initialAlpha * (1.0F - pop);
        this.setSpriteFromAge(this.sprites);
    }
}
