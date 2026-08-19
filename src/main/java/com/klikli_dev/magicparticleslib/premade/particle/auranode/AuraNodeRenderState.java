// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.auranode;

import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public record AuraNodeRenderState(List<AuraNodeRenderData> entries) implements ParticleGroupRenderState {
    public AuraNodeRenderState {
        entries = List.copyOf(entries);
    }

    @Override
    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (this.entries.isEmpty()) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        poseStack.translate(-camera.pos.x(), -camera.pos.y(), -camera.pos.z());

        // Billboards rotate their quad offsets around the world-space center by the camera
        // orientation, so every layer faces the player. Each layer is additionally rotated
        // around the billboard's view axis by its own strand rotation before the camera
        // rotation is applied. The pose stack already turns world coords camera-relative.
        Quaternionf rotation = new Quaternionf(camera.orientation);

        if (this.hasLayers(true)) {
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypeRegistry.auraNode(),
                    (pose, consumer) -> this.emitLayers(consumer, pose, rotation, true)
            );
        }
        if (this.hasLayers(false)) {
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypeRegistry.auraNodeNormalBlend(),
                    (pose, consumer) -> this.emitLayers(consumer, pose, rotation, false)
            );
        }
    }

    private boolean hasLayers(boolean additive) {
        for (AuraNodeRenderData entry : this.entries) {
            for (AuraNodeQuad quad : entry.quads()) {
                if (quad.additive() == additive) {
                    return true;
                }
            }
        }
        return false;
    }

    private void emitLayers(VertexConsumer consumer, PoseStack.Pose pose, Quaternionf rotation, boolean additive) {
        for (AuraNodeRenderData entry : this.entries) {
            for (AuraNodeQuad quad : entry.quads()) {
                if (quad.additive() == additive) {
                    emitQuad(consumer, pose, rotation, quad, entry.light());
                }
            }
        }
    }

    private static void emitQuad(VertexConsumer consumer, PoseStack.Pose pose, Quaternionf rotation, AuraNodeQuad quad, int light) {
        emitVertex(consumer, pose, rotation, quad, 1.0F, -1.0F, quad.u1(), quad.v1(), light);
        emitVertex(consumer, pose, rotation, quad, 1.0F, 1.0F, quad.u1(), quad.v0(), light);
        emitVertex(consumer, pose, rotation, quad, -1.0F, 1.0F, quad.u0(), quad.v0(), light);
        emitVertex(consumer, pose, rotation, quad, -1.0F, -1.0F, quad.u0(), quad.v1(), light);
    }

    private static void emitVertex(VertexConsumer consumer, PoseStack.Pose pose, Quaternionf rotation, AuraNodeQuad quad, float nx, float ny, float u, float v, int light) {
        Vector3f corner = new Vector3f(nx, ny, 0.0F)
                .rotateZ(quad.rotation())
                .rotate(rotation)
                .mul(quad.edge())
                .add(quad.x(), quad.y(), quad.z());
        consumer.addVertex(pose, corner.x(), corner.y(), corner.z())
                .setUv(u, v)
                .setColor(quad.color())
                .setLight(light);
    }
}