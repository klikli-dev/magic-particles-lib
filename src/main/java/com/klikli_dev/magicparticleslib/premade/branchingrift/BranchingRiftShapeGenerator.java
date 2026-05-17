// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.branchingrift;

import com.klikli_dev.magicparticleslib.premade.rift.RiftShape;
import com.klikli_dev.magicparticleslib.premade.rift.RiftShapeGenerator;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BranchingRiftShapeGenerator {
    private static final double BRANCH_GROWTH_STEP = 0.12;
    private static final double SUB_BRANCH_GROWTH_STEP = 0.08;
    private static final double BRANCH_MAIN_STEP_LENGTH = 0.18;
    private static final double BRANCH_TIP_STEP_LENGTH = 0.1;
    private static final float BASE_ANGLE_SCALE = 0.33F;
    private static final double DEGENERATE_DIRECTION_EPSILON = 1.0E-8;

    private BranchingRiftShapeGenerator() {
    }

    public static BranchingRiftShape generate(int seed, int skeletonSize, float volume, int branchCount, float jaggedness, float taper) {
        // Reuse the plain rift generator for the trunk so both variants share the same base silhouette language.
        RiftShape trunkSkeleton = RiftShapeGenerator.generate(seed, skeletonSize);
        if (trunkSkeleton.isEmpty()) {
            return BranchingRiftShape.EMPTY;
        }

        ArrayList<BranchingRiftSegment> segments = new ArrayList<>();
        ArrayList<List<Vec3>> absolutePaths = new ArrayList<>();
        // Segment 0 is always the trunk, which makes parent lookup deterministic everywhere else.
        segments.add(new BranchingRiftSegment(trunkSkeleton, BranchingRiftSegment.ROOT_PARENT_SEGMENT, BranchingRiftSegment.ROOT_PARENT_ANCHOR, 0, rootGrowthScale(volume)));
        absolutePaths.add(trunkSkeleton.points());

        if (branchCount <= 0 || trunkSkeleton.points().size() < 4) {
            return new BranchingRiftShape(segments, computeBounds(segments, absolutePaths));
        }

        for (int branchIndex = 0; branchIndex < branchCount; branchIndex++) {
            // Each branch gets its own deterministic RNG stream so adding later branches does not reshape earlier ones.
            RandomSource random = RandomSource.create(mixSeed(seed, branchIndex));
            // Parent selection decides whether growth continues from the trunk or from an existing branch.
            int parentSegmentIndex = pickParentSegmentIndex(branchIndex, segments.size(), random);
            BranchingRiftSegment parentSegment = segments.get(parentSegmentIndex);
            List<Vec3> parentAbsolutePath = absolutePaths.get(parentSegmentIndex);
            // Branch anchors stay away from the segment endpoints so the attachment has room to taper.
            int anchorIndex = pickAnchorIndex(parentSegment.shape(), random);
            // Branch generation returns a path relative to its anchor, not absolute world/entity coordinates.
            RiftShape branch = generateBranch(parentSegment.shape(), anchorIndex, skeletonSize, random, jaggedness, taper, parentSegment.depth());
            if (branch.isEmpty()) {
                continue;
            }

            // growthScale is the branch-specific width budget; lowering volume or increasing branch count thins later branches.
            segments.add(new BranchingRiftSegment(branch, parentSegmentIndex, anchorIndex, parentSegment.depth() + 1, branchGrowthScale(volume, branchIndex, parentSegment.depth() + 1)));
            // Store absolute paths alongside relative segment data so bounds can be accumulated without rebuilding parents.
            absolutePaths.add(makeAbsolutePath(branch.points(), parentAbsolutePath.get(anchorIndex)));
        }

        return new BranchingRiftShape(segments, computeBounds(segments, absolutePaths));
    }

    private static long mixSeed(int seed, int branchIndex) {
        // This mix spreads sequential branch indices over the full bit range, preventing visibly similar siblings.
        // Replacing this with a simpler seed mix will make adjacent branches look more alike.
        long mixed = (((long) seed) << 32) ^ (0x9E3779B97F4A7C15L + branchIndex * 0xBF58476D1CE4E5B9L);
        mixed ^= (mixed >>> 30);
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= (mixed >>> 27);
        mixed *= 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    private static double rootGrowthScale(float volume) {
        return Math.max(0.0, volume);
    }

    private static double branchGrowthScale(float volume, int branchOrder, int depth) {
        // Later and deeper branches consume more of the available volume budget and therefore render thinner.
        return Math.max(0.0, volume - (branchOrder + 1) * BRANCH_GROWTH_STEP - depth * SUB_BRANCH_GROWTH_STEP);
    }

    private static int pickParentSegmentIndex(int branchIndex, int existingSegmentCount, RandomSource random) {
        if (branchIndex <= 1 || existingSegmentCount <= 1) {
            // Force the first few branches onto the trunk so the tree has a readable main structure.
            return 0;
        }

        int priorBranchCount = existingSegmentCount - 1;
        double subBranchChance = Math.min(0.8, 0.3 + branchIndex * 0.08);
        // As more branches are added, bias some new growth toward existing branches to build a tree instead of a starburst.
        if (priorBranchCount > 0 && random.nextDouble() < subBranchChance) {
            // Index 0 is the trunk, so offset by one when deliberately choosing from branch segments only.
            return 1 + random.nextInt(priorBranchCount);
        }

        // Otherwise bias toward earlier segments so the silhouette stays coherent near the trunk.
        int favoredParentCount = Math.min(existingSegmentCount, 1 + Math.max(1, branchIndex / 2));
        return random.nextInt(favoredParentCount);
    }

    private static int pickAnchorIndex(RiftShape parent, RandomSource random) {
        int minIndex = 1;
        int maxIndex = parent.points().size() - 2;
        if (maxIndex <= minIndex) {
            return minIndex;
        }

        // Favor the middle stretch of the parent path, then add jitter so multiple branches do not stack perfectly.
        double fraction = 0.2 + random.nextDouble() * 0.65;
        // Shifting this range closer to 0 or 1 would move branch attachments toward the segment tips.
        int baseIndex = minIndex + (int) Math.round((maxIndex - minIndex) * fraction);
        int jitter = Math.max(1, (maxIndex - minIndex) / 8);
        int candidate = baseIndex + random.nextInt(jitter * 2 + 1) - jitter;
        return Math.max(minIndex, Math.min(maxIndex, candidate));
    }

    private static RiftShape generateBranch(RiftShape parent, int anchorIndex, int skeletonSize, RandomSource random, float jaggedness, float taper, int depth) {
        // Estimate the parent flow direction at the attachment point so the branch can peel away naturally.
        Vec3 tangent = tangentAt(parent.points(), anchorIndex);
        // Start from a sideways vector, then blend some forward motion back in to avoid T-junction looking growth.
        Vec3 lateral = perpendicularDirection(random, tangent);
        double side = random.nextBoolean() ? 1.0 : -1.0;
        // Blend outward and forward motion so branches peel off the trunk instead of turning into right-angle spikes.
        Vec3 direction = lateral.scale(side * (0.75 + random.nextDouble() * 0.45)).add(tangent.scale(0.15 + random.nextDouble() * 0.35));
        if (direction.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
            // If blending cancels out numerically, fall back to a pure sideways launch.
            direction = lateral.scale(side);
        }

        direction = direction.normalize();

        // Deeper branches get fewer steps so the tree naturally thins out instead of growing forever.
        int steps = Math.max(2, 2 + skeletonSize / 10 + random.nextInt(3) - depth);
        // Child branches inherit width from the anchor point, then lose size with depth.
        double initialWidth = parent.widths().get(anchorIndex) * (0.7 + random.nextDouble() * 0.2) * Math.pow(0.78, depth);
        // Higher taper values steepen the width falloff toward the tip.
        double taperExponent = Math.exp(taper * 0.55);
        // Step length controls branch reach; depth scaling keeps sub-branches shorter than primary branches.
        double stepLength = BRANCH_MAIN_STEP_LENGTH * (0.55 + skeletonSize / 24.0) * (0.8 + random.nextDouble() * 0.35) * Math.pow(0.86, depth);

        ArrayList<Vec3> points = new ArrayList<>();
        ArrayList<Double> widths = new ArrayList<>();
        // Branch-local coordinates start at zero because the anchor offset is applied later.
        points.add(Vec3.ZERO);
        widths.add(initialWidth);

        Vec3 current = Vec3.ZERO;
        // Jaggedness scales the random turn angle, while depth slightly increases variation on child branches.
        float angleScale = (float) (BASE_ANGLE_SCALE * Math.exp(jaggedness * 0.35) * (1.0 + depth * 0.08));
        for (int step = 0; step < steps; step++) {
            // More jaggedness means larger random heading changes between samples.
            direction = perturbDirection(direction, random, angleScale);
            current = current.add(direction.scale(stepLength));
            // progress is normalized to [0, 1] across the interior samples so tapering is size-independent.
            double progress = (step + 1.0) / (steps + 1.0);
            // taperExponent reshapes the falloff curve: larger values pinch width closer to the anchor.
            double width = initialWidth * Math.pow(Math.max(0.0, 1.0 - progress), taperExponent);
            points.add(current);
            widths.add(width);
        }

        // The final zero-width tip closes the branch into a point for cleaner extrusion.
        current = current.add(direction.scale(BRANCH_TIP_STEP_LENGTH * Math.pow(0.9, depth)));
        points.add(current);
        widths.add(0.0);

        // Bounds stay relative here because the caller decides where the branch is attached in absolute space.
        return new RiftShape(points, widths, computeSegmentBounds(points, widths, 1.0));
    }

    private static Vec3 tangentAt(List<Vec3> points, int anchorIndex) {
        // Use neighboring samples so the tangent reflects local path flow instead of the global segment direction.
        int previousIndex = Math.max(0, anchorIndex - 1);
        int nextIndex = Math.min(points.size() - 1, anchorIndex + 1);
        Vec3 tangent = points.get(nextIndex).subtract(points.get(previousIndex));
        if (tangent.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
            // Fallback avoids undefined normalization if the neighborhood collapses to a single point.
            return new Vec3(0.0, 1.0, 0.0);
        }

        return tangent.normalize();
    }

    private static Vec3 perpendicularDirection(RandomSource random, Vec3 tangent) {
        Vec3 candidate = initialDirection(random);
        // Project away the tangent so the new branch starts from a genuinely sideways direction.
        Vec3 perpendicular = candidate.subtract(tangent.scale(candidate.dot(tangent)));
        if (perpendicular.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
            // If the random vector happened to align with the tangent, build a fallback from a world-up cross product.
            perpendicular = tangent.cross(new Vec3(0.0, 1.0, 0.0));
        }

        if (perpendicular.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
            // World-up also fails when tangent is vertical, so use the X axis as a second fallback.
            perpendicular = tangent.cross(new Vec3(1.0, 0.0, 0.0));
        }

        return perpendicular.normalize();
    }

    private static Vec3 initialDirection(RandomSource random) {
        Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        if (direction.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
            return new Vec3(0.0, 1.0, 0.0);
        }

        return direction.normalize();
    }

    private static Vec3 perturbDirection(Vec3 direction, RandomSource random, float angleScale) {
        // angleScale is the main jaggedness knob: larger values produce noisier branch trajectories.
        Vec3 perturbed = direction.xRot((float) (random.nextGaussian() * angleScale)).yRot((float) (random.nextGaussian() * angleScale));
        if (perturbed.lengthSqr() <= DEGENERATE_DIRECTION_EPSILON) {
            return direction;
        }

        return perturbed.normalize();
    }

    private static List<Vec3> makeAbsolutePath(List<Vec3> relativePoints, Vec3 origin) {
        ArrayList<Vec3> absolutePoints = new ArrayList<>(relativePoints.size());
        for (Vec3 point : relativePoints) {
            // Relative branch points are translated once from their parent anchor into trunk-relative coordinates.
            absolutePoints.add(origin.add(point));
        }

        return List.copyOf(absolutePoints);
    }

    private static AABB computeBounds(List<BranchingRiftSegment> segments, List<List<Vec3>> absolutePaths) {
        // Bounds are accumulated in absolute space because child segments are stored relative to their anchor.
        AABB firstBounds = computeSegmentBounds(absolutePaths.get(0), segments.get(0).shape().widths(), segments.get(0).growthScale());
        double minX = firstBounds.minX;
        double minY = firstBounds.minY;
        double minZ = firstBounds.minZ;
        double maxX = firstBounds.maxX;
        double maxY = firstBounds.maxY;
        double maxZ = firstBounds.maxZ;

        for (int index = 1; index < segments.size(); index++) {
            AABB bounds = computeSegmentBounds(absolutePaths.get(index), segments.get(index).shape().widths(), segments.get(index).growthScale());
            // Each segment contributes a width-expanded box; unioning them yields the whole branching structure bounds.
            minX = Math.min(minX, bounds.minX);
            minY = Math.min(minY, bounds.minY);
            minZ = Math.min(minZ, bounds.minZ);
            maxX = Math.max(maxX, bounds.maxX);
            maxY = Math.max(maxY, bounds.maxY);
            maxZ = Math.max(maxZ, bounds.maxZ);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static AABB computeSegmentBounds(List<Vec3> points, List<Double> widths, double widthScale) {
        double minX = 0.0;
        double minY = 0.0;
        double minZ = 0.0;
        double maxX = 0.0;
        double maxY = 0.0;
        double maxZ = 0.0;
        boolean first = true;

        for (int index = 0; index < points.size(); index++) {
            Vec3 point = points.get(index);
            double width = widths.get(index) * widthScale;
            if (first) {
                // Seed the segment bounds from the first width-expanded sample.
                minX = point.x - width;
                minY = point.y - width;
                minZ = point.z - width;
                maxX = point.x + width;
                maxY = point.y + width;
                maxZ = point.z + width;
                first = false;
                continue;
            }

            // widthScale lets callers reuse the same helper for both raw geometry and volume-scaled branch bounds.
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
