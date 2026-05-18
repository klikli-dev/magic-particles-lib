// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.electricarc;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class ElectricArcShapeGenerator {
    // Treat extremely short arcs as degenerate so we can skip all frame/noise math.
    // Raising this makes very short bolts collapse to the fallback sooner.
    // Lowering it allows more tiny arcs to keep their full wobble logic.
    private static final double DEGENERATE_DIRECTION_EPSILON = 1.0E-8D;
    // Motion now keys almost entirely off age, because the original visual grew by time rather than
    // by distance. Keeping the distance terms at zero makes short and long arcs wobble with the same
    // base intensity, which is much closer to the original effect.
    private static final double BASE_SWAY = 0.0D;
    private static final double DISTANCE_SWAY = 0.0D;
    private static final double TIME_SWAY = 0.1D;

    private ElectricArcShapeGenerator() {
    }

    /**
     * Builds the animated centerline that both the halo and core meshes follow.
     *
     * The arc is still built through the rewritten pipeline, but the tuning below intentionally
     * matches the original visual behavior: point density follows distance * PI, motion grows with
     * age, the silhouette is jagged instead of fluid, and width variation uses abrupt pinches.
     */
    static ElectricArcShape build(Vec3 start, Vec3 end, float width, int seed, int age, float partialTickTime) {
        // Vector from start to end. This gives us both direction and total beam length.
        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        if (distance <= DEGENERATE_DIRECTION_EPSILON) {
            // Degenerate case: create a nearly-zero-length segment with a tiny vertical offset
            // so the renderer still has two distinct points to extrude along.
            return new ElectricArcShape(List.of(start, end.add(0.0D, 0.001D, 0.0D)), List.of(width, width));
        }

        // Fractional animation time. Using partialTickTime keeps the motion smooth between ticks.
        double time = age + partialTickTime;
        // Match the original segmentation feel: roughly one point per block * PI.
        int pointCount = Math.max(2, (int) (distance * Math.PI));
        // Match the original time-driven amplitude growth.
        double amplitude = animatedAmplitude(distance, time);
        // Use a deterministic seed-derived drift so each arc keeps a stable phase offset.
        double drift = unitValue(seed ^ 0x68BC21EB) * 50.0D * Math.PI;
        ArrayList<Vec3> points = new ArrayList<>(pointCount);
        ArrayList<Float> widths = new ArrayList<>(pointCount);

        for (int index = 0; index < pointCount; index++) {
            // Normalized position from start (0) to end (1).
            double t = index / (double) (pointCount - 1);
            // Straight-line location before we add any arc deformation.
            Vec3 anchor = start.add(delta.scale(t));
            if (index == 0 || index == pointCount - 1) {
                // Keep the endpoints fixed so the bolt attaches exactly to the chosen positions.
                points.add(anchor);
                widths.add(width);
                continue;
            }

            // Recreate the old per-axis sine stepping. Using different divisors per axis makes the
            // path kink irregularly instead of flowing as a single ribbon.
            double step = index * distance * Math.PI / pointCount + drift;
            double dx = Math.sin(step / 4.0D) * amplitude;
            double dy = Math.sin(step / 3.0D) * amplitude;
            double dz = Math.sin(step / 2.0D) * amplitude;

            // Tiny per-sample jitter restores the original crackly noise between adjacent segments.
            dx += signedNoise(seed ^ 0x3C6EF372, index * 3) * 0.1D;
            dy += signedNoise(seed ^ 0xDAA66D2B, index * 3 + 1) * 0.1D;
            dz += signedNoise(seed ^ 0x78DDE6E5, index * 3 + 2) * 0.1D;

            Vec3 point = anchor.add(dx, dy, dz);
            points.add(point);

            // Abrupt occasional pinches match the original thickness behavior much better than
            // smooth width pulsing.
            float widthScale = signedNoise(seed ^ 0x1B873593, index) > 0.5D ? (1.0F - age * 0.25F) : 1.0F;
            widths.add(Math.max(0.0F, width * widthScale));
        }

        return new ElectricArcShape(points, widths);
    }

    // Match the old bounds behavior closely: scale padding mostly from beam length, not from the
    // finer details of the new generator.
    static double maxLateralDisplacement(double distance, float width) {
        return Math.max(distance / 10.0D, 0.5D) * 1.75D;
    }

    // Match the old time-driven amplitude growth exactly.
    private static double animatedAmplitude(double distance, double time) {
        return BASE_SWAY + distance * DISTANCE_SWAY + Math.max(0.0D, time) * TIME_SWAY;
    }

    // Retained for the rewritten structure even though the current matching pass does not rely on it.
    private static LocalFrame frame(Vec3 delta) {
        Vec3 axis = delta.normalize();
        Vec3 reference = Math.abs(axis.y()) > 0.85D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 normalA = axis.cross(reference);
        if (normalA.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
            // Extra fallback in the unlikely case the first reference still ended up too parallel.
            normalA = axis.cross(new Vec3(0.0D, 0.0D, 1.0D));
        }

        normalA = normalA.normalize();
        Vec3 normalB = axis.cross(normalA).normalize();
        return new LocalFrame(axis, normalA, normalB);
    }

    // Retained for future tuning; the current matching pass uses hashed per-sample noise instead.
    private static double smoothNoise(int seed, double position) {
        int base = Mth.floor(position);
        double fraction = position - base;
        // Smoothstep curve. Replacing this with linear interpolation would make transitions harsher.
        double smoothFraction = fraction * fraction * (3.0D - 2.0D * fraction);
        double from = signedNoise(seed, base);
        double to = signedNoise(seed, base + 1);
        return from + (to - from) * smoothFraction;
    }

    // Maps deterministic noise into [0, 1] for phase-style parameters.
    private static double unitValue(int value) {
        return (signedNoise(value, 0) + 1.0D) * 0.5D;
    }

    // Tiny deterministic hash-to-noise function.
    // This is the root randomness source for phases, frequencies, and noise samples.
    // Changing the mixing constants completely changes the visual "personality" of the bolt.
    private static double signedNoise(int seed, int index) {
        long bits = Integer.toUnsignedLong(seed) * 0x9E3779B97F4A7C15L;
        bits ^= (long) index * 0xC2B2AE3D27D4EB4FL;
        bits ^= bits >>> 33;
        bits *= 0xFF51AFD7ED558CCDL;
        bits ^= bits >>> 33;
        bits *= 0xC4CEB9FE1A85EC53L;
        bits ^= bits >>> 33;
        return ((bits & 0x1FFFFFL) / 1048575.0D) * 2.0D - 1.0D;
    }

    // Beam-local coordinate basis used to express offsets as "along the beam" and
    // "sideways from the beam".
    private record LocalFrame(Vec3 axis, Vec3 normalA, Vec3 normalB) {
    }
}
