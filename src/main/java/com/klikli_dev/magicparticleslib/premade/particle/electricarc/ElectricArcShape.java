// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.electricarc;

import net.minecraft.world.phys.Vec3;

import java.util.List;

// Immutable sampled representation of the beam centerline.
// Each point has a matching width entry.
record ElectricArcShape(List<Vec3> points, List<Float> widths) {
    ElectricArcShape {
        // Freeze the lists so render code cannot accidentally mutate procedural state after generation.
        points = List.copyOf(points);
        widths = List.copyOf(widths);

        if (points.size() != widths.size()) {
            throw new IllegalArgumentException("Electric arc shape points and widths must have identical sizes.");
        }
    }

    int pointCount() {
        return this.points.size();
    }

    boolean isRenderable() {
        return this.pointCount() > 1;
    }

    Vec3 point(int index) {
        return this.points.get(index);
    }

    // Returns a slightly smoothed width sample.
    // Using the current sample alone makes abrupt width noise much more visible in the mesh.
    // Heavier weighting on neighbors would make the bolt thickness more uniform and less spark-like.
    float sampledWidth(int index) {
        float current = this.widths.get(index);
        if (index <= 0 || index >= this.widths.size() - 1) {
            return Math.max(0.0F, current);
        }

        float previous = this.widths.get(index - 1);
        float next = this.widths.get(index + 1);
        return Math.max(0.0F, current * 0.55F + (previous + next) * 0.225F);
    }
}
