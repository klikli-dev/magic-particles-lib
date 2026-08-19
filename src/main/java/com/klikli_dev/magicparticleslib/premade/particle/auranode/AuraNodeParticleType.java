// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.auranode;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

public class AuraNodeParticleType extends ParticleType<AuraNodeParticleOptions> {
    public AuraNodeParticleType() {
        super(false);
    }

    @Override
    public @NonNull MapCodec<AuraNodeParticleOptions> codec() {
        return AuraNodeParticleOptions.CODEC;
    }

    @Override
    public @NonNull StreamCodec<? super RegistryFriendlyByteBuf, AuraNodeParticleOptions> streamCodec() {
        return AuraNodeParticleOptions.STREAM_CODEC;
    }
}