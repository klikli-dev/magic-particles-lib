// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.entity.rift;

import com.klikli_dev.magicparticleslib.extrusion.Extrusion;
import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.extrusion.ExtrusionOptions;
import com.klikli_dev.magicparticleslib.extrusion.JoinStyle;
import com.klikli_dev.magicparticleslib.extrusion.NormalStyle;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RiftMeshBuilder {
    private static final ExtrusionOptions OPTIONS = Extrusion.options()
            .joinStyle(JoinStyle.ANGLE)
            .normalStyle(NormalStyle.PATH_EDGE)
            .capEnds(false)
            .tubeSegments(6)
            .build();

    private RiftMeshBuilder() {
    }

    public static ExtrusionMesh build(RiftShape shape, float ageInTicks, float visualIntensity, double widthScale) {
        if (shape.isEmpty() || shape.points().size() < 2) {
            return ExtrusionMesh.EMPTY;
        }

        // Plain rifts wobble outwards from their center so both ends feel equally alive.
        int centerIndex = RiftVisualProfile.centerIndex(shape.points().size(), false);
        // visualIntensity only affects animation here, so widening the mesh still stays deterministic per pass.
        double wobbleStrength = RiftVisualProfile.wobbleStrength(visualIntensity);
        ArrayList<Vec3> animatedPath = animatePath(shape.points(), ageInTicks, centerIndex, wobbleStrength);
        ArrayList<Double> animatedRadii = animateRadii(shape.widths(), ageInTicks, centerIndex, wobbleStrength, widthScale);

        // polyCone interprets each point/radius pair as a tube spine sample; path and radii must stay aligned.
        return Extrusion.engine().polyCone(List.copyOf(animatedPath), List.copyOf(animatedRadii), OPTIONS);
    }

    private static ArrayList<Vec3> animatePath(List<Vec3> sourcePath, float ageInTicks, int centerIndex, double wobbleStrength) {
        ArrayList<Vec3> animatedPath = new ArrayList<>(sourcePath.size());
        for (int pointIndex = 0; pointIndex < sourcePath.size(); pointIndex++) {
            Vec3 point = sourcePath.get(pointIndex);
            // Offsetting phase by distance from the center makes the motion travel symmetrically along the rift.
            double phase = RiftVisualProfile.phase(ageInTicks, pointIndex, centerIndex);
            // Higher wobbleStrength increases displacement on all axes because wobbleOffset scales its sine waves.
            animatedPath.add(point.add(RiftVisualProfile.wobbleOffset(phase, wobbleStrength)));
        }

        return animatedPath;
    }

    private static ArrayList<Double> animateRadii(List<Double> sourceRadii, float ageInTicks, int centerIndex, double wobbleStrength, double widthScale) {
        ArrayList<Double> animatedRadii = new ArrayList<>(sourceRadii.size());
        for (int pointIndex = 0; pointIndex < sourceRadii.size(); pointIndex++) {
            double phase = RiftVisualProfile.phase(ageInTicks, pointIndex, centerIndex);
            // Width pulsing is kept in phase with the positional wobble so the mesh feels coherent.
            double radiusMultiplier = RiftVisualProfile.radiusPulse(phase, wobbleStrength);
            // widthScale is the render-pass multiplier; changing it affects halo/core layering, not base shape data.
            animatedRadii.add(sourceRadii.get(pointIndex) * radiusMultiplier * widthScale);
        }

        return animatedRadii;
    }
}
