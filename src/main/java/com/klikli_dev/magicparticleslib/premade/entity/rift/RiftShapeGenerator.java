// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.entity.rift;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RiftShapeGenerator {
    private static final double MAIN_STEP_LENGTH = 0.2;
    private static final double TIP_STEP_LENGTH = 0.1;
    private static final float ANGLE_SCALE = 0.33F;
    private static final double DEGENERATE_DIRECTION_EPSILON = 1.0E-8;

    private RiftShapeGenerator() {
    }

    public static RiftShape generate(int seed, int size) {
        if (size <= 0) {
            return RiftShape.EMPTY;
        }

        RandomSource random = RandomSource.create(seed);
        // Larger requested sizes translate into more center-out growth steps.
        int mainSteps = Mth.ceil(size / 3.0F);
        // Width scales gently with size so large rifts feel fuller without exploding in thickness.
        double initialWidth = size / 300.0;
        // Each growth step consumes an equal slice of width budget until the ends taper out.
        double widthStep = initialWidth / (mainSteps + 1.0);

        // The whole rift is generated from one random heading, then mirrored by reversing it.
        Vec3 positiveDirection = initialDirection(random);
        Vec3 negativeDirection = positiveDirection.reverse();
        Vec3 positivePosition = Vec3.ZERO;
        Vec3 negativePosition = Vec3.ZERO;

        ArrayList<Vec3> points = new ArrayList<>();
        ArrayList<Double> widths = new ArrayList<>();
        points.add(Vec3.ZERO);
        widths.add(initialWidth);

        // Grow both halves away from the center so the final path is symmetric around the spawn point.
        for (int step = 0; step < mainSteps; step++) {
            // Perturbation bends the path a little each step; increasing ANGLE_SCALE makes rifts more chaotic.
            positiveDirection = perturbDirection(positiveDirection, random);
            negativeDirection = perturbDirection(negativeDirection, random);

            // Step length controls overall rift length. Larger values stretch the silhouette quickly.
            positivePosition = positivePosition.add(positiveDirection.scale(MAIN_STEP_LENGTH));
            negativePosition = negativePosition.add(negativeDirection.scale(MAIN_STEP_LENGTH));

            // Width shrinks toward the ends so the later extrusion has a natural taper.
            double width = Math.max(0.0, initialWidth - widthStep * (step + 1));
            // Insert the negative half at the front so the final list runs from one tip to the other.
            points.add(0, negativePosition);
            widths.add(0, width);
            points.add(positivePosition);
            widths.add(width);
        }

        // Add a short zero-width tip on each end so the extrusion closes into a cleaner point.
        negativePosition = negativePosition.add(negativeDirection.scale(TIP_STEP_LENGTH));
        positivePosition = positivePosition.add(positiveDirection.scale(TIP_STEP_LENGTH));
        points.add(0, negativePosition);
        widths.add(0, 0.0);
        points.add(positivePosition);
        widths.add(0.0);

        return new RiftShape(List.copyOf(points), List.copyOf(widths), computeBounds(points, widths));
    }

    private static Vec3 initialDirection(RandomSource random) {
        // Gaussian components bias directions toward smooth, organic variation instead of axis-aligned picks.
        Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        if (direction.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
            // Fallback prevents divide-by-zero normalization if the random vector is effectively empty.
            return new Vec3(0.0, 1.0, 0.0);
        }

        return direction.normalize();
    }

    private static Vec3 perturbDirection(Vec3 direction, RandomSource random) {
        // Rotate around two axes instead of replacing the vector outright to preserve smooth forward growth.
        // Raising ANGLE_SCALE produces sharper bends; lowering it makes the rift straighter.
        Vec3 perturbed = direction.xRot((float) (random.nextGaussian() * ANGLE_SCALE)).yRot((float) (random.nextGaussian() * ANGLE_SCALE));
        if (perturbed.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
            // Keep the previous direction if the perturbation collapses toward zero.
            return direction;
        }

        return perturbed.normalize();
    }

    private static AABB computeBounds(List<Vec3> points, List<Double> widths) {
        double minX = 0.0;
        double minY = 0.0;
        double minZ = 0.0;
        double maxX = 0.0;
        double maxY = 0.0;
        double maxZ = 0.0;
        boolean first = true;

        for (int index = 0; index < points.size(); index++) {
            Vec3 point = points.get(index);
            double width = widths.get(index);
            if (first) {
                // Seed the bounds from the first point so later iterations can use simple min/max expansion.
                minX = point.x - width;
                minY = point.y - width;
                minZ = point.z - width;
                maxX = point.x + width;
                maxY = point.y + width;
                maxZ = point.z + width;
                first = false;
                continue;
            }

            // Width expands each point into a local box because the rendered mesh extends equally in all directions.
            minX = Math.min(minX, point.x - width);
            minY = Math.min(minY, point.y - width);
            minZ = Math.min(minZ, point.z - width);
            maxX = Math.max(maxX, point.x + width);
            maxY = Math.max(maxY, point.y + width);
            maxZ = Math.max(maxZ, point.z + width);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
