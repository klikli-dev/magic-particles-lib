// SPDX-FileCopyrightText: 1990, 1991, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

import net.minecraft.world.phys.Vec3;

/**
 * Matrix utilities corresponding to the upstream rotation and viewpoint helpers.
 */
public final class ExtrusionTransforms {
    private ExtrusionTransforms() {
    }

    /**
     * Computes a 4x4 matrix that places the forward direction on the z-axis and the up vector on the y-axis.
     *
     * @param direction input direction
     * @param up input up vector
     * @return 4x4 orientation matrix
     */
    public static double[][] lookAlong(Vec3 direction, Vec3 up) {
        Vec3 forward = direction.lengthSqr() < 1.0E-12 ? ExtrusionMath.Z_AXIS : direction.normalize();
        Vec3 right = up.cross(forward);
        if (right.lengthSqr() < 1.0E-12) {
            right = ExtrusionMath.X_AXIS;
        } else {
            right = right.normalize();
        }

        Vec3 orthogonalUp = forward.cross(right).normalize();
        return new double[][] {
                {right.x, orthogonalUp.x, forward.x, 0.0},
                {right.y, orthogonalUp.y, forward.y, 0.0},
                {right.z, orthogonalUp.z, forward.z, 0.0},
                {0.0, 0.0, 0.0, 1.0}
        };
    }

    /**
     * Computes a 4x4 matrix that translates the origin to {@code from}, puts the z-axis along {@code to - from},
     * and aligns the y-axis with {@code up}.
     *
     * @param from point to translate the origin to
     * @param to point defining the viewing direction
     * @param up up vector
     * @return 4x4 viewpoint matrix
     */
    public static double[][] lookAt(Vec3 from, Vec3 to, Vec3 up) {
        double[][] matrix = lookAlong(to.subtract(from), up);
        matrix[3][0] = from.x;
        matrix[3][1] = from.y;
        matrix[3][2] = from.z;
        return matrix;
    }

    /**
     * Computes a rotation matrix around the specified axis.
     *
     * @param radians rotation angle in radians
     * @param axis rotation axis
     * @return 4x4 rotation matrix
     */
    public static double[][] rotationAroundAxisRadians(double radians, Vec3 axis) {
        Vec3 normalized = axis.lengthSqr() < 1.0E-12 ? ExtrusionMath.Z_AXIS : axis.normalize();
        double halfAngle = radians / 2.0;
        double s = Math.sin(halfAngle);
        double c = Math.cos(halfAngle);
        double ssq = s * s;
        double csq = c * c;
        double cts = 2.0 * c * s;

        double[][] matrix = new double[4][4];
        matrix[0][0] = csq - ssq + ssq * 2.0 * normalized.x * normalized.x;
        matrix[1][1] = csq - ssq + ssq * 2.0 * normalized.y * normalized.y;
        matrix[2][2] = csq - ssq + ssq * 2.0 * normalized.z * normalized.z;
        matrix[3][3] = 1.0;

        double shared = ssq * 2.0;
        matrix[0][1] = normalized.x * normalized.y * shared + cts * normalized.z;
        matrix[1][0] = normalized.x * normalized.y * shared - cts * normalized.z;
        matrix[1][2] = normalized.y * normalized.z * shared + cts * normalized.x;
        matrix[2][1] = normalized.y * normalized.z * shared - cts * normalized.x;
        matrix[2][0] = normalized.z * normalized.x * shared + cts * normalized.y;
        matrix[0][2] = normalized.z * normalized.x * shared - cts * normalized.y;
        return matrix;
    }

    /**
     * Computes a rotation matrix around the specified axis.
     *
     * @param degrees rotation angle in degrees
     * @param axis rotation axis
     * @return 4x4 rotation matrix
     */
    public static double[][] rotationAroundAxisDegrees(double degrees, Vec3 axis) {
        return rotationAroundAxisRadians(Math.toRadians(degrees), axis);
    }
}
