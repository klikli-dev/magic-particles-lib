// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.rift;

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
        int mainSteps = Mth.ceil(size / 3.0F);
        double initialWidth = size / 300.0;
        double widthStep = initialWidth / (mainSteps + 1.0);

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
            positiveDirection = perturbDirection(positiveDirection, random);
            negativeDirection = perturbDirection(negativeDirection, random);

            positivePosition = positivePosition.add(positiveDirection.scale(MAIN_STEP_LENGTH));
            negativePosition = negativePosition.add(negativeDirection.scale(MAIN_STEP_LENGTH));

            double width = Math.max(0.0, initialWidth - widthStep * (step + 1));
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
        Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        if (direction.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
            return new Vec3(0.0, 1.0, 0.0);
        }

        return direction.normalize();
    }

    private static Vec3 perturbDirection(Vec3 direction, RandomSource random) {
        // Rotate around two axes instead of replacing the vector outright to preserve smooth forward growth.
        Vec3 perturbed = direction.xRot((float) (random.nextGaussian() * ANGLE_SCALE)).yRot((float) (random.nextGaussian() * ANGLE_SCALE));
        if (perturbed.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
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
