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
 * Samples are ordered head first, tail last. All tapers, wobble and phases are measured in blocks of arc length from the
 * head or the back end, so they do not depend on how many samples the stream has.
 */
final class EssentiaStreamMeshBuilder {
    // Eight sides is round enough for a liquid tube at this size while keeping the vertex count low.
    private static final int TUBE_SEGMENTS = 8;
    private static final Contour2D TUBE_CONTOUR = Contour2D.circle(1.0D, TUBE_SEGMENTS);
    // Texture repeats per trail point (about 0.1 blocks), so repeats per block is derived from the sample spacing.
    private static final double TEXTURE_REPEATS_PER_POINT = 0.075D;
    // Package-private so the particle can scale the dissolve scroll by the drain speed.
    static final double TEXTURE_REPEATS_PER_BLOCK = TEXTURE_REPEATS_PER_POINT / EssentiaStreamParticle.TRAIL_POINT_SPACING;
    // Head tip swells from zero radius over this distance, in blocks.
    private static final double TIP_LENGTH = 0.35D;
    // Tail end tapers to zero over this distance once the back end is free of the source.
    private static final double TAIL_LENGTH = 1.0D;
    // While the back end is anchored at the source, the tail tapers over this shorter distance so the tube emerges from the source.
    private static final double ANCHORED_TAIL_LENGTH = 0.3D;
    // Wobble fades in over this distance from the back end, so it is zero at an anchored source.
    private static final double WOBBLE_FADE_LENGTH = 0.5D;
    // Body thickness breathing: slow, large pulse over the whole stream.
    private static final float PULSE_DEPTH = 0.25F;
    private static final float PULSE_RATE = 0.12F;
    // Thickness ripple that travels along the body, so it reads as flowing liquid. Wave number is in radians per block.
    private static final float RIPPLE_DEPTH = 0.2F;
    private static final float RIPPLE_RATE = 0.35F;
    private static final float RIPPLE_WAVE_NUMBER = 6.0F;
    // Brightness shimmer that travels along the tube. Wave number is in radians per block.
    private static final float SHIMMER_DEPTH = 0.1F;
    private static final float SHIMMER_RATE = 0.2F;
    private static final float SHIMMER_WAVE_NUMBER = 3.0F;
    // Shimmering centre-line wobble, in blocks per axis, with a different rate per axis.
    static final double WOBBLE_AMPLITUDE = 0.03D;
    private static final double WOBBLE_RATE_X = 0.31D;
    private static final double WOBBLE_RATE_Y = 0.23D;
    private static final double WOBBLE_RATE_Z = 0.37D;
    // Wobble phase advances by this many radians per block along the stream.
    private static final double WOBBLE_PHASE_PER_BLOCK = 4.0D;
    private static final int OPAQUE = 255;

    // Initialised after the constants above, since it reads them.
    private static final ExtrusionOptions OPTIONS = options(0.0D);

    private EssentiaStreamMeshBuilder() {
    }

    /**
     * Extrudes the stream.
     * <p>
     * Positions are relative to origin, so the float vertices stay small; the caller translates the pose by origin - camera.
     * textureOffset is the dissolve scroll in texture repeats. It is 0 while the stream is growing or holding, and while
     * draining it grows with the distance the back end has moved.
     */
    static ExtrusionMesh build(List<EssentiaStreamParticle.TrailSample> samples, Vec3 origin, float baseRadius, int rgbColor, float time, float phase, float textureOffset) {
        int count = samples.size();
        double frontArc = samples.get(0).arc();
        double backArc = samples.get(count - 1).arc();
        double tailLength = tailTaperLength(backArc);
        // If the stream is shorter than the tapers, scale both down so the radii stay sensible.
        double length = Math.max(0.0D, frontArc - backArc);
        double taperTotal = TIP_LENGTH + tailLength;
        double taperScale = Math.min(1.0D, length / taperTotal);
        double tip = TIP_LENGTH * taperScale;
        double tail = tailLength * taperScale;

        ArrayList<PathPoint> points = new ArrayList<>(count);
        for (EssentiaStreamParticle.TrailSample sample : samples) {
            double fromFront = Math.max(0.0D, frontArc - sample.arc());
            double fromBack = Math.max(0.0D, sample.arc() - backArc);
            Vec3 position = wobble(sample.position().subtract(origin), time, phase, fromFront, fromBack);
            float radius = radius(baseRadius, time, phase, fromFront, fromBack, tip, tail, sample.arrival());
            points.add(PathPoint.of(position)
                    .withColor(shimmer(rgbColor, time, phase, fromFront))
                    .withContourTransform(AffineTransform2D.scale(radius, radius)));
        }

        ExtrusionOptions options = textureOffset != 0.0F ? options(textureOffset) : OPTIONS;
        return Extrusion.engine().extrude(TUBE_CONTOUR, points, options);
    }

