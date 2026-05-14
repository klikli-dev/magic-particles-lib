// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.rift;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record RiftShape(List<Vec3> points, List<Double> widths, AABB bounds) {
    public static final RiftShape EMPTY = new RiftShape(List.of(), List.of(), new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0));

    public RiftShape {
        points = List.copyOf(points);
        widths = List.copyOf(widths);
    }

    public boolean isEmpty() {
        return this.points.isEmpty();
    }
}
