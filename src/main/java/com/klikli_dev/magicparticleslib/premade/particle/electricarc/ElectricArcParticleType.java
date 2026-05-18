// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.electricarc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

public class ElectricArcParticleType extends ParticleType<ElectricArcParticleOptions> {
    public ElectricArcParticleType() {
        super(false);
    }

    @Override
    public @NonNull MapCodec<ElectricArcParticleOptions> codec() {
        return ElectricArcParticleOptions.CODEC;
    }

    @Override
    public @NonNull StreamCodec<? super RegistryFriendlyByteBuf, ElectricArcParticleOptions> streamCodec() {
        return ElectricArcParticleOptions.STREAM_CODEC;
    }
}
