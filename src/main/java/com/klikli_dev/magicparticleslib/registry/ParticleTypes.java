// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.registry;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, MagicParticlesLib.MODID);

    public static final Supplier<ParticleType<GlowParticleOptions>> GLOW = PARTICLE_TYPES.register("glow", GlowParticleType::new);
}
