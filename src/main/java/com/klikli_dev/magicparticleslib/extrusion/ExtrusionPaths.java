// SPDX-FileCopyrightText: 1990, 1991, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for translating upstream array-shaped inputs into the modern typed API.
 */
public final class ExtrusionPaths {
    private ExtrusionPaths() {
    }

    /**
     * Converts a polyline vertex array to a typed path.
     *
     * @param values polyline vertices as {@code [n][3]}
     * @return immutable list of path vertices
     */
    public static List<Vec3> fromArray(double[][] values) {
        ArrayList<Vec3> points = new ArrayList<>(values.length);
        for (double[] value : values) {
            if (value == null || value.length < 3) {
                continue;
            }
            points.add(new Vec3(value[0], value[1], value[2]));
        }
        return List.copyOf(points);
    }

    /**
     * Converts upstream contour arrays into a typed contour.
     *
     * @param points 2D contour points as {@code [n][2]}
     * @param normals 2D contour normals as {@code [n][2]}
     * @param closed whether the contour is closed or open
     * @return contour with generated normals where missing
     */
    public static Contour2D contourFromArrays(double[][] points, double[][] normals, boolean closed) {
        ArrayList<ContourPoint> contour = new ArrayList<>(points.length);
        for (int index = 0; index < points.length; index++) {
            double[] point = points[index];
            if (point == null || point.length < 2) {
                continue;
            }

            Vec2d normal = null;
            if (normals != null && index < normals.length && normals[index] != null && normals[index].length >= 2) {
                normal = new Vec2d(normals[index][0], normals[index][1]);
            }
            contour.add(new ContourPoint(new Vec2d(point[0], point[1]), normal));
        }
        return Contour2D.of(List.copyOf(contour), closed);
    }

    /**
     * Converts upstream path arrays into typed path samples.
     *
     * @param path polyline vertices as {@code [n][3]}
     * @param colors colors at polyline vertices as {@code [n][4]}
     * @param transforms 2D contour transforms as {@code [n][2][3]}
     * @param twists contour twists in degrees, one per path point
     * @return immutable list of typed path points
     */
    public static List<PathPoint> pointsFromArrays(double[][] path, float[][] colors, double[][][] transforms, double[] twists) {
        ArrayList<PathPoint> result = new ArrayList<>(path.length);
        for (int index = 0; index < path.length; index++) {
            double[] point = path[index];
            if (point == null || point.length < 3) {
                continue;
            }

            PathPoint pathPoint = PathPoint.of(new Vec3(point[0], point[1], point[2]));
            if (colors != null && index < colors.length && colors[index] != null && colors[index].length >= 4) {
                float[] color = colors[index];
                pathPoint = pathPoint.withColor(ExtrusionEngine.argb(color[0], color[1], color[2], color[3]));
            }
            if (transforms != null && index < transforms.length) {
                pathPoint = pathPoint.withContourTransform(AffineTransform2D.fromLegacyMatrix(transforms[index]));
            }
            if (twists != null && index < twists.length) {
                pathPoint = pathPoint.withTwistDegrees(twists[index]);
            }
            result.add(pathPoint);
        }
        return List.copyOf(result);
    }
}
