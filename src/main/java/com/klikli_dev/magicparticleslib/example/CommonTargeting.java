// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example;

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

public final class CommonTargeting {
    private CommonTargeting() {
    }

    // Finds a practical target point for held-item style examples.
    // Blocks limit the reach, and entities inside that reachable segment can override the block hit.
    public static Targeting resolve(Player player, double range, double entityMargin) {
        // Shift slightly down from eye height so the beam feels closer to a held item than forehead fire.
        Vec3 origin = player.getEyePosition().add(0.0D, -0.5D, 0.0D);
        // The normalized view direction becomes the basis for both block and entity queries.
        Vec3 direction = player.calculateViewVector(player.getXRot(), player.getYRot()).normalize();
        // Maximum fallback endpoint if neither a block nor an entity is hit first.
        Vec3 maxTarget = origin.add(direction.scale(range));

        // Raycast blocks first so walls stop the beam at the first solid obstruction.
        BlockHitResult blockHit = player.level().clip(new ClipContext(origin, maxTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        // Use the hit position when blocked, otherwise keep the full-range endpoint.
        Vec3 blockTarget = blockHit.getType() == HitResult.Type.MISS ? maxTarget : blockHit.getLocation();
        // Entity hits beyond the first block should be ignored, so use the block-clamped distance as the cap.
        double maxDistanceSq = origin.distanceToSqr(blockTarget);

        // Expand the player's box along the aim vector so nearby mobs are easy to acquire while aiming.
        AABB searchBox = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(entityMargin);
        // Prefer any pickable entity intersecting the same reachable segment, except the shooter.
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, origin, blockTarget, searchBox, entity -> EntitySelector.CAN_BE_PICKED.test(entity) && entity != player, maxDistanceSq);
        if (entityHit != null) {
            Entity targetEntity = entityHit.getEntity();
            // Use the entity center so the beam lands in the body rather than at an edge corner.
            return new Targeting(origin, targetEntity.getBoundingBox().getCenter());
        }

        // No entity override: keep the block hit or maximum reach point.
        return new Targeting(origin, blockTarget);
    }

    // Shared simple sound helper for held-beam examples.
    // Higher pitch randomness makes repeated casts sound less mechanical.
    public static void playResonate(Level level, Player player, RandomSource random, float volume, float minPitch, float pitchSpread) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, volume, minPitch + random.nextFloat() * pitchSpread);
    }

    public record Targeting(Vec3 origin, Vec3 target) {
    }
}
