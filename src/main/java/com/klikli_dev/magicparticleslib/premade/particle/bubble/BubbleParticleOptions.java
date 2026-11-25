// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.bubble;

import com.klikli_dev.magicparticleslib.registry.ParticleTypeRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.NonNull;

/**
 * A small translucent bubble droplet.
 *
 * @param color packed ARGB color; the alpha is the particle's starting alpha
 * @param scale size factor. The quad is {@code scale * 0.1} blocks across before it shrinks
 * @param age   lifetime in ticks
 */
public record BubbleParticleOptions(int color, float scale, int age) implements ParticleOptions {
    public static final float DEFAULT_SCALE = 0.5F;
    public static final int DEFAULT_AGE = 24;
    // Half transparent, so droplets read as liquid bubbles rather than solid specks.
    public static final int DEFAULT_ALPHA = 128;

    public BubbleParticleOptions {
        scale = Math.max(0.0F, scale);
        age = Math.max(1, age);
    }

    public static final MapCodec<BubbleParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.ARGB_COLOR_CODEC.fieldOf("color").forGetter(BubbleParticleOptions::color),
            Codec.FLOAT.fieldOf("scale").forGetter(BubbleParticleOptions::scale),
            Codec.INT.fieldOf("age").forGetter(BubbleParticleOptions::age)
    ).apply(instance, BubbleParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BubbleParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            BubbleParticleOptions::color,
            ByteBufCodecs.FLOAT,
            BubbleParticleOptions::scale,
            ByteBufCodecs.VAR_INT,
            BubbleParticleOptions::age,
            BubbleParticleOptions::new
    );

    /**
     * Creates a bubble tinted with an RGB colour (the upper 8 bits are ignored) at the default alpha.
     */
    public static BubbleParticleOptions of(int rgb, float scale, int age) {
        return new BubbleParticleOptions(ARGB.color(DEFAULT_ALPHA, rgb), scale, age);
    }

    public float red() {
        return ARGB.redFloat(this.color);
    }

    public float green() {
        return ARGB.greenFloat(this.color);
    }

    public float blue() {
        return ARGB.blueFloat(this.color);
    }

    public float alpha() {
        return ARGB.alphaFloat(this.color);
    }

    @Override
    public @NonNull ParticleType<BubbleParticleOptions> getType() {
        return ParticleTypeRegistry.BUBBLE.get();
    }
}
