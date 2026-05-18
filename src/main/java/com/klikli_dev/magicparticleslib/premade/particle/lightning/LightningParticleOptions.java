// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.lightning;

import com.klikli_dev.magicparticleslib.registry.ParticleTypeRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public record LightningParticleOptions(Vec3 target, int color, float heightGain, float width, int seed, int lifetime) implements ParticleOptions {
    public static final float DEFAULT_HEIGHT_GAIN = 0.5F;
    public static final float DEFAULT_WIDTH = 0.75F;
    public static final int DEFAULT_LIFETIME = 4;

    public LightningParticleOptions {
        target = target == null ? Vec3.ZERO : target;
        heightGain = Math.max(0.0F, heightGain);
        width = Math.max(0.0F, width);
        lifetime = Math.max(1, lifetime);
    }

    public static final MapCodec<LightningParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3.CODEC.fieldOf("target").forGetter(LightningParticleOptions::target),
            Codec.INT.fieldOf("color").forGetter(LightningParticleOptions::color),
            Codec.FLOAT.fieldOf("height_gain").forGetter(LightningParticleOptions::heightGain),
            Codec.FLOAT.fieldOf("width").forGetter(LightningParticleOptions::width),
            Codec.INT.fieldOf("seed").forGetter(LightningParticleOptions::seed),
            Codec.INT.fieldOf("lifetime").forGetter(LightningParticleOptions::lifetime)
    ).apply(instance, LightningParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LightningParticleOptions> STREAM_CODEC = StreamCodec.composite(
            Vec3.STREAM_CODEC,
            LightningParticleOptions::target,
            ByteBufCodecs.INT,
            LightningParticleOptions::color,
            ByteBufCodecs.FLOAT,
            LightningParticleOptions::heightGain,
            ByteBufCodecs.FLOAT,
            LightningParticleOptions::width,
            ByteBufCodecs.VAR_INT,
            LightningParticleOptions::seed,
            ByteBufCodecs.VAR_INT,
            LightningParticleOptions::lifetime,
            LightningParticleOptions::new
    );

    public static LightningParticleOptions of(Vec3 target, int color) {
        return new LightningParticleOptions(target, color, DEFAULT_HEIGHT_GAIN, DEFAULT_WIDTH, 0, DEFAULT_LIFETIME);
    }

    @Override
    public @NonNull ParticleType<LightningParticleOptions> getType() {
        return ParticleTypeRegistry.LIGHTNING.get();
    }
}
