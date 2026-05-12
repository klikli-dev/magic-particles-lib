// SPDX-FileCopyrightText: 1990, 1991, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 1994, 1995 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ExtrusionEngine {
    public ExtrusionMesh polyCylinder(List<Vec3> path, double radius, ExtrusionOptions options) {
        if (path == null || path.size() < 2 || radius <= 0.0) {
            return ExtrusionMesh.EMPTY;
        }

        Contour2D contour = Contour2D.circle(1.0, options.tubeSegments());
        ArrayList<PathPoint> pathPoints = new ArrayList<>(path.size());
        for (Vec3 point : path) {
            pathPoints.add(PathPoint.of(point).withContourTransform(AffineTransform2D.scale(radius, radius)));
        }
        return extrude(contour, pathPoints, options);
    }

    public ExtrusionMesh polyCone(List<Vec3> path, List<Double> radii, ExtrusionOptions options) {
        if (path == null || radii == null || path.size() < 2 || radii.size() < path.size()) {
            return ExtrusionMesh.EMPTY;
        }

        Contour2D contour = Contour2D.circle(1.0, options.tubeSegments());
        ArrayList<PathPoint> pathPoints = new ArrayList<>(path.size());
        for (int index = 0; index < path.size(); index++) {
            double radius = Math.max(0.0, radii.get(index));
            pathPoints.add(PathPoint.of(path.get(index)).withContourTransform(AffineTransform2D.scale(radius, radius)));
        }
        return extrude(contour, pathPoints, options);
    }

    public ExtrusionMesh extrude(Contour2D contour, List<PathPoint> path, ExtrusionOptions options) {
        if (contour == null || path == null || path.size() < 2) {
            return ExtrusionMesh.EMPTY;
        }
        return new SweepBuilder(contour, List.copyOf(path), options).build();
    }

    public ExtrusionMesh twistExtrude(Contour2D contour, List<Vec3> path, List<Double> twistDegrees, ExtrusionOptions options) {
        if (path == null || twistDegrees == null || twistDegrees.size() < path.size()) {
            return ExtrusionMesh.EMPTY;
        }

        ArrayList<PathPoint> pathPoints = new ArrayList<>(path.size());
        for (int index = 0; index < path.size(); index++) {
            pathPoints.add(PathPoint.of(path.get(index)).withTwistDegrees(twistDegrees.get(index)));
        }
        return extrude(contour, pathPoints, options);
    }

    public ExtrusionMesh superExtrude(Contour2D contour, List<Vec3> path, List<AffineTransform2D> transforms, ExtrusionOptions options) {
        if (path == null || transforms == null || transforms.size() < path.size()) {
            return ExtrusionMesh.EMPTY;
        }

        ArrayList<PathPoint> pathPoints = new ArrayList<>(path.size());
        for (int index = 0; index < path.size(); index++) {
            pathPoints.add(PathPoint.of(path.get(index)).withContourTransform(transforms.get(index)));
        }
        return extrude(contour, pathPoints, options);
    }

    public ExtrusionMesh spiral(Contour2D contour, ExtrusionOptions options, double startRadius, double radiusDeltaPerRevolution, double startZ, double zDeltaPerRevolution, AffineTransform2D startTransform, AffineTransform2D transformDeltaPerRevolution, double startThetaDegrees, double sweepThetaDegrees) {
        GeneratedPath generated = GeneratedPath.spiral(options, startRadius, radiusDeltaPerRevolution, startZ, zDeltaPerRevolution, startTransform, transformDeltaPerRevolution, startThetaDegrees, sweepThetaDegrees);
        ExtrusionOptions forced = ExtrusionOptions.builder()
                .upVector(options.upVector())
                .joinStyle(JoinStyle.ANGLE)
                .normalStyle(options.normalStyle())
                .capEnds(options.capEnds())
                .textureMode(options.textureMode())
                .tubeSegments(options.tubeSegments())
                .roundJoinSegments(options.roundJoinSegments())
                .textureLengthScale(options.textureLengthScale())
                .textureLengthOffset(options.textureLengthOffset())
                .degeneracyTolerance(options.degeneracyTolerance())
                .build();
        return extrude(contour, generated.points(), forced);
    }

    public ExtrusionMesh lathe(Contour2D contour, ExtrusionOptions options, double startRadius, double radiusDeltaPerRevolution, double startZ, double zDeltaPerRevolution, AffineTransform2D startTransform, AffineTransform2D transformDeltaPerRevolution, double startThetaDegrees, double sweepThetaDegrees) {
        Vec3 up = options.upVector();
        Vec3 projectedUp = new Vec3(up.x, 0.0, up.z);
        if (projectedUp.lengthSqr() < 1.0E-8) {
            projectedUp = ExtrusionMath.Z_AXIS;
        } else {
            projectedUp = projectedUp.normalize();
        }

        double tx = projectedUp.z * radiusDeltaPerRevolution - projectedUp.x * zDeltaPerRevolution;
        double ty = projectedUp.x * radiusDeltaPerRevolution + projectedUp.z * zDeltaPerRevolution;
        AffineTransform2D adjustedDelta = (transformDeltaPerRevolution == null ? AffineTransform2D.identity() : transformDeltaPerRevolution)
                .andThen(AffineTransform2D.translation(tx, ty));
        return spiral(contour, options, startRadius, 0.0, startZ, 0.0, startTransform == null ? AffineTransform2D.identity() : startTransform, adjustedDelta, startThetaDegrees, sweepThetaDegrees);
    }

    public ExtrusionMesh helicoid(double toroidRadius, ExtrusionOptions options, double startRadius, double radiusDeltaPerRevolution, double startZ, double zDeltaPerRevolution, AffineTransform2D startTransform, AffineTransform2D transformDeltaPerRevolution, double startThetaDegrees, double sweepThetaDegrees) {
        ExtrusionOptions local = ExtrusionOptions.builder()
                .upVector(new Vec3(1.0, 0.0, 0.0))
                .joinStyle(options.joinStyle())
                .normalStyle(NormalStyle.PATH_EDGE)
                .capEnds(options.capEnds())
                .textureMode(options.textureMode())
                .tubeSegments(options.tubeSegments())
                .roundJoinSegments(options.roundJoinSegments())
                .textureLengthScale(options.textureLengthScale())
                .textureLengthOffset(options.textureLengthOffset())
                .degeneracyTolerance(options.degeneracyTolerance())
                .build();
        return spiral(Contour2D.circle(toroidRadius, options.tubeSegments()), local, startRadius, radiusDeltaPerRevolution, startZ, zDeltaPerRevolution, startTransform, transformDeltaPerRevolution, startThetaDegrees, sweepThetaDegrees);
    }

    public ExtrusionMesh toroid(double toroidRadius, ExtrusionOptions options, double startRadius, double radiusDeltaPerRevolution, double startZ, double zDeltaPerRevolution, AffineTransform2D startTransform, AffineTransform2D transformDeltaPerRevolution, double startThetaDegrees, double sweepThetaDegrees) {
        ExtrusionOptions local = ExtrusionOptions.builder()
                .upVector(new Vec3(1.0, 0.0, 0.0))
                .joinStyle(options.joinStyle())
                .normalStyle(NormalStyle.PATH_EDGE)
                .capEnds(options.capEnds())
                .textureMode(options.textureMode())
                .tubeSegments(options.tubeSegments())
                .roundJoinSegments(options.roundJoinSegments())
                .textureLengthScale(options.textureLengthScale())
                .textureLengthOffset(options.textureLengthOffset())
                .degeneracyTolerance(options.degeneracyTolerance())
                .build();
        return lathe(Contour2D.circle(toroidRadius, options.tubeSegments()), local, startRadius, radiusDeltaPerRevolution, startZ, zDeltaPerRevolution, startTransform, transformDeltaPerRevolution, startThetaDegrees, sweepThetaDegrees);
    }

    public ExtrusionMesh screw(Contour2D contour, ExtrusionOptions options, double startZ, double endZ, double twistDegrees) {
        int segmentCount = Math.max(4, (int) Math.abs(twistDegrees / 18.0) + 4);
        double deltaZ = (endZ - startZ) / (segmentCount - 3);
        double deltaAngle = twistDegrees / (segmentCount - 3);

        ArrayList<Vec3> path = new ArrayList<>(segmentCount);
        ArrayList<Double> twists = new ArrayList<>(segmentCount);
        double z = startZ - deltaZ;
        double angle = -deltaAngle;
        for (int index = 0; index < segmentCount; index++) {
            path.add(new Vec3(0.0, 0.0, z));
            twists.add(angle);
            z += deltaZ;
            angle += deltaAngle;
        }

        return twistExtrude(contour, path, twists, options);
    }

    public static int argb(float red, float green, float blue, float alpha) {
        return ARGB.color(
                Mth.floor(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F),
                Mth.floor(Mth.clamp(red, 0.0F, 1.0F) * 255.0F),
                Mth.floor(Mth.clamp(green, 0.0F, 1.0F) * 255.0F),
                Mth.floor(Mth.clamp(blue, 0.0F, 1.0F) * 255.0F)
        );
    }

    private record GeneratedPath(List<PathPoint> points) {
        private static GeneratedPath spiral(ExtrusionOptions options, double startRadius, double radiusDeltaPerRevolution, double startZ, double zDeltaPerRevolution, AffineTransform2D startTransform, AffineTransform2D transformDeltaPerRevolution, double startThetaDegrees, double sweepThetaDegrees) {
            int pointCount = (int) (((double) options.tubeSegments() / 360.0) * Math.abs(sweepThetaDegrees)) + 4;
            pointCount = Math.max(pointCount, 4);

            double deltaAngle = Math.toRadians(sweepThetaDegrees) / (pointCount - 3);
            double theta = Math.toRadians(startThetaDegrees) - deltaAngle;
            double radius = startRadius - radiusDeltaPerRevolution * (deltaAngle / (2.0 * Math.PI));
            double z = startZ - zDeltaPerRevolution * (deltaAngle / (2.0 * Math.PI));
            double radiusStep = radiusDeltaPerRevolution * (deltaAngle / (2.0 * Math.PI));
            double zStep = zDeltaPerRevolution * (deltaAngle / (2.0 * Math.PI));
            AffineTransform2D transform = startTransform == null ? AffineTransform2D.identity() : startTransform;

            ArrayList<PathPoint> points = new ArrayList<>(pointCount);
            for (int index = 0; index < pointCount; index++) {
                points.add(new PathPoint(
                        new Vec3(radius * Math.cos(theta), radius * Math.sin(theta), z),
                        PathPoint.DEFAULT_COLOR,
                        transform,
                        0.0
                ));
                theta += deltaAngle;
                radius += radiusStep;
                z += zStep;
                transform = transform.integrateDifferential(transformDeltaPerRevolution, deltaAngle / (2.0 * Math.PI));
            }
            return new GeneratedPath(List.copyOf(points));
        }
    }
}
