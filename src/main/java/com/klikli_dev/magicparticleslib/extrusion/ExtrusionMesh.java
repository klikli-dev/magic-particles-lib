// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.extrusion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

public record ExtrusionMesh(List<ExtrusionQuad> quads, List<ExtrusionTriangle> triangles) {
    public static final ExtrusionMesh EMPTY = new ExtrusionMesh(List.of(), List.of());

    public ExtrusionMesh {
        quads = List.copyOf(quads);
        triangles = List.copyOf(triangles);
    }

    public boolean isEmpty() {
        return this.quads.isEmpty() && this.triangles.isEmpty();
    }

    public List<ExtrusionTriangle> triangulated() {
        ArrayList<ExtrusionTriangle> result = new ArrayList<>(this.triangles.size() + this.quads.size() * 2);
        result.addAll(this.triangles);
        for (ExtrusionQuad quad : this.quads) {
            result.add(new ExtrusionTriangle(quad.first(), quad.second(), quad.third()));
            result.add(new ExtrusionTriangle(quad.first(), quad.third(), quad.fourth()));
        }
        return List.copyOf(result);
    }

    public void emit(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay) {
        for (ExtrusionTriangle triangle : triangulated()) {
            triangle.first().emit(consumer, pose, light, overlay);
            triangle.second().emit(consumer, pose, light, overlay);
            triangle.third().emit(consumer, pose, light, overlay);
        }
    }
}
