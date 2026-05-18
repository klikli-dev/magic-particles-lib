// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.lightning;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class LightningShapeGenerator {
    // Collapse extremely short beams into a tiny fallback segment.
    // Raising this makes tiny target distances stop animating sooner.
    private static final double DEGENERATE_DIRECTION_EPSILON = 1.0E-8D;
    // Downward acceleration used by the one-shot pseudo-projectile path.
    // Raising this flattens the arch and pulls the midpoint lower.
    private static final double STEP_GRAVITY = 0.112D;
    // Amount of random per-step displacement added to the ballistic samples.
    // Raising it makes the bolt look rougher and more chaotic.
    private static final double STEP_NOISE = 0.24D;
    // Hard cap to keep pathological long beams from generating too many samples.
    // Raising this increases detail on long bolts at higher mesh cost.
    private static final int MAX_POINTS = 48;
    // Endpoint width reduction so the caps read closer to the legacy pointed tips.
    // Raising this makes the start/end look chunkier.
    private static final float END_WIDTH_SCALE = 0.4F;

    private LightningShapeGenerator() {
    }

    static LightningShape build(Vec3 start, Vec3 end, float heightGain, float width, int seed) {
        // Measure the overall beam vector so we can derive the travel distance and arch.
        Vec3 delta = end.subtract(start);
        // Length is used for the degenerate fallback and bounds heuristics.
        double distance = delta.length();
        // Very short beams cannot support a meaningful curve, so return a tiny fallback.
        if (distance <= DEGENERATE_DIRECTION_EPSILON) {
            return new LightningShape(List.of(start, end.add(0.0D, 0.001D, 0.0D)), List.of(width, width), List.of(0.0F, 0.0F));
        }

        // Derive an initial launch velocity that arches toward the target before falling back down.
        Vec3 velocity = initialVelocity(delta, Math.max(0.12D, heightGain));
        // Stop when the remaining distance is small enough that adding another noisy sample would overshoot.
        double stopDistanceSq = velocity.lengthSqr();
        // The cursor walks the relative ballistic path from the beam origin toward the target.
        Vec3 cursor = Vec3.ZERO;
        // Deterministic random source keeps each bolt shape stable for its entire lifetime.
        RandomSource random = RandomSource.create(seed ^ 0x7A4F13C5L);
        ArrayList<Vec3> points = new ArrayList<>();
        ArrayList<Float> widths = new ArrayList<>();
        ArrayList<Float> twists = new ArrayList<>();
        // Always start exactly at the source attachment point.
        points.add(start);
        widths.add(Math.max(0.0F, width * END_WIDTH_SCALE));
        twists.add(0.0F);

        int count = 0;
        while (distanceSquared(delta, cursor) > stopDistanceSq && count < MAX_POINTS) {
            // Advance one ballistic step along the clean path.
            Vec3 next = cursor.add(velocity);
            // Persist the clean cursor so the stop test follows the underlying arc rather than the jitter.
            cursor = next;
            // Add symmetric random jitter per axis to recreate the legacy kinked lightning silhouette.
            next = next.add(
                    signedRandom(random) * STEP_NOISE,
                    signedRandom(random) * STEP_NOISE,
                    signedRandom(random) * STEP_NOISE
            );
            // Store the noisy sample in world space.
            points.add(start.add(next));
            // Keep the strip width nearly constant like the original crossed quads.
            widths.add(width);
            // Ribbon contours should not twist; the crossed look comes from using two rotated contours instead.
            twists.add(0.0F);
            // Gravity is applied a little more softly each step so the arc descends over the full path.
            velocity = velocity.subtract(0.0D, STEP_GRAVITY / 1.85D, 0.0D);
            count++;
        }

        // Always finish exactly on the target so the bolt visibly lands where the caller requested.
        points.add(end);
        widths.add(Math.max(0.0F, width * END_WIDTH_SCALE));
        twists.add(0.0F);

        return new LightningShape(points, widths, twists);
    }

    static double maxLateralDisplacement(double distance, float heightGain, float width) {
        // Reserve room for both the upward arch and the per-step jitter so culling does not clip the beam.
        double archAllowance = Math.max(0.15D, heightGain) * 2.2D;
        double noiseAllowance = STEP_NOISE * 1.5D;
        return Math.max(width * 0.25D, Math.max(distance * 0.08D, archAllowance + noiseAllowance));
    }

    private static Vec3 initialVelocity(Vec3 delta, double heightGain) {
        // Horizontal projection decides how far the bolt must travel before it can land.
        Vec3 horizontal = new Vec3(delta.x(), 0.0D, delta.z());
        // Horizontal length is used to derive the forward speed.
        double horizontalDistance = horizontal.length();
        // Crest height sits above the higher of the two endpoints so the arc visibly bows upward.
        double crestHeight = Math.max(heightGain, delta.y() + heightGain);
        // Initial upward speed needed to reach that crest under the configured gravity.
        double initialYVelocity = Math.sqrt(Math.max(0.001D, crestHeight * STEP_GRAVITY));
        // Solve for the horizontal speed from the rise/fall slope needed to reach the target height.
        double parabolaFactor = -horizontalDistance * horizontalDistance / Math.max(0.001D, crestHeight * 4.0D);
        double slope = (-horizontalDistance / (2.0D * parabolaFactor)) - Math.sqrt(Math.max(0.001D, horizontalDistance * horizontalDistance - 4.0D * parabolaFactor * -delta.y())) / (2.0D * parabolaFactor);
        double horizontalSpeed = initialYVelocity / Math.max(0.001D, slope);
        // Horizontal direction becomes the forward axis of the ballistic travel.
        Vec3 horizontalDirection = horizontalDistance <= DEGENERATE_DIRECTION_EPSILON ? Vec3.ZERO : horizontal.scale(1.0D / horizontalDistance);
        // Combine horizontal travel and upward lift into the starting velocity.
        return horizontalDirection.scale(horizontalSpeed).add(0.0D, initialYVelocity, 0.0D);
    }

    private static double distanceSquared(Vec3 to, Vec3 from) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double dz = to.z() - from.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private static double signedRandom(RandomSource random) {
        // Difference-of-uniforms produces a centered distribution with frequent small offsets and occasional larger kicks.
        return random.nextDouble() - random.nextDouble();
    }
}
