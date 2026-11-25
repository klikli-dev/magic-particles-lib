// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.essentiastream;

import com.klikli_dev.magicparticleslib.extrusion.AffineTransform2D;
import com.klikli_dev.magicparticleslib.extrusion.Contour2D;
import com.klikli_dev.magicparticleslib.extrusion.Extrusion;
import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.extrusion.ExtrusionOptions;
import com.klikli_dev.magicparticleslib.extrusion.JoinStyle;
import com.klikli_dev.magicparticleslib.extrusion.NormalStyle;
import com.klikli_dev.magicparticleslib.extrusion.PathPoint;
import com.klikli_dev.magicparticleslib.extrusion.TextureCoordinateMode;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the stream's render samples into a tube mesh.
 * <p>
 * Every sample knows when it was laid ({@code birth}, in ticks). Its age in ticks, {@code slot = time - birth}, is what the
 * ripple, wobble and shimmer are indexed by. Samples that were laid while the head was slow are close together, so the
 * patterns bunch there, and fast segments stretch them.
 */
final class EssentiaStreamMeshBuilder {
    // Eight sides is round enough for a liquid tube at this size while keeping the vertex count low.
    private static final int TUBE_SEGMENTS = 8;
    private static final Contour2D TUBE_CONTOUR = Contour2D.circle(1.0D, TUBE_SEGMENTS);
    // Texture repeats per tick of travel. The path-length texture scale is derived from this and the cruise speed.
    static final double TEXTURE_REPEATS_PER_TICK = 0.075D;
    // Head: the first ticks are radius zero, then the radius blends up to the full body over a few samples.
    private static final double HEAD_ZERO_TICKS = 1.0D;
    private static final double HEAD_FULL_TICKS = 5.0D;
    // Tail: the samples within this many ticks of the back end taper with a cosine.
    private static final double TAIL_TICKS = 10.0D;
    // Ripple: radius varies by this fraction, one wave every 3 ticks of phase.
    private static final double RIPPLE_DEPTH = 0.2D;
    private static final double RIPPLE_DIVISOR = 3.0D;
    // Shimmer only darkens: brightness dips by this fraction, one wave every 2 ticks of phase.
    private static final double SHIMMER_DEPTH = 0.1D;
    private static final double SHIMMER_DIVISOR = 2.0D;
    // Centre-line wobble, in blocks, with a different period per axis.
    static final double WOBBLE_AMPLITUDE = 0.03D;
    private static final double WOBBLE_DIVISOR_X = 6.0D;
    private static final double WOBBLE_DIVISOR_Y = 7.0D;
    private static final double WOBBLE_DIVISOR_Z = 8.0D;
    private static final int OPAQUE = 255;

    private EssentiaStreamMeshBuilder() {
    }

    /**
     * The texture repeats per block of path for a stream with the given cruise speed, so that the texture moves 0.075
     * repeats per tick of travel.
     */
    static double repeatsPerBlock(double cruisePerTick) {
        return TEXTURE_REPEATS_PER_TICK / cruisePerTick;
    }

    /**
     * Extrudes the stream.
     * <p>
     * Positions are relative to origin, so the float vertices stay small; the caller translates the pose by origin - camera.
     * {@code time} is the stream's age in ticks including the partial tick, and {@code back} is the birth time of the back end.
     * textureOffset is the dissolve scroll in texture repeats. It is 0 while the stream is flying.
     */
    static ExtrusionMesh build(List<EssentiaStreamParticle.TrailSample> samples, Vec3 origin, double time, double back, int rgbColor, double repeatsPerBlock, double textureOffset) {
        ArrayList<PathPoint> points = new ArrayList<>(samples.size());
        for (EssentiaStreamParticle.TrailSample sample : samples) {
            double slot = Math.max(0.0D, time - sample.birth());
            double phase = slot + time;
            Vec3 position = wobble(sample.position().subtract(origin), phase);
            float radius = radius(sample, slot, phase, back);
            points.add(PathPoint.of(position)
                    .withColor(shimmer(rgbColor, phase))
                    .withContourTransform(AffineTransform2D.scale(radius, radius)));
        }

        return Extrusion.engine().extrude(TUBE_CONTOUR, points, options(repeatsPerBlock, textureOffset));
    }

