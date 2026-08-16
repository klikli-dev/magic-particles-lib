// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.nitor;

import com.klikli_dev.magicparticleslib.registry.ParticleTypeRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

public record NitorParticleOptions(int color, float size, float speed, float intensity, int lifetime, NitorCoreType core) implements ParticleOptions {
    public static final float DEFAULT_SIZE = 1.0F;
    public static final float DEFAULT_SPEED = 1.0F;
    public static final float DEFAULT_INTENSITY = 1.0F;
    public static final int DEFAULT_LIFETIME = 101;

    public NitorParticleOptions {
        size = Math.max(0.0F, size);
        speed = Math.max(0.0F, speed);
        intensity = Math.max(0.0F, intensity);
        lifetime = Math.max(1, lifetime);
    }

    public static final MapCodec<NitorParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("color").forGetter(NitorParticleOptions::color),
            Codec.FLOAT.fieldOf("size").forGetter(NitorParticleOptions::size),
            Codec.FLOAT.fieldOf("speed").forGetter(NitorParticleOptions::speed),
            Codec.FLOAT.fieldOf("intensity").forGetter(NitorParticleOptions::intensity),
            Codec.INT.fieldOf("lifetime").forGetter(NitorParticleOptions::lifetime),
            NitorCoreType.CODEC.fieldOf("core").forGetter(NitorParticleOptions::core)
    ).apply(instance, NitorParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, NitorParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            NitorParticleOptions::color,
            ByteBufCodecs.FLOAT,
            NitorParticleOptions::size,
            ByteBufCodecs.FLOAT,
            NitorParticleOptions::speed,
            ByteBufCodecs.FLOAT,
            NitorParticleOptions::intensity,
            ByteBufCodecs.VAR_INT,
            NitorParticleOptions::lifetime,
            ByteBufCodecs.STRING_UTF8.map(NitorCoreType::fromName, NitorCoreType::getSerializedName),
            NitorParticleOptions::core,
            NitorParticleOptions::new
    );

    public static NitorParticleOptions of(int color) {
        return new NitorParticleOptions(color, DEFAULT_SIZE, DEFAULT_SPEED, DEFAULT_INTENSITY, DEFAULT_LIFETIME, NitorCoreType.DARK);
    }

    public static NitorParticleOptions of(int color, NitorCoreType core) {
        return new NitorParticleOptions(color, DEFAULT_SIZE, DEFAULT_SPEED, DEFAULT_INTENSITY, DEFAULT_LIFETIME, core);
    }

    @Override
    public @NonNull ParticleType<NitorParticleOptions> getType() {
        return ParticleTypeRegistry.NITOR.get();
    }
}