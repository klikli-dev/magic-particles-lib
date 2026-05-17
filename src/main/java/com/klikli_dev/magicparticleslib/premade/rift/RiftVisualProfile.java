// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.rift;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RiftVisualProfile {
    private static final float HALO_START_WIDTH_SCALE = 1.25F;
    private static final float HALO_WIDTH_STEP = 0.5F;
    private static final int HALO_PASS_COUNT = 3;
    private static final float CORE_WIDTH_SCALE = 1.0F;

    private static final double PHASE_SPACING = 10.0;
    private static final double BRANCH_PHASE_OFFSET = 17.0;
    private static final double X_WOBBLE_PERIOD = 50.0;
    private static final double Y_WOBBLE_PERIOD = 60.0;
    private static final double Z_WOBBLE_PERIOD = 70.0;
    private static final double WIDTH_PULSE_PERIOD = 8.0;
    private static final double BASE_WOBBLE_STRENGTH = 0.1;

    private RiftVisualProfile() {
    }

    public static int passCount() {
        return HALO_PASS_COUNT + 1;
    }

    public static boolean haloPass(int passIndex) {
        return passIndex < HALO_PASS_COUNT;
    }

    public static double widthScale(int passIndex) {
        // The first passes intentionally overdraw a fatter shell; changing these constants directly changes glow thickness.
        return haloPass(passIndex)
                ? HALO_START_WIDTH_SCALE + HALO_WIDTH_STEP * passIndex
                : CORE_WIDTH_SCALE;
    }

    public static int centerIndex(int pointCount, boolean anchoredAtStart) {
        // Branch segments are anchored at their first point, while standalone rifts should pulse from the middle.
        return anchoredAtStart ? 0 : pointCount / 2;
    }

    public static double wobbleStrength(float visualIntensity) {
        // visualIntensity is a straight multiplier, so doubling it doubles both positional wobble and width pulse amplitude.
        return BASE_WOBBLE_STRENGTH * visualIntensity;
    }

    public static double phase(float ageInTicks, int pointIndex, int centerIndex) {
        // PHASE_SPACING controls how quickly motion shifts across the path: larger values create a slower travelling wave.
        return ageInTicks + Math.abs(pointIndex - centerIndex) * PHASE_SPACING;
    }

    public static double phase(float ageInTicks, int segmentIndex, int pointIndex, int centerIndex) {
        // A per-segment offset keeps child branches from wobbling in perfect lockstep with the trunk.
        return phase(ageInTicks, pointIndex, centerIndex) + segmentIndex * BRANCH_PHASE_OFFSET;
    }

    public static Vec3 wobbleOffset(double phase, double wobbleStrength) {
        // Different axis periods stop the motion from looping as a simple circle.
        return new Vec3(
                Mth.sin((float) (phase / X_WOBBLE_PERIOD)) * wobbleStrength,
                Mth.sin((float) (phase / Y_WOBBLE_PERIOD)) * wobbleStrength,
                Mth.sin((float) (phase / Z_WOBBLE_PERIOD)) * wobbleStrength
        );
    }

    public static double radiusPulse(double phase, double wobbleStrength) {
        // WIDTH_PULSE_PERIOD sets how quickly the tube breathes; lower values make the width flicker faster.
        return 1.0 + Mth.sin((float) (phase / WIDTH_PULSE_PERIOD)) * wobbleStrength;
    }
}