    // Tail taper eases from the anchored length to the free length as the back end moves away from the source.
    private static double tailTaperLength(double backArc) {
        double freedom = Math.min(1.0D, backArc / TAIL_LENGTH);
        return ANCHORED_TAIL_LENGTH + (TAIL_LENGTH - ANCHORED_TAIL_LENGTH) * freedom;
    }

    private static ExtrusionOptions options(double textureOffset) {
        // Zero-radius tips close the tube by themselves, so caps stay off.
        // PATH_EDGE normals are enough because the stream is fullbright.
        // VERTEX_CYLINDER maps U around the tube and V along the stream, head first.
        return Extrusion.options()
                .joinStyle(JoinStyle.ANGLE)
                .normalStyle(NormalStyle.PATH_EDGE)
                .capEnds(false)
                .textureMode(TextureCoordinateMode.VERTEX_CYLINDER)
                .tubeSegments(TUBE_SEGMENTS)
                .textureLengthScale(TEXTURE_REPEATS_PER_BLOCK)
                .textureLengthOffset(textureOffset)
                .build();
    }

    private static Vec3 wobble(Vec3 position, float time, float phase, double fromFront, double fromBack) {
        double fade = Math.min(1.0D, fromBack / WOBBLE_FADE_LENGTH);
        double shift = phase + fromFront * WOBBLE_PHASE_PER_BLOCK;
        double amplitude = WOBBLE_AMPLITUDE * fade;
        return new Vec3(
                position.x + amplitude * Mth.sin(time * WOBBLE_RATE_X + shift),
                position.y + amplitude * Mth.sin(time * WOBBLE_RATE_Y + shift * 1.3D),
                position.z + amplitude * Mth.sin(time * WOBBLE_RATE_Z + shift * 0.7D)
        );
    }

    private static float radius(float baseRadius, float time, float phase, double fromFront, double fromBack, double tip, double tail, float arrival) {
        float tipSwell = tip > 0.0D ? Mth.sin(Math.min(1.0D, fromFront / tip) * Mth.HALF_PI) : 0.0F;
        float tailSwell = tail > 0.0D ? Mth.sin(Math.min(1.0D, fromBack / tail) * Mth.HALF_PI) : 0.0F;
        float pulse = 1.0F + PULSE_DEPTH * Mth.sin(time * PULSE_RATE + phase);
        float ripple = 1.0F + RIPPLE_DEPTH * Mth.sin((float) fromFront * RIPPLE_WAVE_NUMBER - time * RIPPLE_RATE + phase);
        // Clamped so no combination of terms can produce a negative radius.
        return Math.max(0.0F, baseRadius * pulse * ripple * tipSwell * tailSwell * arrival);
    }

    private static int shimmer(int rgbColor, float time, float phase, double fromFront) {
        float brightness = 1.0F + SHIMMER_DEPTH * Mth.sin(time * SHIMMER_RATE - (float) fromFront * SHIMMER_WAVE_NUMBER + phase);
        return ARGB.color(OPAQUE,
                channel(ARGB.red(rgbColor) * brightness),
                channel(ARGB.green(rgbColor) * brightness),
                channel(ARGB.blue(rgbColor) * brightness));
    }

    private static int channel(float value) {
        return Mth.clamp(Mth.floor(value), 0, OPAQUE);
    }
}
