// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.bubble;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

public class BubbleParticleType extends ParticleType<BubbleParticleOptions> {
    public BubbleParticleType() {
        super(false);
    }

    @Override
    public @NonNull MapCodec<BubbleParticleOptions> codec() {
        return BubbleParticleOptions.CODEC;
    }

    @Override
    public @NonNull StreamCodec<? super RegistryFriendlyByteBuf, BubbleParticleOptions> streamCodec() {
        return BubbleParticleOptions.STREAM_CODEC;
    }
}
