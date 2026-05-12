// SPDX-FileCopyrightText: 1990, 1991, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 1994, 1995 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

import net.minecraft.world.phys.Vec3;

/**
 * Immutable configuration for extrusion and tubing generation.
 *
 * @param upVector up vector for the contour orientation
 * @param joinStyle join style for tube intersections
 * @param normalStyle normal handling mode
 * @param capEnds whether front and back end caps are emitted
 * @param textureMode texture coordinate generation mode
 * @param tubeSegments number of sides used to draw cylinders and circular contours
 * @param roundJoinSegments number of pieces used to tessellate round joins
 * @param textureLengthScale scale applied to path-length-based texture coordinates
 * @param textureLengthOffset offset applied to path-length-based texture coordinates
 * @param degeneracyTolerance degeneracy tolerance used for repeated points and near-colinear segments
 */
public record ExtrusionOptions(
        Vec3 upVector,
        JoinStyle joinStyle,
        NormalStyle normalStyle,
        boolean capEnds,
        TextureCoordinateMode textureMode,
        int tubeSegments,
        int roundJoinSegments,
        double textureLengthScale,
        double textureLengthOffset,
        double degeneracyTolerance
) {
    /**
     * Creates a mutable builder for extrusion options.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns default options matching the modern port defaults.
     *
     * @return default extrusion options
     */
    public static ExtrusionOptions defaults() {
        return builder().build();
    }

    /**
     * Converts upstream-style bit flags into typed options.
     *
     * @param styleBits bitwise OR of upstream join and normal flags
     * @param textureBits upstream texture mode flags
     * @return options derived from the legacy flags
     */
    public static ExtrusionOptions fromLegacyStyle(int styleBits, int textureBits) {
        return builder()
                .joinStyle(JoinStyle.fromLegacyBits(styleBits))
                .normalStyle(NormalStyle.fromLegacyBits(styleBits))
                .capEnds((styleBits & 0x10) != 0)
                .textureMode(TextureCoordinateMode.fromLegacyBits(textureBits))
                .build();
    }

    public ExtrusionOptions {
        upVector = upVector == null ? new Vec3(0.0, 1.0, 0.0) : upVector;
        joinStyle = joinStyle == null ? JoinStyle.ANGLE : joinStyle;
        normalStyle = normalStyle == null ? NormalStyle.FACET : normalStyle;
        textureMode = textureMode == null ? TextureCoordinateMode.NONE : textureMode;
        tubeSegments = Math.max(3, tubeSegments);
        roundJoinSegments = Math.max(1, roundJoinSegments);
        textureLengthScale = Math.max(0.0, textureLengthScale);
        degeneracyTolerance = Math.max(1.0E-8, degeneracyTolerance);
    }

    public static final class Builder {
        private Vec3 upVector = new Vec3(0.0, 1.0, 0.0);
        private JoinStyle joinStyle = JoinStyle.ANGLE;
        private NormalStyle normalStyle = NormalStyle.FACET;
        private boolean capEnds = true;
        private TextureCoordinateMode textureMode = TextureCoordinateMode.NONE;
        private int tubeSegments = 20;
        private int roundJoinSegments = 5;
        private double textureLengthScale = 1.0;
        private double textureLengthOffset = 0.0;
        private double degeneracyTolerance = 0.000002;

        /**
         * @param value up vector for the contour frame
         * @return this builder
         */
        public Builder upVector(Vec3 value) {
            this.upVector = value;
            return this;
        }

        /**
         * @param value join style controlling tube intersections
         * @return this builder
         */
        public Builder joinStyle(JoinStyle value) {
            this.joinStyle = value;
            return this;
        }

        /**
         * @param value normal handling mode
         * @return this builder
         */
        public Builder normalStyle(NormalStyle value) {
            this.normalStyle = value;
            return this;
        }

        /**
         * @param value whether to emit front and back caps
         * @return this builder
         */
        public Builder capEnds(boolean value) {
            this.capEnds = value;
            return this;
        }

        /**
         * @param value texture coordinate generation mode
         * @return this builder
         */
        public Builder textureMode(TextureCoordinateMode value) {
            this.textureMode = value;
            return this;
        }

        /**
         * @param value number of sides used for cylinders and circular contours
         * @return this builder
         */
        public Builder tubeSegments(int value) {
            this.tubeSegments = value;
            return this;
        }

        /**
         * @param value number of tessellation pieces used for round joins
         * @return this builder
         */
        public Builder roundJoinSegments(int value) {
            this.roundJoinSegments = value;
            return this;
        }

        /**
         * @param value scale applied to path-length-based texture coordinates
         * @return this builder
         */
        public Builder textureLengthScale(double value) {
            this.textureLengthScale = value;
            return this;
        }

        /**
         * @param value offset applied to path-length-based texture coordinates
         * @return this builder
         */
        public Builder textureLengthOffset(double value) {
            this.textureLengthOffset = value;
            return this;
        }

        /**
         * @param value relative tolerance for degenerate segments and points
         * @return this builder
         */
        public Builder degeneracyTolerance(double value) {
            this.degeneracyTolerance = value;
            return this;
        }

        /**
         * Builds the immutable option set.
         *
         * @return configured extrusion options
         */
        public ExtrusionOptions build() {
            return new ExtrusionOptions(
                    this.upVector,
                    this.joinStyle,
                    this.normalStyle,
                    this.capEnds,
                    this.textureMode,
                    this.tubeSegments,
                    this.roundJoinSegments,
                    this.textureLengthScale,
                    this.textureLengthOffset,
                    this.degeneracyTolerance
            );
        }
    }
}
