// SPDX-FileCopyrightText: 1990, 1991, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 1994, 1995 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

import net.minecraft.world.phys.Vec3;

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
    public static Builder builder() {
        return new Builder();
    }

    public static ExtrusionOptions defaults() {
        return builder().build();
    }

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

        public Builder upVector(Vec3 value) {
            this.upVector = value;
            return this;
        }

        public Builder joinStyle(JoinStyle value) {
            this.joinStyle = value;
            return this;
        }

        public Builder normalStyle(NormalStyle value) {
            this.normalStyle = value;
            return this;
        }

        public Builder capEnds(boolean value) {
            this.capEnds = value;
            return this;
        }

        public Builder textureMode(TextureCoordinateMode value) {
            this.textureMode = value;
            return this;
        }

        public Builder tubeSegments(int value) {
            this.tubeSegments = value;
            return this;
        }

        public Builder roundJoinSegments(int value) {
            this.roundJoinSegments = value;
            return this;
        }

        public Builder textureLengthScale(double value) {
            this.textureLengthScale = value;
            return this;
        }

        public Builder textureLengthOffset(double value) {
            this.textureLengthOffset = value;
            return this;
        }

        public Builder degeneracyTolerance(double value) {
            this.degeneracyTolerance = value;
            return this;
        }

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
