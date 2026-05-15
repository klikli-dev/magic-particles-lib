// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.branchingrift;

import com.klikli_dev.magicparticleslib.extrusion.Extrusion;
import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.extrusion.ExtrusionOptions;
import com.klikli_dev.magicparticleslib.extrusion.JoinStyle;
import com.klikli_dev.magicparticleslib.extrusion.NormalStyle;
import com.klikli_dev.magicparticleslib.premade.rift.RiftShape;
import com.klikli_dev.magicparticleslib.premade.rift.RiftVisualProfile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BranchingRiftMeshBuilder {
    private static final ExtrusionOptions OPTIONS = Extrusion.options()
            .joinStyle(JoinStyle.ANGLE)
            .normalStyle(NormalStyle.PATH_EDGE)
            .capEnds(false)
            .tubeSegments(6)
            .build();

    private BranchingRiftMeshBuilder() {
    }

    public static List<List<Vec3>> animatePaths(BranchingRiftShape shape, float ageInTicks, float visualIntensity) {
        if (shape.isEmpty()) {
            return List.of();
        }

        ArrayList<List<Vec3>> animatedPaths = new ArrayList<>(shape.segments().size());
        for (int segmentIndex = 0; segmentIndex < shape.segments().size(); segmentIndex++) {
            BranchingRiftSegment segment = shape.segments().get(segmentIndex);
            // Child segments start from a point on the already-animated parent path, not the static skeleton.
            Vec3 origin = segment.isRoot()
                    ? Vec3.ZERO
                    : animatedPaths.get(segment.parentSegmentIndex()).get(segment.parentAnchorIndex());
            animatedPaths.add(animatePath(segment, origin, ageInTicks, visualIntensity, segmentIndex));
        }

        return List.copyOf(animatedPaths);
    }

    public static ExtrusionMesh build(BranchingRiftSegment segment, List<Vec3> animatedPath, float ageInTicks, float visualIntensity, double widthScale, int segmentIndex) {
        RiftShape shape = segment.shape();
        if (shape.isEmpty() || animatedPath.size() < 2 || segment.growthScale() <= 0.0) {
            return ExtrusionMesh.EMPTY;
        }

        // Branches pulse from their anchor outward so the trunk connection stays visually stable.
        int centerIndex = RiftVisualProfile.centerIndex(shape.points().size(), !segment.isRoot());
        double wobbleStrength = RiftVisualProfile.wobbleStrength(visualIntensity);
        ArrayList<Double> animatedRadii = animateRadii(shape, segment, ageInTicks, centerIndex, wobbleStrength, widthScale, segmentIndex);

        return Extrusion.engine().polyCone(animatedPath, List.copyOf(animatedRadii), OPTIONS);
    }

    private static List<Vec3> animatePath(BranchingRiftSegment segment, Vec3 origin, float ageInTicks, float visualIntensity, int segmentIndex) {
        RiftShape shape = segment.shape();
        if (shape.isEmpty() || shape.points().size() < 2) {
            return List.of();
        }

        // Deeper branches get slightly calmer motion so the silhouette does not become visual noise.
        double wobbleStrength = RiftVisualProfile.wobbleStrength(visualIntensity) * Math.pow(0.92, segment.depth());
        int centerIndex = RiftVisualProfile.centerIndex(shape.points().size(), !segment.isRoot());
        ArrayList<Vec3> animatedPath = new ArrayList<>(shape.points().size());
        for (int pointIndex = 0; pointIndex < shape.points().size(); pointIndex++) {
            Vec3 point = segment.isRoot() ? shape.points().get(pointIndex) : origin.add(shape.points().get(pointIndex));
            if (!segment.isRoot() && pointIndex == 0) {
                // Keep the first point exactly on the parent anchor to avoid cracks between segments.
                animatedPath.add(origin);
                continue;
            }

            double phase = RiftVisualProfile.phase(ageInTicks, segmentIndex, pointIndex, centerIndex);
            animatedPath.add(point.add(RiftVisualProfile.wobbleOffset(phase, wobbleStrength)));
        }

        return List.copyOf(animatedPath);
    }

    private static ArrayList<Double> animateRadii(RiftShape shape, BranchingRiftSegment segment, float ageInTicks, int centerIndex, double wobbleStrength, double widthScale, int segmentIndex) {
        ArrayList<Double> animatedRadii = new ArrayList<>(shape.widths().size());
        for (int pointIndex = 0; pointIndex < shape.widths().size(); pointIndex++) {
            double phase = RiftVisualProfile.phase(ageInTicks, segmentIndex, pointIndex, centerIndex);
            double radiusMultiplier = RiftVisualProfile.radiusPulse(phase, wobbleStrength);
            // growthScale lets later or deeper branches visibly taper away as the volume budget runs out.
            animatedRadii.add(shape.widths().get(pointIndex) * segment.growthScale() * radiusMultiplier * widthScale);
        }

        return animatedRadii;
    }
}
