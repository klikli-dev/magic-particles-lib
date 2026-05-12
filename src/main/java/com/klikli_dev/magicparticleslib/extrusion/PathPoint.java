// SPDX-FileCopyrightText: 1991, 1993, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

import net.minecraft.world.phys.Vec3;

public record PathPoint(Vec3 position, int color, AffineTransform2D contourTransform, double twistDegrees) {
    public static final int DEFAULT_COLOR = -1;

    public PathPoint {
        contourTransform = contourTransform == null ? AffineTransform2D.identity() : contourTransform;
    }

    public static PathPoint of(Vec3 position) {
        return new PathPoint(position, DEFAULT_COLOR, AffineTransform2D.identity(), 0.0);
    }

    public PathPoint withColor(int value) {
        return new PathPoint(this.position, value, this.contourTransform, this.twistDegrees);
    }

    public PathPoint withContourTransform(AffineTransform2D value) {
        return new PathPoint(this.position, this.color, value, this.twistDegrees);
    }

    public PathPoint withTwistDegrees(double value) {
        return new PathPoint(this.position, this.color, this.contourTransform, value);
    }

    AffineTransform2D effectiveTransform() {
        return this.contourTransform.andThen(AffineTransform2D.rotationDegrees(this.twistDegrees));
    }
}
