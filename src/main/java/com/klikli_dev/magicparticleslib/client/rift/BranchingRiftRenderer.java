// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.client.rift;

import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.premade.rift.BranchingRiftEntity;
import com.klikli_dev.magicparticleslib.premade.rift.BranchingRiftSegment;
import com.klikli_dev.magicparticleslib.registry.MPLRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BranchingRiftRenderer extends EntityRenderer<BranchingRiftEntity, BranchingRiftRenderState> {
    public BranchingRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    public BranchingRiftRenderState createRenderState() {
        return new BranchingRiftRenderState();
    }

    @Override
    public void extractRenderState(BranchingRiftEntity entity, BranchingRiftRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.shape = entity.shape();
        state.visualIntensity = entity.visualIntensity();
    }

    @Override
    public Vec3 getRenderOffset(BranchingRiftRenderState state) {
        return Vec3.ZERO;
    }

    @Override
    public void submit(BranchingRiftRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.shape.isEmpty()) {
            List<List<Vec3>> animatedPaths = BranchingRiftMeshBuilder.animatePaths(state.shape, state.ageInTicks, state.visualIntensity);
            for (int passIndex = 0; passIndex < RiftVisualProfile.passCount(); passIndex++) {
                for (int segmentIndex = 0; segmentIndex < state.shape.segments().size(); segmentIndex++) {
                    BranchingRiftSegment segment = state.shape.segments().get(segmentIndex);
                    ExtrusionMesh mesh = BranchingRiftMeshBuilder.build(
                            segment,
                            animatedPaths.get(segmentIndex),
                            state.ageInTicks,
                            state.visualIntensity,
                            RiftVisualProfile.widthScale(passIndex),
                            segmentIndex
                    );
                    if (mesh.isEmpty()) {
                        continue;
                    }

                    submitNodeCollector.submitCustomGeometry(
                            poseStack,
                            RiftVisualProfile.haloPass(passIndex) ? MPLRenderTypes.halo() : MPLRenderTypes.portal(),
                            (pose, consumer) -> mesh.emit(consumer, pose, state.lightCoords, OverlayTexture.NO_OVERLAY)
                    );
                }
            }
        }

        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
