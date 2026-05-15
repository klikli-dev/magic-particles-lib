// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.branchingrift;

import com.klikli_dev.magicparticleslib.premade.rift.RiftShape;

// parentSegmentIndex/parentAnchorIndex describe where this segment attaches into the already-generated tree.
// growthScale is the structural width multiplier applied on top of the segment's local radii.
public record BranchingRiftSegment(RiftShape shape, int parentSegmentIndex, int parentAnchorIndex, int depth, double growthScale) {
    public static final int ROOT_PARENT_SEGMENT = -1;
    public static final int ROOT_PARENT_ANCHOR = -1;
    public static final BranchingRiftSegment EMPTY = new BranchingRiftSegment(RiftShape.EMPTY, ROOT_PARENT_SEGMENT, ROOT_PARENT_ANCHOR, 0, 0.0);

    public boolean isRoot() {
        return this.parentSegmentIndex == ROOT_PARENT_SEGMENT;
    }
}
