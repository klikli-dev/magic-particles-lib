// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.electricarc;

import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import org.jspecify.annotations.Nullable;

public class ElectricArcParticle extends Particle {
    // Converts the user-facing width into an extrusion radius.
    // Higher values make both halo and core visibly thicker.
    private static final double WIDTH_TO_RADIUS = 0.1D;
    // Core radius relative to the halo radius.
    // Smaller values create a sharper bright center; larger values make the core fill more of the bolt.
    private static final double INNER_RADIUS_SCALE = 1.0D / 3.0D;
    // Fade never goes fully invisible while the particle is alive.
    // Raising this keeps dying arcs brighter; lowering it makes them fade out more completely.
    private static final float MIN_ALPHA = 0.1F;

    private final Vec3 start;
    private final Vec3 end;
    private final int rgbColor;
    private final float width;
    private final int seed;
    private final double distance;
    private final double maxDisplacement;

    public ElectricArcParticle(ClientLevel level, double x, double y, double z, ElectricArcParticleOptions options) {
        // Zero motion: this particle is entirely procedural and rendered from start/end positions,
        // not simulated like a drifting sprite particle.
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.start = new Vec3(x, y, z);
        this.end = options.target();
        // Store a packed opaque RGB color. Alpha is animated separately in currentColor().
        this.rgbColor = ARGB.color(255, ARGB.red(options.color()), ARGB.green(options.color()), ARGB.blue(options.color()));
        this.width = options.width();
        this.seed = options.seed();
        // Precompute distance-dependent values once so renderData() stays cheap.
        this.distance = this.end.subtract(this.start).length();
        this.maxDisplacement = ElectricArcShapeGenerator.maxLateralDisplacement(this.distance, this.width);
        // The arc should hang in place and ignore vanilla particle physics.
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        // Lifetime directly controls how many animation states the procedural arc can show.
        // Longer lifetimes let TIME_SWAY and the alpha curve evolve further.
        this.lifetime = Math.max(1, options.lifetime());
        // Bounds must cover the worst-case procedural wobble or the beam can be culled too early.
        this.setBoundingBox(this.buildBounds());
    }

    @Override
    public ParticleRenderType getGroup() {
        return RenderTypeRegistry.ELECTRIC_ARC_GROUP;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void tick() {
        // Preserve previous position fields even though the particle is stationary.
        // Some particle/render infrastructure expects xo/yo/zo to be maintained.
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.age++;
    }

    public @Nullable ElectricArcRenderData renderData(float partialTickTime) {
        if (!this.isAlive()) {
            return null;
        }

        // Rebuild the procedural arc every frame so it can animate over its short lifetime.
        ElectricArcShape shape = ElectricArcShapeGenerator.build(this.start, this.end, this.width, this.seed, this.age, partialTickTime);
        if (!shape.isRenderable()) {
            return null;
        }

        // Same RGB for both meshes; only geometry thickness differs.
        int color = this.currentColor();
        // Halo keeps the full radius, which gives the bolt its broader soft glow.
        ExtrusionMesh haloMesh = ElectricArcMeshBuilder.build(shape, color, 1.0D);
        // Core reuses the same path but at a smaller radius so the center looks brighter and tighter.
        ExtrusionMesh coreMesh = ElectricArcMeshBuilder.build(shape, color, INNER_RADIUS_SCALE);
        return new ElectricArcRenderData(haloMesh, coreMesh, LightCoordsUtil.FULL_BRIGHT);
    }

    // Normalized age in [0, 1].
    // This is the master control for fade timing.
    private float ageProgress() {
        return Mth.clamp((float) this.age / (float) this.lifetime, 0.0F, 1.0F);
    }

    // Computes the per-frame ARGB color.
    // Match the original simple linear fade.
    private int currentColor() {
        float progress = this.ageProgress();
        float alpha = Math.max(MIN_ALPHA, 1.0F - progress);
        return ARGB.color(Mth.floor(alpha * 255.0F), ARGB.red(this.rgbColor), ARGB.green(this.rgbColor), ARGB.blue(this.rgbColor));
    }

    // Match the original bounds expansion so culling/visibility behavior stays the same.
    private AABB buildBounds() {
        double radius = Math.max(0.25D, this.width * WIDTH_TO_RADIUS);
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
