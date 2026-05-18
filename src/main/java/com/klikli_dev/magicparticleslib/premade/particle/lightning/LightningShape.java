// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.lightning;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Immutable sampled representation of the lightning centerline.
// Each sampled point carries a matching width and contour twist value.
record LightningShape(List<Vec3> points, List<Float> widths, List<Float> twists) {
    LightningShape {
        points = List.copyOf(points);
        widths = List.copyOf(widths);
        twists = List.copyOf(twists);

        if (points.size() != widths.size() || points.size() != twists.size()) {
            throw new IllegalArgumentException("Lightning shape points, widths, and twists must have identical sizes.");
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

    float sampledWidth(int index) {
        float current = this.widths.get(index);
        if (index <= 0 || index >= this.widths.size() - 1) {
            return Math.max(0.0F, current);
        }

        float previous = this.widths.get(index - 1);
        float next = this.widths.get(index + 1);
        return Math.max(0.0F, current * 0.6F + (previous + next) * 0.2F);
    }

    float sampledTwist(int index) {
        float current = this.twists.get(index);
        if (index <= 0 || index >= this.twists.size() - 1) {
            return current;
        }

        float previous = this.twists.get(index - 1);
        float next = this.twists.get(index + 1);
        return Mth.lerp(0.5F, current, (previous + next) * 0.5F);
    }
}
