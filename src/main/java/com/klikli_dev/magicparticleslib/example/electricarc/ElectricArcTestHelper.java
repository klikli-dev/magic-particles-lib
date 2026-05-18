// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example.electricarc;

import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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
        Vec3 origin = player.getEyePosition().add(0.0D, -0.5D, 0.0D);
        Vec3 direction = player.calculateViewVector(player.getXRot(), player.getYRot()).normalize();
        Vec3 maxTarget = origin.add(direction.scale(RANGE));

        // Block hit decides the furthest legal target point along the aim direction.
        BlockHitResult blockHit = player.level().clip(new ClipContext(origin, maxTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 blockTarget = blockHit.getType() == HitResult.Type.MISS ? maxTarget : blockHit.getLocation();
        double maxDistanceSq = origin.distanceToSqr(blockTarget);

        // Then look for entities inside the same reach volume so mobs/players can override the block target.
        AABB searchBox = player.getBoundingBox().expandTowards(direction.scale(RANGE)).inflate(ENTITY_MARGIN);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, origin, blockTarget, searchBox, entity -> EntitySelector.CAN_BE_PICKED.test(entity) && entity != player, maxDistanceSq);
        if (entityHit != null) {
            Entity targetEntity = entityHit.getEntity();
            return new ArcTargeting(origin, targetEntity.getBoundingBox().getCenter());
        }

        return new ArcTargeting(origin, blockTarget);
    }

    // Creates the example particle options. random.nextInt() is used as the visual seed,
    // so repeated test shots at the same target still get distinct bolt shapes.
    public static ElectricArcParticleOptions createOptions(Vec3 target, RandomSource random) {
        return new ElectricArcParticleOptions(target, COLOR, WIDTH, random.nextInt(), LIFETIME);
    }

    // Lightweight sound accompaniment for the test helper.
    // The pitch randomness makes repeated arcs feel less repetitive.
    public static void playSound(Level level, Player player, RandomSource random) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.55F, 1.5F + random.nextFloat() * 0.2F);
    }

    public record ArcTargeting(Vec3 origin, Vec3 target) {
    }
}
