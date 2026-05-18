// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.lightning;

import com.mojang.blaze3d.vertex.PoseStack;
import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.List;

public record LightningRenderState(List<LightningRenderData> entries) implements ParticleGroupRenderState {
    public LightningRenderState {
        entries = List.copyOf(entries);
    }

    @Override
    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (this.entries.isEmpty()) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        poseStack.translate(-camera.pos.x(), -camera.pos.y(), -camera.pos.z());

        for (LightningRenderData entry : this.entries) {
            if (!entry.haloMesh().isEmpty()) {
                submitNodeCollector.submitCustomGeometry(
                        poseStack,
                        RenderTypeRegistry.lightningHalo(),
                        (pose, consumer) -> entry.haloMesh().emit(consumer, pose, entry.light(), OverlayTexture.NO_OVERLAY)
                );
            }

            if (!entry.coreMesh().isEmpty()) {
                submitNodeCollector.submitCustomGeometry(
                        poseStack,
                        RenderTypeRegistry.lightningCore(),
                        (pose, consumer) -> entry.coreMesh().emit(consumer, pose, entry.light(), OverlayTexture.NO_OVERLAY)
                );
            }
        }
    }
}
