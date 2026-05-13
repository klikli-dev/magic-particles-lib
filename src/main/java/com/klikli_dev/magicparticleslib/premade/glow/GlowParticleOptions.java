// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.glow;

import com.klikli_dev.magicparticleslib.registry.ParticleTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GlowParticleOptions(float red, float green, float blue, boolean disableDepthTest, float size, float alpha, int age) implements ParticleOptions {
    public static final float DEFAULT_SIZE = 0.25F;
    public static final float DEFAULT_ALPHA = 1.0F;
    public static final int DEFAULT_AGE = 36;

    public static final MapCodec<GlowParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(GlowParticleOptions::red),
            Codec.FLOAT.fieldOf("g").forGetter(GlowParticleOptions::green),
            Codec.FLOAT.fieldOf("b").forGetter(GlowParticleOptions::blue),
            Codec.BOOL.fieldOf("disableDepthTest").forGetter(GlowParticleOptions::disableDepthTest),
            Codec.FLOAT.fieldOf("size").forGetter(GlowParticleOptions::size),
            Codec.FLOAT.fieldOf("alpha").forGetter(GlowParticleOptions::alpha),
            Codec.INT.fieldOf("age").forGetter(GlowParticleOptions::age)
    ).apply(instance, GlowParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GlowParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            GlowParticleOptions::red,
            ByteBufCodecs.FLOAT,
            GlowParticleOptions::green,
            ByteBufCodecs.FLOAT,
            GlowParticleOptions::blue,
            ByteBufCodecs.BOOL,
            GlowParticleOptions::disableDepthTest,
            ByteBufCodecs.FLOAT,
            GlowParticleOptions::size,
            ByteBufCodecs.FLOAT,
            GlowParticleOptions::alpha,
            ByteBufCodecs.VAR_INT,
            GlowParticleOptions::age,
            GlowParticleOptions::new
    );

    public GlowParticleOptions(float red, float green, float blue) {
        this(red, green, blue, false);
    }

    public GlowParticleOptions(float red, float green, float blue, boolean disableDepthTest) {
        this(red, green, blue, disableDepthTest, DEFAULT_SIZE, DEFAULT_ALPHA, DEFAULT_AGE);
    }

    public static GlowParticleOptions create(float red, float green, float blue) {
        return new GlowParticleOptions(red, green, blue);
    }

    public static GlowParticleOptions create(float red, float green, float blue, boolean disableDepthTest, float size, float alpha, int age) {
        return new GlowParticleOptions(red, green, blue, disableDepthTest, size, alpha, age);
    }

    @Override
    public ParticleType<GlowParticleOptions> getType() {
        return ParticleTypes.GLOW.get();
    }
}
