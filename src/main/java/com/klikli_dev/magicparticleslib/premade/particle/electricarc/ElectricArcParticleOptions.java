// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.electricarc;

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

public record ElectricArcParticleOptions(Vec3 target, int color, float width, int seed, int lifetime) implements ParticleOptions {
    public static final int DEFAULT_LIFETIME = 3;
    public static final float DEFAULT_WIDTH = 1.0F;

    public ElectricArcParticleOptions {
        target = target == null ? Vec3.ZERO : target;
        width = Math.max(0.0F, width);
        lifetime = Math.max(1, lifetime);
    }

    public static final MapCodec<ElectricArcParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3.CODEC.fieldOf("target").forGetter(ElectricArcParticleOptions::target),
            Codec.INT.fieldOf("color").forGetter(ElectricArcParticleOptions::color),
            Codec.FLOAT.fieldOf("width").forGetter(ElectricArcParticleOptions::width),
            Codec.INT.fieldOf("seed").forGetter(ElectricArcParticleOptions::seed),
            Codec.INT.fieldOf("lifetime").forGetter(ElectricArcParticleOptions::lifetime)
    ).apply(instance, ElectricArcParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElectricArcParticleOptions> STREAM_CODEC = StreamCodec.composite(
            Vec3.STREAM_CODEC,
            ElectricArcParticleOptions::target,
            ByteBufCodecs.INT,
            ElectricArcParticleOptions::color,
            ByteBufCodecs.FLOAT,
            ElectricArcParticleOptions::width,
            ByteBufCodecs.VAR_INT,
            ElectricArcParticleOptions::seed,
            ByteBufCodecs.VAR_INT,
            ElectricArcParticleOptions::lifetime,
            ElectricArcParticleOptions::new
    );

    public static ElectricArcParticleOptions of(Vec3 target, int color) {
        return new ElectricArcParticleOptions(target, color, DEFAULT_WIDTH, 0, DEFAULT_LIFETIME);
    }

    @Override
    public @NonNull ParticleType<ElectricArcParticleOptions> getType() {
        return ParticleTypeRegistry.ELECTRIC_ARC.get();
    }
}
