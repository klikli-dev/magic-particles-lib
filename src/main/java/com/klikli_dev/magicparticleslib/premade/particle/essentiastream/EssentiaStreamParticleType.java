// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.essentiastream;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

public class EssentiaStreamParticleType extends ParticleType<EssentiaStreamParticleOptions> {
    public EssentiaStreamParticleType() {
        super(false);
    }

    @Override
    public @NonNull MapCodec<EssentiaStreamParticleOptions> codec() {
        return EssentiaStreamParticleOptions.CODEC;
    }

    @Override
    public @NonNull StreamCodec<? super RegistryFriendlyByteBuf, EssentiaStreamParticleOptions> streamCodec() {
        return EssentiaStreamParticleOptions.STREAM_CODEC;
    }
}
