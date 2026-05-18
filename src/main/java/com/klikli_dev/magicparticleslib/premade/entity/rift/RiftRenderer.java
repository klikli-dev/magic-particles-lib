// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.entity.rift;

import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.premade.entity.rift.RiftEntity;
import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

public class RiftRenderer extends EntityRenderer<RiftEntity, RiftRenderState> {
    public RiftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    public RiftRenderState createRenderState() {
        return new RiftRenderState();
    }

    @Override
    public void extractRenderState(RiftEntity entity, RiftRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.shape = entity.shape();
        state.visualIntensity = entity.visualIntensity();
    }

    @Override
    public Vec3 getRenderOffset(RiftRenderState state) {
        return Vec3.ZERO;
    }

    @Override
    public void submit(RiftRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.shape.isEmpty()) {
            // Render several widened halo shells first, then a final core pass to give the rift some depth.
            for (int passIndex = 0; passIndex < RiftVisualProfile.passCount(); passIndex++) {
                // Every pass rebuilds the same animated spine with a different width multiplier.
                ExtrusionMesh mesh = RiftMeshBuilder.build(
                        state.shape,
                        state.ageInTicks,
                        state.visualIntensity,
                        RiftVisualProfile.widthScale(passIndex)
                );
                if (mesh.isEmpty()) {
                    continue;
                }

                submitNodeCollector.submitCustomGeometry(
                        poseStack,
                        // Halo passes use the softer additive pipeline; the last pass uses the brighter portal core.
                        RiftVisualProfile.haloPass(passIndex) ? RenderTypeRegistry.riftHalo() : RenderTypeRegistry.riftPortal(),
                        // The mesh is already built in local space, so we only need to stream its vertices here.
                        (pose, consumer) -> mesh.emit(consumer, pose, state.lightCoords, OverlayTexture.NO_OVERLAY)
                );
            }
        }

        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
