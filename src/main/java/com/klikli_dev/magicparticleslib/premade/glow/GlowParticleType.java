// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.glow;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class GlowParticleType extends ParticleType<GlowParticleOptions> {
    public GlowParticleType() {
        super(false);
    }

    @Override
    public MapCodec<GlowParticleOptions> codec() {
        return GlowParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, GlowParticleOptions> streamCodec() {
        return GlowParticleOptions.STREAM_CODEC;
    }
}
