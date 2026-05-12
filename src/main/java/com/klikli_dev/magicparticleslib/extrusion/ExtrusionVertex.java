// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.extrusion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public record ExtrusionVertex(float x, float y, float z, float normalX, float normalY, float normalZ, float u, float v, int color) {
    public void emit(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay) {
        consumer.addVertex(pose, this.x, this.y, this.z)
                .setColor(this.color)
                .setUv(this.u, this.v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, this.normalX, this.normalY, this.normalZ);
    }
}
