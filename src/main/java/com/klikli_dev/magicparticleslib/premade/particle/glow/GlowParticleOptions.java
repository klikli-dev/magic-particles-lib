// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.glow;

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

public record GlowParticleOptions(int color, boolean disableDepthTest, boolean shrinkWithAge, float size, int age) implements ParticleOptions {
    public static final float DEFAULT_SIZE = 0.25F;
    public static final int DEFAULT_AGE = 36;
    public static final boolean DEFAULT_SHRINK_WITH_AGE = true;

    public GlowParticleOptions {
        size = Math.max(0.0F, size);
        age = Math.max(1, age);
    }

    public static final MapCodec<GlowParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.ARGB_COLOR_CODEC.fieldOf("color").forGetter(GlowParticleOptions::color),
            Codec.BOOL.fieldOf("disableDepthTest").forGetter(GlowParticleOptions::disableDepthTest),
            Codec.BOOL.fieldOf("shrinkWithAge").forGetter(GlowParticleOptions::shrinkWithAge),
            Codec.FLOAT.fieldOf("size").forGetter(GlowParticleOptions::size),
            Codec.INT.fieldOf("age").forGetter(GlowParticleOptions::age)
    ).apply(instance, GlowParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GlowParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            GlowParticleOptions::color,
            ByteBufCodecs.BOOL,
            GlowParticleOptions::disableDepthTest,
            ByteBufCodecs.BOOL,
            GlowParticleOptions::shrinkWithAge,
            ByteBufCodecs.FLOAT,
            GlowParticleOptions::size,
            ByteBufCodecs.VAR_INT,
            GlowParticleOptions::age,
            GlowParticleOptions::new
    );

    public static GlowParticleOptions of(int color) {
        return new GlowParticleOptions(color, false, DEFAULT_SHRINK_WITH_AGE, DEFAULT_SIZE, DEFAULT_AGE);
    }

    public GlowParticleOptions color(int color) {
        return new GlowParticleOptions(color, this.disableDepthTest, this.shrinkWithAge, this.size, this.age);
    }

    public GlowParticleOptions disableDepthTest(boolean disableDepthTest) {
        return new GlowParticleOptions(this.color, disableDepthTest, this.shrinkWithAge, this.size, this.age);
    }

    public GlowParticleOptions shrinkWithAge(boolean shrinkWithAge) {
        return new GlowParticleOptions(this.color, this.disableDepthTest, shrinkWithAge, this.size, this.age);
    }

    public GlowParticleOptions size(float size) {
        return new GlowParticleOptions(this.color, this.disableDepthTest, this.shrinkWithAge, size, this.age);
    }

    public GlowParticleOptions age(int age) {
        return new GlowParticleOptions(this.color, this.disableDepthTest, this.shrinkWithAge, this.size, age);
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
    public @NonNull ParticleType<GlowParticleOptions> getType() {
        return ParticleTypeRegistry.GLOW.get();
    }
}
