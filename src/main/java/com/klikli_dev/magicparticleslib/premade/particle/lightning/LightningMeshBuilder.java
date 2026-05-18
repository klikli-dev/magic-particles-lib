// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.lightning;

import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.extrusion.ExtrusionQuad;
import com.klikli_dev.magicparticleslib.extrusion.ExtrusionVertex;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class LightningMeshBuilder {
    // Converts logical width samples into strip half-width.
    // Raising it makes the ribbons thicker and more opaque-looking.
    private static final double WIDTH_TO_HALF_WIDTH = 0.17D;

    private LightningMeshBuilder() {
    }

    static ExtrusionMesh buildVerticalStrip(LightningShape shape, int color, double widthScale) {
        // Straight up/down offset reproduces the first legacy lightning sheet.
        return build(shape, color, new Vec3(0.0D, -1.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D), widthScale);
    }

    static ExtrusionMesh buildDiagonalStrip(LightningShape shape, int color, double widthScale) {
        // Diagonal x/z offset reproduces the second crossed sheet from the original effect.
        return build(shape, color, new Vec3(-1.0D, 0.0D, -1.0D), new Vec3(1.0D, 0.0D, 1.0D), widthScale);
    }

    private static ExtrusionMesh build(LightningShape shape, int color, Vec3 negativeAxis, Vec3 positiveAxis, double widthScale) {
        if (!shape.isRenderable()) {
            return ExtrusionMesh.EMPTY;
        }

        ArrayList<ExtrusionQuad> quads = new ArrayList<>(Math.max(0, shape.pointCount() - 1));
        // Use start-to-end distance like the original mesh builder so U scroll stays continuous across the full bolt.
        float lightningLength = Math.max(1.0F, (float) shape.point(0).distanceTo(shape.point(shape.pointCount() - 1)));

        for (int index = 0; index < shape.pointCount() - 1; index++) {
            Vec3 from = shape.point(index);
            Vec3 to = shape.point(index + 1);
            // Sample the smoothed logical widths so adjacent quads do not visibly pop in thickness.
            double fromHalfWidth = Math.max(0.0D, shape.sampledWidth(index) * WIDTH_TO_HALF_WIDTH * widthScale);
            double toHalfWidth = Math.max(0.0D, shape.sampledWidth(index + 1) * WIDTH_TO_HALF_WIDTH * widthScale);
            // Scale the strip axes independently at both ends so the ribbon can taper with the shape.
            Vec3 fromNegative = from.add(negativeAxis.scale(fromHalfWidth));
            Vec3 fromPositive = from.add(positiveAxis.scale(fromHalfWidth));
            Vec3 toNegative = to.add(negativeAxis.scale(toHalfWidth));
            Vec3 toPositive = to.add(positiveAxis.scale(toHalfWidth));
            // Keep the U coordinate continuous along the whole lightning length rather than restarting per segment.
            float u0 = index / lightningLength;
            float u1 = (index + 1) / lightningLength;
            // Give the whole strip a stable face normal. Additive rendering means this is mostly just a formality.
            Vec3 faceNormal = to.subtract(from).cross(fromPositive.subtract(fromNegative)).normalize();
            if (faceNormal.lengthSqr() < 1.0E-10D) {
                faceNormal = new Vec3(0.0D, 1.0D, 0.0D);
            }

            quads.add(new ExtrusionQuad(
                    vertex(fromNegative, faceNormal, u0, 1.0F, color),
                    vertex(fromPositive, faceNormal, u0, 0.0F, color),
                    vertex(toPositive, faceNormal, u1, 0.0F, color),
                    vertex(toNegative, faceNormal, u1, 1.0F, color)
            ));
        }

        return quads.isEmpty() ? ExtrusionMesh.EMPTY : new ExtrusionMesh(List.copyOf(quads), List.of());
    }

    private static ExtrusionVertex vertex(Vec3 position, Vec3 normal, float u, float v, int color) {
        return new ExtrusionVertex(
                (float) position.x(),
                (float) position.y(),
                (float) position.z(),
                (float) normal.x(),
                (float) normal.y(),
                (float) normal.z(),
                u,
                v,
                color
        );
    }
}
