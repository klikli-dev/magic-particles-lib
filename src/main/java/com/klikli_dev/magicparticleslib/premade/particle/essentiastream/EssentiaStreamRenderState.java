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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * One frame of every visible essentia stream. All streams are submitted through the same render type,
 * so they are drawn in a single batch.
 */
public record EssentiaStreamRenderState(List<Entry> entries) implements ParticleGroupRenderState {
    public EssentiaStreamRenderState {
        entries = List.copyOf(entries);
    }

    @Override
    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (this.entries.isEmpty()) {
            return;
        }

        for (Entry entry : this.entries) {
            // The mesh is relative to the stream origin, so only origin - camera goes into the pose.
            // Both the offset and the mesh stay small, which keeps float precision good far from the world origin.
            PoseStack poseStack = new PoseStack();
            Vec3 origin = entry.origin();
            poseStack.translate(origin.x - camera.pos.x(), origin.y - camera.pos.y(), origin.z - camera.pos.z());

            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypeRegistry.essentiaStream(),
                    (pose, consumer) -> entry.mesh().emit(consumer, pose, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
            );
        }
    }

    /**
     * One stream's mesh and the world-space point the mesh is relative to.
     */
    public record Entry(ExtrusionMesh mesh, Vec3 origin) {
    }
}
