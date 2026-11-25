// SPDX-FileCopyrightText: 1991, 1993, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One sample along an extrusion path.
 *
 * @param position     polyline vertex position
 * @param color        packed ARGB color at the vertex
 * @param contourTransform local contour affine transform for this path point
 * @param twistDegrees local contour twist in degrees
 * @param textureV     optional explicit texture V for the ring at this point. When null, V is derived from the path
 *                     length using the options' {@code textureLengthScale} and {@code textureLengthOffset}
 */
public record PathPoint(Vec3 position, int color, AffineTransform2D contourTransform, double twistDegrees, @Nullable Double textureV) {
    public static final int DEFAULT_COLOR = -1;

    public PathPoint {
        contourTransform = contourTransform == null ? AffineTransform2D.identity() : contourTransform;
    }

    /**
     * Creates a path point with default color and no local contour transform, twist or texture override.
     *
     * @param position polyline vertex position
     * @return path point with default rendering attributes
     */
    public static PathPoint of(Vec3 position) {
        return new PathPoint(position, DEFAULT_COLOR, AffineTransform2D.identity(), 0.0, null);
    }

    /**
     * @param value color at the polyline vertex
     * @return updated path point
     */
    public PathPoint withColor(int value) {
        return new PathPoint(this.position, value, this.contourTransform, this.twistDegrees, this.textureV);
    }

    /**
     * @param value local contour affine transform for this path point
     * @return updated path point
     */
    public PathPoint withContourTransform(AffineTransform2D value) {
        return new PathPoint(this.position, this.color, value, this.twistDegrees, this.textureV);
    }

    /**
     * @param value local contour twist in degrees for this path point
     * @return updated path point
     */
    public PathPoint withTwistDegrees(double value) {
        return new PathPoint(this.position, this.color, this.contourTransform, value, this.textureV);
    }

    /**
     * Sets an explicit texture V for the ring at this point, instead of deriving V from the path length.
     * Use it when V should follow something other than arc length, such as the time a sample was laid.
     *
     * @param value texture V coordinate of the ring at this point
     * @return updated path point
     */
    public PathPoint withTextureV(double value) {
        return new PathPoint(this.position, this.color, this.contourTransform, this.twistDegrees, value);
    }

    AffineTransform2D effectiveTransform() {
        return this.contourTransform.andThen(AffineTransform2D.rotationDegrees(this.twistDegrees));
    }
}
