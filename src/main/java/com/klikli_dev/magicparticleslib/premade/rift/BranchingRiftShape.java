// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.rift;

import net.minecraft.world.phys.AABB;

import java.util.List;

public record BranchingRiftShape(List<BranchingRiftSegment> segments, AABB bounds) {
    public static final BranchingRiftShape EMPTY = new BranchingRiftShape(List.of(), new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0));

    public BranchingRiftShape {
        segments = List.copyOf(segments);
    }

    public boolean isEmpty() {
        return this.segments.isEmpty();
    }

    public BranchingRiftSegment rootSegment() {
        return this.isEmpty() ? BranchingRiftSegment.EMPTY : this.segments.get(0);
    }
}
