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

/**
 * Modern Java facade for the upstream GLE tubing and extrusion primitives.
 */
public final class ExtrusionEngine {
    /**
     * Draws a polycylinder specified as a polyline.
     *
     * @param path polyline vertices
     * @param radius radius of the polycylinder
     * @param options join, normal, cap, and texture settings
     * @return generated mesh for the polycylinder
     */
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

    /**
     * Draws a polycone specified as a polyline with a radius for each polyline vertex.
     *
     * @param path polyline vertices
     * @param radii cone radii at polyline vertices
     * @param options join, normal, cap, and texture settings
     * @return generated mesh for the polycone
     */
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

    /**
     * Extrudes an arbitrary 2D contour along an arbitrary 3D path.
     *
     * @param contour 2D contour and optional contour normals
     * @param path polyline path samples, including per-point color and contour transforms
     * @param options up vector, join style, caps, and texturing behavior
     * @return generated mesh for the swept contour
     */
    public ExtrusionMesh extrude(Contour2D contour, List<PathPoint> path, ExtrusionOptions options) {
        if (contour == null || path == null || path.size() < 2) {
            return ExtrusionMesh.EMPTY;
        }
        return new SweepBuilder(contour, List.copyOf(path), options).build();
    }

    /**
     * Extrudes a 2D contour while applying a local twist at each path point.
     *
     * @param contour 2D contour and optional contour normals
     * @param path polyline vertices
     * @param twistDegrees contour twists in degrees, one per path point
     * @param options up vector, join style, caps, and texturing behavior
     * @return generated mesh for the twisted extrusion
     */
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

    /**
     * Extrudes a 2D contour while applying a local affine transform at each path point.
     *
     * @param contour 2D contour and optional contour normals
     * @param path polyline vertices
     * @param transforms contour affine transforms, one per path point
     * @param options up vector, join style, caps, and texturing behavior
     * @return generated mesh for the transformed extrusion
     */
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

    /**
     * Moves a contour along a helical path by parallel transport.
     *
     * @param contour number of contour points and contour geometry to sweep
     * @param options up vector, caps, normals, and texture behavior
     * @param startRadius spiral start radius in the x-y plane
     * @param radiusDeltaPerRevolution change in radius per revolution
     * @param startZ starting z value
     * @param zDeltaPerRevolution change in z per revolution
     * @param startTransform starting contour affine transform
     * @param transformDeltaPerRevolution affine transform delta per revolution
     * @param startThetaDegrees start angle in the x-y plane, in degrees
     * @param sweepThetaDegrees degrees to spiral around
     * @return generated mesh for the spiral sweep
     */
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

    /**
     * Moves a contour along a helical path by helically shearing the local contour space.
     *
     * @param contour number of contour points and contour geometry to sweep
     * @param options up vector, caps, normals, and texture behavior
     * @param startRadius spiral start radius in the x-y plane
     * @param radiusDeltaPerRevolution change in radius per revolution
     * @param startZ starting z value
     * @param zDeltaPerRevolution change in z per revolution
     * @param startTransform starting contour affine transform
     * @param transformDeltaPerRevolution affine transform delta per revolution
     * @param startThetaDegrees start angle in the x-y plane, in degrees
     * @param sweepThetaDegrees degrees to spiral around
     * @return generated mesh for the lathed sweep
     */
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

    /**
     * Similar to {@link #spiral(Contour2D, ExtrusionOptions, double, double, double, double, AffineTransform2D, AffineTransform2D, double, double)},
     * except the contour is a circle.
     *
     * @param toroidRadius circle contour radius
     * @param options up vector, caps, normals, and texture behavior
     * @param startRadius spiral start radius in the x-y plane
     * @param radiusDeltaPerRevolution change in radius per revolution
     * @param startZ starting z value
     * @param zDeltaPerRevolution change in z per revolution
     * @param startTransform starting contour affine transform
     * @param transformDeltaPerRevolution affine transform delta per revolution
     * @param startThetaDegrees start angle in the x-y plane, in degrees
     * @param sweepThetaDegrees degrees to spiral around
     * @return generated mesh for the helicoid
     */
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

    /**
     * Similar to {@link #lathe(Contour2D, ExtrusionOptions, double, double, double, double, AffineTransform2D, AffineTransform2D, double, double)},
     * except the contour is a circle.
     *
     * @param toroidRadius circle contour radius
     * @param options up vector, caps, normals, and texture behavior
     * @param startRadius spiral start radius in the x-y plane
     * @param radiusDeltaPerRevolution change in radius per revolution
     * @param startZ starting z value
     * @param zDeltaPerRevolution change in z per revolution
     * @param startTransform starting contour affine transform
     * @param transformDeltaPerRevolution affine transform delta per revolution
     * @param startThetaDegrees start angle in the x-y plane, in degrees
     * @param sweepThetaDegrees degrees to spiral around
     * @return generated mesh for the toroid
     */
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

    /**
     * Draws a screw shape by extruding a contour from {@code startZ} to {@code endZ}
     * while applying a uniform twist.
     *
     * @param contour 2D contour and optional contour normals
     * @param options up vector, join style, caps, and texturing behavior
     * @param startZ start of the segment
     * @param endZ end of the segment
     * @param twistDegrees total amount of twist in degrees
     * @return generated mesh for the screw
     */
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

    /**
     * Converts normalized RGBA components to an ARGB packed integer.
     *
     * @param red red component in the range {@code [0, 1]}
     * @param green green component in the range {@code [0, 1]}
     * @param blue blue component in the range {@code [0, 1]}
     * @param alpha alpha component in the range {@code [0, 1]}
     * @return packed ARGB color
     */
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
