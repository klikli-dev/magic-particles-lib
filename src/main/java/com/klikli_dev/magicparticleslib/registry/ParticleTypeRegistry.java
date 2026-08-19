// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.registry;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import com.klikli_dev.magicparticleslib.premade.particle.auranode.AuraNodeParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.auranode.AuraNodeParticleType;
import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleType;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleType;
import com.klikli_dev.magicparticleslib.premade.particle.lightning.LightningParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.lightning.LightningParticleType;
import com.klikli_dev.magicparticleslib.premade.particle.nitor.NitorParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.nitor.NitorParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ParticleTypeRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, MagicParticlesLib.MODID);

    public static final Supplier<ParticleType<GlowParticleOptions>> GLOW = PARTICLE_TYPES.register("glow", GlowParticleType::new);
    public static final Supplier<ParticleType<ElectricArcParticleOptions>> ELECTRIC_ARC = PARTICLE_TYPES.register("electric_arc", ElectricArcParticleType::new);
    public static final Supplier<ParticleType<LightningParticleOptions>> LIGHTNING = PARTICLE_TYPES.register("lightning", LightningParticleType::new);
    public static final Supplier<ParticleType<NitorParticleOptions>> NITOR = PARTICLE_TYPES.register("nitor", NitorParticleType::new);
    public static final Supplier<ParticleType<AuraNodeParticleOptions>> AURA_NODE = PARTICLE_TYPES.register("aura_node", AuraNodeParticleType::new);
}
