// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.nitor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

public class NitorParticleType extends ParticleType<NitorParticleOptions> {
    public NitorParticleType() {
        super(false);
    }

    @Override
    public @NonNull MapCodec<NitorParticleOptions> codec() {
        return NitorParticleOptions.CODEC;
    }

    @Override
    public @NonNull StreamCodec<? super RegistryFriendlyByteBuf, NitorParticleOptions> streamCodec() {
        return NitorParticleOptions.STREAM_CODEC;
    }
}