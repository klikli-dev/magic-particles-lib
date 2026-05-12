// SPDX-FileCopyrightText: 1991, 1993, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

import net.minecraft.world.phys.Vec3;

final class ExtrusionMath {
    static final Vec3 X_AXIS = new Vec3(1.0, 0.0, 0.0);
    static final Vec3 Y_AXIS = new Vec3(0.0, 1.0, 0.0);
    static final Vec3 Z_AXIS = new Vec3(0.0, 0.0, 1.0);

    private ExtrusionMath() {
    }

    static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback, double tolerance) {
        return vector.lengthSqr() <= tolerance * tolerance ? fallback : vector.normalize();
    }

    static Vec3 perpendicularComponent(Vec3 vector, Vec3 axis) {
        return vector.subtract(axis.scale(vector.dot(axis)));
    }

    static Vec3 reflectAcrossPlane(Vec3 vector, Vec3 planeNormal) {
        return vector.subtract(planeNormal.scale(2.0 * vector.dot(planeNormal)));
    }

    static boolean isDegenerate(Vec3 first, Vec3 second, double tolerance) {
        Vec3 diff = second.subtract(first);
        double diffLength = diff.lengthSqr();
        Vec3 sum = second.add(first);
        double sumLength = Math.max(sum.lengthSqr(), 1.0);
        return diffLength <= tolerance * tolerance * sumLength;
    }

    static Vec3 interpolate(Vec3 a, Vec3 b, double t) {
        return a.scale(1.0 - t).add(b.scale(t));
    }

    static Vec3 intersectPlane(Vec3 planePoint, Vec3 planeNormal, Vec3 lineStart, Vec3 lineEnd) {
        Vec3 line = lineStart.subtract(lineEnd);
        double denominator = line.dot(planeNormal);
        if (Math.abs(denominator) < 1.0E-10) {
            return lineStart;
        }

        double numerator = planePoint.subtract(lineEnd).dot(planeNormal);
        double t = numerator / denominator;
        double omt = 1.0 - t;
        return new Vec3(
                t * lineStart.x + omt * lineEnd.x,
                t * lineStart.y + omt * lineEnd.y,
                t * lineStart.z + omt * lineEnd.z
        );
    }

    static Vec3 bisectingPlane(Vec3 previous, Vec3 current, Vec3 next, double tolerance) {
        Vec3 v21 = current.subtract(previous);
        Vec3 v32 = next.subtract(current);
        double len21 = v21.length();
        double len32 = v32.length();

        if (len21 <= tolerance * len32) {
            return len32 <= tolerance ? Vec3.ZERO : v32.scale(1.0 / len32);
        }

        if (len32 <= tolerance * len21) {
            return v21.scale(1.0 / len21);
        }

        Vec3 n21 = v21.scale(1.0 / len21);
        Vec3 n32 = v32.scale(1.0 / len32);
        double dot = n32.dot(n21);
        if (dot >= (1.0 - tolerance) || dot <= (-1.0 + tolerance)) {
            return n21;
        }

        Vec3 bisector = new Vec3(
                dot * (n32.x + n21.x) - n32.x - n21.x,
                dot * (n32.y + n21.y) - n32.y - n21.y,
                dot * (n32.z + n21.z) - n32.z - n21.z
        );
        return normalizeOrFallback(bisector, n21, tolerance);
    }

    static Vec3 cuttingPlane(Vec3 previous, Vec3 current, Vec3 next, double tolerance) {
        Vec3 v21 = current.subtract(previous);
        Vec3 v32 = next.subtract(current);
        double len21 = v21.length();
        double len32 = v32.length();

        if (len21 <= tolerance * len32) {
            return len32 <= tolerance ? Vec3.ZERO : v32.scale(1.0 / len32);
        }

        if (len32 <= tolerance * len21) {
            return v21.scale(1.0 / len21);
        }

        Vec3 n21 = v21.scale(1.0 / len21);
        Vec3 n32 = v32.scale(1.0 / len32);
        Vec3 cut = n21.subtract(n32);
        double length = cut.length();
        return length < tolerance ? Vec3.ZERO : cut.scale(1.0 / length);
    }

    static Frame frameFromTangent(Vec3 origin, Vec3 tangent, Vec3 upHint, double tolerance) {
        Vec3 tangentUnit = normalizeOrFallback(tangent, Z_AXIS, tolerance);
        Vec3 normal = perpendicularComponent(upHint, tangentUnit);
        if (normal.lengthSqr() < tolerance * tolerance) {
            normal = tangentUnit.cross(Math.abs(tangentUnit.dot(Y_AXIS)) > 0.98 ? X_AXIS : Y_AXIS);
        }
        normal = normalizeOrFallback(normal, X_AXIS, tolerance);
        Vec3 binormal = normalizeOrFallback(tangentUnit.cross(normal), Z_AXIS, tolerance);
        normal = normalizeOrFallback(binormal.cross(tangentUnit), X_AXIS, tolerance);
        return new Frame(origin, tangentUnit, normal, binormal);
    }

    record Frame(Vec3 origin, Vec3 tangent, Vec3 normal, Vec3 binormal) {
        Vec3 toWorld(Vec2d point) {
            return this.origin.add(this.normal.scale(point.x())).add(this.binormal.scale(point.y()));
        }

        Vec3 directionToWorld(Vec2d direction) {
            return this.normal.scale(direction.x()).add(this.binormal.scale(direction.y()));
        }
    }
}
