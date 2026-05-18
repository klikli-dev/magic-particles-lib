// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.lightning;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

public class LightningParticleType extends ParticleType<LightningParticleOptions> {
    public LightningParticleType() {
        super(false);
    }

    @Override
    public @NonNull MapCodec<LightningParticleOptions> codec() {
        return LightningParticleOptions.CODEC;
    }

    @Override
    public @NonNull StreamCodec<? super RegistryFriendlyByteBuf, LightningParticleOptions> streamCodec() {
        return LightningParticleOptions.STREAM_CODEC;
    }
}
