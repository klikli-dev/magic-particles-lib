// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.essentiastream;

import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;

import java.util.List;

/**
 * One frame of every visible essentia stream. All streams are submitted through the same render type,
 * so they are drawn in a single batch.
 */
public record EssentiaStreamRenderState(List<ExtrusionMesh> meshes) implements ParticleGroupRenderState {
    public EssentiaStreamRenderState {
        meshes = List.copyOf(meshes);
    }

    @Override
    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (this.meshes.isEmpty()) {
            return;
        }

        // Meshes are built in world space, so the camera offset is applied through the pose.
        PoseStack poseStack = new PoseStack();
        poseStack.translate(-camera.pos.x(), -camera.pos.y(), -camera.pos.z());

        for (ExtrusionMesh mesh : this.meshes) {
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypeRegistry.essentiaStream(),
                    (pose, consumer) -> mesh.emit(consumer, pose, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
            );
        }
    }
}
