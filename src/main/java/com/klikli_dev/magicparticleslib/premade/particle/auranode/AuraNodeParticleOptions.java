// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.auranode;

import com.klikli_dev.magicparticleslib.registry.ParticleTypeRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.NonNull;

/**
 * Spawn parameters for an aura node particle. The wire format carries only the
 * preset id; all visual parameters are resolved client-side through the
 * {@link AuraNodePresets} registry, so preset contents can evolve freely
 * without breaking compatibility.
 */
public record AuraNodeParticleOptions(Identifier presetId, int color, float scale, int lifetime) implements ParticleOptions {
    public static final Identifier DEFAULT_PRESET_ID = AuraNodePresets.NORMAL.getId();
    public static final float DEFAULT_SCALE = 1.0F;
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;
    public static final int DEFAULT_LIFETIME = 72000;

    public static final MapCodec<AuraNodeParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("preset").forGetter(AuraNodeParticleOptions::presetId),
            ExtraCodecs.ARGB_COLOR_CODEC.fieldOf("color").forGetter(AuraNodeParticleOptions::color),
            Codec.FLOAT.fieldOf("scale").forGetter(AuraNodeParticleOptions::scale),
            Codec.INT.fieldOf("lifetime").forGetter(AuraNodeParticleOptions::lifetime)
    ).apply(instance, AuraNodeParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuraNodeParticleOptions> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            AuraNodeParticleOptions::presetId,
            ByteBufCodecs.INT,
            AuraNodeParticleOptions::color,
            ByteBufCodecs.FLOAT,
            AuraNodeParticleOptions::scale,
            ByteBufCodecs.VAR_INT,
            AuraNodeParticleOptions::lifetime,
            AuraNodeParticleOptions::new
    );

    public AuraNodeParticleOptions {
        scale = Math.max(0.05F, scale);
        lifetime = Math.max(1, lifetime);
    }

    public static AuraNodeParticleOptions of(AuraNodePreset preset) {
        return new AuraNodeParticleOptions(presetId(preset), DEFAULT_COLOR, DEFAULT_SCALE, DEFAULT_LIFETIME);
    }

    public static AuraNodeParticleOptions of(AuraNodePreset preset, int color, float scale, int lifetime) {
        return new AuraNodeParticleOptions(presetId(preset), color, scale, lifetime);
    }

    private static Identifier presetId(AuraNodePreset preset) {
        return AuraNodePresets.registry().getKey(preset);
    }

    @Override
    public @NonNull ParticleType<AuraNodeParticleOptions> getType() {
        return ParticleTypeRegistry.AURA_NODE.get();
    }
}