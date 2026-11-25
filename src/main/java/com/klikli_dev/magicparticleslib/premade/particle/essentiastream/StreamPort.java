// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.essentiastream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * A point where a stream leaves or enters a block through one of its faces.
 * <p>
 * The {@code anchor} is where the stream starts (exit) or ends (entry). The {@code clearance} is a point further out along
 * the face's normal. An exit launches toward the clearance point, so the stream leaves the face before it turns. An entry
 * comes in through the clearance point, so it dives straight into the anchor.
 *
 * @param anchor    the point on or just outside the face
 * @param clearance the point out from the face, on the same normal, used as a waypoint
 */
public record StreamPort(Vec3 anchor, Vec3 clearance) {
    // Nozzles sit just outside the face, and clear it by a larger distance.
    private static final double NOZZLE_ANCHOR_OFFSET = 0.55D;
    private static final double NOZZLE_CLEARANCE_OFFSET = 1.15D;
    // Panes (mirrors, flat faces) are anchored just in front of the face, and clear it by a smaller distance.
    private static final double PANE_ANCHOR_OFFSET = 0.64D;
    private static final double PANE_CLEARANCE_OFFSET = 1.28D;

    /**
     * Creates a port from an anchor and a clearance point.
     */
    public static StreamPort of(Vec3 anchor, Vec3 clearance) {
        return new StreamPort(anchor, clearance);
    }

    /**
     * A nozzle on a block face: the anchor is 0.05 blocks outside the face, and the clearance is 0.65 blocks further out.
     *
     * @param pos    the block the nozzle is on
     * @param facing the face the nozzle points out of
     */
    public static StreamPort nozzle(BlockPos pos, Direction facing) {
        return onFace(pos, facing, NOZZLE_ANCHOR_OFFSET, NOZZLE_CLEARANCE_OFFSET);
    }

    /**
     * A flat pane such as a mirror on a block face: the anchor is 0.14 blocks in front of the face, and the clearance is
     * 0.78 blocks in front of the face.
     *
     * @param pos    the block the pane is on
     * @param facing the face the pane is on, pointing out of the block
     */
    public static StreamPort pane(BlockPos pos, Direction facing) {
        return onFace(pos, facing, PANE_ANCHOR_OFFSET, PANE_CLEARANCE_OFFSET);
    }

    private static StreamPort onFace(BlockPos pos, Direction facing, double anchorOffset, double clearanceOffset) {
        Vec3 centre = Vec3.atCenterOf(pos);
        Vec3 normal = Vec3.atLowerCornerOf(facing.getUnitVec3i());
        return new StreamPort(centre.add(normal.scale(anchorOffset)), centre.add(normal.scale(clearanceOffset)));
    }
}
