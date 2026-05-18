// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.electricarc;

import com.klikli_dev.magicparticleslib.extrusion.AffineTransform2D;
import com.klikli_dev.magicparticleslib.extrusion.Contour2D;
import com.klikli_dev.magicparticleslib.extrusion.Extrusion;
import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.extrusion.ExtrusionOptions;
import com.klikli_dev.magicparticleslib.extrusion.JoinStyle;
import com.klikli_dev.magicparticleslib.extrusion.NormalStyle;
import com.klikli_dev.magicparticleslib.extrusion.PathPoint;
import com.klikli_dev.magicparticleslib.extrusion.TextureCoordinateMode;

import java.util.ArrayList;

final class ElectricArcMeshBuilder {
    // Five-sided circular approximation. More sides look rounder but cost more vertices.
    private static final Contour2D TUBE_CONTOUR = Contour2D.circle(1.0, 5);
    // Extrusion settings for the beam mesh.
    // tubeSegments controls roundness along the contour, capEnds seals both ends,
    // and VERTEX_CYLINDER keeps the texture stretched along arc length like a tube.
    private static final ExtrusionOptions OPTIONS = Extrusion.options()
            .joinStyle(JoinStyle.ANGLE)
            .normalStyle(NormalStyle.PATH_EDGE)
            .capEnds(true)
            .textureMode(TextureCoordinateMode.VERTEX_CYLINDER)
            .tubeSegments(5)
            .textureLengthScale(1.0)
            .textureLengthOffset(0.0)
            .build();
    // Converts logical width samples into extrusion radius.
    // Increase to make every sample produce a thicker mesh.
    private static final double WIDTH_TO_RADIUS = 0.1D;

    private ElectricArcMeshBuilder() {
    }

    // Converts a sampled centerline into a closed extrusion mesh.
    // radiusScale lets the caller reuse the same path for halo and core passes.
    static ExtrusionMesh build(ElectricArcShape shape, int color, double radiusScale) {
        ArrayList<PathPoint> points = new ArrayList<>(shape.pointCount());

        for (int index = 0; index < shape.pointCount(); index++) {
            // sampledWidth() lightly smooths neighboring widths so abrupt width noise doesn't create
            // ugly hard rings in the mesh.
            double baseRadius = shape.sampledWidth(index) * WIDTH_TO_RADIUS;
            double radius = Math.max(0.0D, baseRadius * radiusScale);
            points.add(PathPoint.of(shape.point(index))
                    .withColor(color)
                    .withContourTransform(AffineTransform2D.scale(radius, radius)));
        }

        // Extrude the contour through every path sample to build triangles for rendering.
        return Extrusion.engine().extrude(TUBE_CONTOUR, points, OPTIONS);
    }
}