    private static ExtrusionOptions options(double repeatsPerBlock, double textureOffset) {
        // Zero-radius ends close the tube by themselves, so caps stay off.
        // PATH_EDGE normals are enough because the stream is fullbright.
        // VERTEX_CYLINDER maps U around the tube and V along the stream, head first.
        return Extrusion.options()
                .joinStyle(JoinStyle.ANGLE)
                .normalStyle(NormalStyle.PATH_EDGE)
                .capEnds(false)
                .textureMode(TextureCoordinateMode.VERTEX_CYLINDER)
                .tubeSegments(TUBE_SEGMENTS)
                .textureLengthScale(repeatsPerBlock)
                .textureLengthOffset(textureOffset)
                .build();
    }

    private static Vec3 wobble(Vec3 position, double phase) {
        return new Vec3(
                position.x + WOBBLE_AMPLITUDE * Math.sin(phase / WOBBLE_DIVISOR_X),
                position.y + WOBBLE_AMPLITUDE * Math.sin(phase / WOBBLE_DIVISOR_Y),
                position.z + WOBBLE_AMPLITUDE * Math.sin(phase / WOBBLE_DIVISOR_Z)
        );
    }

    private static float radius(EssentiaStreamParticle.TrailSample sample, double slot, double phase, double back) {
        double ripple = 1.0D + RIPPLE_DEPTH * Math.sin(phase / RIPPLE_DIVISOR);
        double raw = sample.radius() * ripple;
        double taper = tailTaper(sample.birth() - back);
        // Clamped so no combination of terms can produce a negative radius.
        return (float) Math.max(0.0D, headCap(slot, sample.base(), raw) * taper);
    }

    // Head cap: zero for the first ticks, then a piecewise-linear blend from zero up to the raw radius.
    // The blend uses the unpulsed base for its base part, so a thinned stream stays thin at its head.
    private static double headCap(double slot, double base, double raw) {
        if (slot <= HEAD_ZERO_TICKS) {
            return 0.0D;
        }
        if (slot >= HEAD_FULL_TICKS) {
            return raw;
        }
        double at2 = 0.25D * base + 0.5D * raw;
        double at3 = 0.5D * base + 0.5D * raw;
        double at4 = (base + 2.0D * raw) / 3.0D;
        if (slot < 2.0D) {
            return Mth.lerp(slot - HEAD_ZERO_TICKS, 0.0D, at2);
        }
        if (slot < 3.0D) {
            return Mth.lerp(slot - 2.0D, at2, at3);
        }
        if (slot < 4.0D) {
            return Mth.lerp(slot - 3.0D, at3, at4);
        }
        return Mth.lerp(slot - 4.0D, at4, raw);
    }

    // Cosine taper from zero at the back end to one, over TAIL_TICKS of the body.
    private static double tailTaper(double ticksFromBack) {
        double fraction = Mth.clamp(ticksFromBack / TAIL_TICKS, 0.0D, 1.0D);
        return 0.5D - 0.5D * Math.cos(Math.PI * fraction);
    }

    private static int shimmer(int rgbColor, double phase) {
        // Only darkens: the brightness is capped at the base colour.
        double brightness = Math.min(1.0D, 1.0D - SHIMMER_DEPTH * Math.sin(phase / SHIMMER_DIVISOR));
        return ARGB.color(OPAQUE,
                channel(ARGB.red(rgbColor) * brightness),
                channel(ARGB.green(rgbColor) * brightness),
                channel(ARGB.blue(rgbColor) * brightness));
    }

    private static int channel(double value) {
        return Mth.clamp(Mth.floor(value), 0, OPAQUE);
    }
}
