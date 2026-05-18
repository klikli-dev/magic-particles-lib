// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.lightning;

import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LightningParticle extends Particle {
    // Halo radius relative to the logical width.
    // Larger values make the lightning look chunkier and more solid.
    private static final double WIDTH_TO_RADIUS = 0.13D;
    // Minimum alpha while alive.
    // Raising this makes the bolt hang around brighter near the end of its life.
    private static final float MIN_ALPHA = 0.08F;

    private final Vec3 start;
    private final Vec3 end;
    private final int rgbColor;
    private final float heightGain;
    private final float width;
    private final int seed;
    private final double distance;
    private final double maxDisplacement;
    private final LightningShape shape;

    public LightningParticle(ClientLevel level, double x, double y, double z, LightningParticleOptions options) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.start = new Vec3(x, y, z);
        this.end = options.target();
        this.rgbColor = ARGB.color(255, ARGB.red(options.color()), ARGB.green(options.color()), ARGB.blue(options.color()));
        this.heightGain = options.heightGain();
        this.width = options.width();
        this.seed = options.seed();
        this.distance = this.end.subtract(this.start).length();
        // Build the full path once, matching the original one-shot lightning behavior.
        // Rebuilding every frame would introduce motion that the reference effect does not have.
        this.shape = LightningShapeGenerator.build(this.start, this.end, this.heightGain, this.width, this.seed);
        this.maxDisplacement = LightningShapeGenerator.maxLateralDisplacement(this.distance, this.heightGain, this.width);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.lifetime = Math.max(1, options.lifetime());
        this.setBoundingBox(this.buildBounds());
    }

    @Override
    public ParticleRenderType getGroup() {
        return RenderTypeRegistry.LIGHTNING_GROUP;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.age++;
    }

    public @Nullable LightningRenderData renderData(float partialTickTime) {
        if (!this.isAlive()) {
            return null;
        }

        if (!this.shape.isRenderable()) {
            return null;
        }

        int color = this.currentColor(partialTickTime);
        // Match the legacy render layout: one upright strip and one rotated strip using the same width and texture family.
        ExtrusionMesh haloMesh = LightningMeshBuilder.buildVerticalStrip(this.shape, color, 1.0D);
        ExtrusionMesh coreMesh = LightningMeshBuilder.buildDiagonalStrip(this.shape, color, 1.0D);
        return new LightningRenderData(haloMesh, coreMesh, LightCoordsUtil.FULL_BRIGHT);
    }

    private float ageProgress(float partialTickTime) {
        return Mth.clamp((this.age + partialTickTime) / (float) this.lifetime, 0.0F, 1.0F);
    }

    private int currentColor(float partialTickTime) {
        // Keep the fade curve in lock-step with the render-time partial tick used by the shape animation.
        // Using 0 here would make the geometry move smoothly while brightness changes only once per tick.
        float progress = this.ageProgress(partialTickTime);
        float alpha = Math.max(MIN_ALPHA, 1.0F - progress);
        return ARGB.color(Mth.floor(alpha * 255.0F), ARGB.red(this.rgbColor), ARGB.green(this.rgbColor), ARGB.blue(this.rgbColor));
    }

    private AABB buildBounds() {
        double radius = Math.max(0.2D, this.width * WIDTH_TO_RADIUS);
        double padding = radius + this.maxDisplacement;
        double minX = Math.min(this.start.x(), this.end.x()) - padding;
        double minY = Math.min(this.start.y(), this.end.y()) - padding;
        double minZ = Math.min(this.start.z(), this.end.z()) - padding;
        double maxX = Math.max(this.start.x(), this.end.x()) + padding;
        double maxY = Math.max(this.start.y(), this.end.y()) + padding;
        double maxZ = Math.max(this.start.z(), this.end.z()) + padding;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
