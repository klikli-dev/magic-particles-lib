// SPDX-FileCopyrightText: 1991, 1993, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

public record AffineTransform2D(double m00, double m01, double m02, double m10, double m11, double m12) {
    public static final AffineTransform2D IDENTITY = new AffineTransform2D(1.0, 0.0, 0.0, 0.0, 1.0, 0.0);

    public static AffineTransform2D identity() {
        return IDENTITY;
    }

    public static AffineTransform2D scale(double scaleX, double scaleY) {
        return new AffineTransform2D(scaleX, 0.0, 0.0, 0.0, scaleY, 0.0);
    }

    public static AffineTransform2D translation(double x, double y) {
        return new AffineTransform2D(1.0, 0.0, x, 0.0, 1.0, y);
    }

    public static AffineTransform2D rotationDegrees(double degrees) {
        return rotationRadians(Math.toRadians(degrees));
    }

    public static AffineTransform2D rotationRadians(double radians) {
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new AffineTransform2D(cos, -sin, 0.0, sin, cos, 0.0);
    }

    static AffineTransform2D fromLegacyMatrix(double[][] matrix) {
        if (matrix == null || matrix.length < 2 || matrix[0].length < 3 || matrix[1].length < 3) {
            return identity();
        }

        return new AffineTransform2D(matrix[0][0], matrix[0][1], matrix[0][2], matrix[1][0], matrix[1][1], matrix[1][2]);
    }

    public AffineTransform2D andThen(AffineTransform2D next) {
        return new AffineTransform2D(
                next.m00 * this.m00 + next.m01 * this.m10,
                next.m00 * this.m01 + next.m01 * this.m11,
                next.m00 * this.m02 + next.m01 * this.m12 + next.m02,
                next.m10 * this.m00 + next.m11 * this.m10,
                next.m10 * this.m01 + next.m11 * this.m11,
                next.m10 * this.m02 + next.m11 * this.m12 + next.m12
        );
    }

    public Vec2d transformPoint(Vec2d point) {
        return new Vec2d(
                this.m00 * point.x() + this.m01 * point.y() + this.m02,
                this.m10 * point.x() + this.m11 * point.y() + this.m12
        );
    }

    public Vec2d transformDirection(Vec2d direction) {
        return new Vec2d(
                this.m00 * direction.x() + this.m01 * direction.y(),
                this.m10 * direction.x() + this.m11 * direction.y()
        );
    }

    public Vec2d transformNormal(Vec2d normal) {
        if ((this.m01 != 0.0) || (this.m10 != 0.0) || (this.m00 != this.m11)) {
            return new Vec2d(
                    this.m11 * normal.x() - this.m10 * normal.y(),
                    -this.m01 * normal.x() + this.m00 * normal.y()
            ).normalize();
        }

        return normal;
    }

    AffineTransform2D integrateDifferential(AffineTransform2D derivativePerRevolution, double deltaRevolutions) {
        if (derivativePerRevolution == null) {
            return this;
        }

        AffineTransform2D delta = new AffineTransform2D(
                1.0 + derivativePerRevolution.m00 * deltaRevolutions,
                derivativePerRevolution.m01 * deltaRevolutions,
                derivativePerRevolution.m02 * deltaRevolutions,
                derivativePerRevolution.m10 * deltaRevolutions,
                1.0 + derivativePerRevolution.m11 * deltaRevolutions,
                derivativePerRevolution.m12 * deltaRevolutions
        );

        return this.andThen(delta);
    }
}
