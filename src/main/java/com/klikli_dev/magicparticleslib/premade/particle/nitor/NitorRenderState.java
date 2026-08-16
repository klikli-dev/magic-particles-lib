// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.nitor;

import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public record NitorRenderState(List<NitorRenderData> entries) implements ParticleGroupRenderState {
    public NitorRenderState {
        entries = List.copyOf(entries);
    }

    @Override
    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (this.entries.isEmpty()) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        poseStack.translate(-camera.pos.x(), -camera.pos.y(), -camera.pos.z());

        // Billboards rotate their quad offsets around the world-space center by the camera orientation,
        // so every mote faces the player. The pose stack already turns world coords camera-relative.
        Quaternionf rotation = new Quaternionf(camera.orientation);

        for (NitorRenderData entry : this.entries) {
            if (!entry.flameQuads().isEmpty()) {
                submitNodeCollector.order(0).submitCustomGeometry(
                        poseStack,
                        RenderTypeRegistry.nitorFlame(),
                        (pose, consumer) -> {
                            for (NitorQuad quad : entry.flameQuads()) {
                                emitQuad(consumer, pose, rotation, quad, entry.light());
                            }
                        }
                );
            }

            if (!entry.coreQuads().isEmpty()) {
                // Both pipelines blend, so both land in the same translucent custom-geometry pass where
                // HashMap iteration order is arbitrary. Rendering the dark core at a higher order bucket
                // guarantees it composites on top of the additive flame (matching golemancy, where the
                // flame draws in the opaque particle pass and the core in the translucent particle pass).
                submitNodeCollector.order(1).submitCustomGeometry(
                        poseStack,
                        RenderTypeRegistry.nitorCore(),
                        (pose, consumer) -> {
                            for (NitorQuad quad : entry.coreQuads()) {
                                emitQuad(consumer, pose, rotation, quad, entry.light());
                            }
                        }
                );
            }
        }
    }

    private static void emitQuad(VertexConsumer consumer, PoseStack.Pose pose, Quaternionf rotation, NitorQuad quad, int light) {
        emitVertex(consumer, pose, rotation, quad, 1.0F, -1.0F, quad.u1(), quad.v1(), light);
        emitVertex(consumer, pose, rotation, quad, 1.0F, 1.0F, quad.u1(), quad.v0(), light);
        emitVertex(consumer, pose, rotation, quad, -1.0F, 1.0F, quad.u0(), quad.v0(), light);
        emitVertex(consumer, pose, rotation, quad, -1.0F, -1.0F, quad.u0(), quad.v1(), light);
    }

    private static void emitVertex(VertexConsumer consumer, PoseStack.Pose pose, Quaternionf rotation, NitorQuad quad, float nx, float ny, float u, float v, int light) {
        // Matches golemancy's QuadParticleRenderState math: rotate the corner offset by the camera
        // orientation, scale it, and add the world-space center. The pose stack applies the
        // -camera.pos translation so the buffer holds camera-relative positions.
        Vector3f corner = new Vector3f(nx, ny, 0.0F).rotate(rotation).mul(quad.scale()).add(quad.x(), quad.y(), quad.z());
        consumer.addVertex(pose, corner.x(), corner.y(), corner.z())
                .setUv(u, v)
                .setColor(quad.color())
                .setLight(light);
    }
}