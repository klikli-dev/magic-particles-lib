// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example.lightning;

import com.klikli_dev.magicparticleslib.example.CommonTargeting;
import com.klikli_dev.magicparticleslib.premade.particle.lightning.LightningParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class LightningTestHelper {
    // Match the original test-item feel with a slightly longer range than the electric arc example.
    public static final double RANGE = 23.0D;
    // Slightly stronger entity snap helps the held-fire demo stay readable on moving targets.
    public static final double ENTITY_MARGIN = 1.35D;
    // Upward bias that produces the arced travel silhouette.
    public static final float HEIGHT_GAIN = 0.48F;
    // Logical width passed into the extrusion builder.
    public static final float WIDTH = 0.72F;
    // Short lifetime keeps each individual discharge punchy.
    public static final int LIFETIME = 4;
    // Cyan-white default close to the original lightning tester.
    public static final int COLOR = 0x88FBFF;

    private LightningTestHelper() {
    }

    public static CommonTargeting.Targeting resolve(Player player) {
        return CommonTargeting.resolve(player, RANGE, ENTITY_MARGIN);
    }

    public static LightningParticleOptions createOptions(Vec3 target, RandomSource random) {
        return new LightningParticleOptions(target, COLOR, HEIGHT_GAIN, WIDTH, random.nextInt(), LIFETIME);
    }

    public static void playSound(Level level, Player player, RandomSource random) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.32F, 1.4F + random.nextFloat() * 0.18F);
    }
}
