// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example.electricarc;

import com.klikli_dev.magicparticleslib.example.CommonTargeting;
import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ElectricArcTestHelper {
    // Max test-targeting distance. Larger values allow longer demonstrations and therefore more samples/sway.
    public static final double RANGE = 16.0D;
    // Inflates the entity search box around the line trace.
    // Higher values make the helper grab nearby entities more aggressively.
    public static final double ENTITY_MARGIN = 1.0D;
    // Default visual width for the spawned test arc.
    public static final float WIDTH = 0.6F;
    // Short lifetime keeps the beam feeling like a quick discharge.
    public static final int LIFETIME = 3;
    // Lavender default color used by the example command.
    public static final int COLOR = 0xB891FF;

    private ElectricArcTestHelper() {
    }

    // Finds a practical target point for the held-item simulation.
    // We raycast blocks first, then prefer entities inside that reachable segment.
    public static ArcTargeting resolve(Player player) {
        return resolve(player, RANGE, ENTITY_MARGIN);
    }

    // Range and entity margin are exposed so other examples can reuse the same target acquisition logic.
    public static ArcTargeting resolve(Player player, double range, double entityMargin) {
        CommonTargeting.Targeting targeting = CommonTargeting.resolve(player, range, entityMargin);
        return new ArcTargeting(targeting.origin(), targeting.target());
    }

    // Creates the example particle options. random.nextInt() is used as the visual seed,
    // so repeated test shots at the same target still get distinct bolt shapes.
    public static ElectricArcParticleOptions createOptions(Vec3 target, RandomSource random) {
        return new ElectricArcParticleOptions(target, COLOR, WIDTH, random.nextInt(), LIFETIME);
    }

    // Lightweight sound accompaniment for the test helper.
    // The pitch randomness makes repeated arcs feel less repetitive.
    public static void playSound(Level level, Player player, RandomSource random) {
        CommonTargeting.playResonate(level, player, random, 0.55F, 1.5F, 0.2F);
    }

    public record ArcTargeting(Vec3 origin, Vec3 target) {
    }
}
