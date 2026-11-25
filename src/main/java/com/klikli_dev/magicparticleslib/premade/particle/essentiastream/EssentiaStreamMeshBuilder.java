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
 * Turns a snapshot of the essentia stream trail into a tube mesh.
 * <p>
 * The trail is ordered head first, tail last, so the V texture coordinate starts at the head.
 */
final class EssentiaStreamMeshBuilder {
    // Eight sides is round enough for a liquid tube at this size while keeping the vertex count low.
    private static final int TUBE_SEGMENTS = 8;
    private static final Contour2D TUBE_CONTOUR = Contour2D.circle(1.0D, TUBE_SEGMENTS);
    // The head is 0.05 blocks per trail point at cruise speed, so 1.5 repeats per block gives 0.075 repeats per point.
    private static final double TEXTURE_REPEATS_PER_BLOCK = 1.5D;
    // Trail points over which the head swells from a zero-radius tip to full body thickness.
    private static final int TIP_POINTS = 4;
    // Trail points over which the tail eases down to a zero-radius end.
    private static final int TAIL_POINTS = 10;
    // Body thickness breathing: slow, large pulse over the whole stream.
    private static final float PULSE_DEPTH = 0.25F;
    private static final float PULSE_RATE = 0.12F;
    // Thickness ripple that travels along the body, so it reads as flowing liquid.
    private static final float RIPPLE_DEPTH = 0.2F;
    private static final float RIPPLE_RATE = 0.35F;
    private static final float RIPPLE_WAVELENGTH = 0.5F;
    // Brightness shimmer that travels along the tube.
    private static final float SHIMMER_DEPTH = 0.1F;
    private static final float SHIMMER_RATE = 0.2F;
    private static final float SHIMMER_WAVELENGTH = 0.4F;
    // Shimmering centre-line wobble, in blocks per axis, with a different rate per axis.
    static final double WOBBLE_AMPLITUDE = 0.03D;
    private static final double WOBBLE_RATE_X = 0.31D;
    private static final double WOBBLE_RATE_Y = 0.23D;
    private static final double WOBBLE_RATE_Z = 0.37D;
    private static final double WOBBLE_PHASE_PER_POINT = 0.35D;
    // Texture repeats per tick that the dissolve scroll moves toward the destination.
    private static final double DISSOLVE_REPEATS_PER_TICK = 0.25D;
    private static final int OPAQUE = 255;

    private static final ExtrusionOptions OPTIONS = options(0.0D);

    private EssentiaStreamMeshBuilder() {
    }

    // Extrudes the trail. Radii, colours and wobble are evaluated per point so the tube animates each frame.
    // dissolveTime is 0 while flying; while draining it is the number of ticks since the drain started.
    static ExtrusionMesh build(List<EssentiaStreamParticle.TrailSample> trail, float baseRadius, int rgbColor, float time, float phase, float dissolveTime) {
        int count = trail.size();
        ArrayList<PathPoint> points = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            EssentiaStreamParticle.TrailSample sample = trail.get(index);
            Vec3 position = wobble(sample.position(), time, phase, index);
            float radius = radius(baseRadius, time, phase, index, count, sample.arrival());
            points.add(PathPoint.of(position)
                    .withColor(shimmer(rgbColor, time, phase, index))
                    .withContourTransform(AffineTransform2D.scale(radius, radius)));
        }

        ExtrusionOptions options = dissolveTime > 0.0F ? options(dissolveTime * DISSOLVE_REPEATS_PER_TICK) : OPTIONS;
        return Extrusion.engine().extrude(TUBE_CONTOUR, points, options);
    }

    private static ExtrusionOptions options(double textureOffset) {
        // Zero-radius tips close the tube by themselves, so caps stay off.
        // PATH_EDGE normals are enough because the stream is fullbright.
        // VERTEX_CYLINDER maps U around the tube and V along the trail, head first.
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

    private static Vec3 wobble(Vec3 position, float time, float phase, int index) {
        double shift = phase + index * WOBBLE_PHASE_PER_POINT;
        return new Vec3(
                position.x + WOBBLE_AMPLITUDE * Mth.sin(time * WOBBLE_RATE_X + shift),
                position.y + WOBBLE_AMPLITUDE * Mth.sin(time * WOBBLE_RATE_Y + shift * 1.3D),
                position.z + WOBBLE_AMPLITUDE * Mth.sin(time * WOBBLE_RATE_Z + shift * 0.7D)
        );
    }

    private static float radius(float baseRadius, float time, float phase, int index, int count, float arrival) {
        float tipProgress = Math.min(1.0F, index / (float) TIP_POINTS);
        float tip = Mth.sin(tipProgress * Mth.HALF_PI);
        float tailProgress = Math.min(1.0F, (count - 1 - index) / (float) TAIL_POINTS);
        float tail = Mth.sin(tailProgress * Mth.HALF_PI);
        float pulse = 1.0F + PULSE_DEPTH * Mth.sin(time * PULSE_RATE + phase);
        float ripple = 1.0F + RIPPLE_DEPTH * Mth.sin(index * RIPPLE_WAVELENGTH - time * RIPPLE_RATE + phase);
        // Clamped so no combination of terms can produce a negative radius.
        return Math.max(0.0F, baseRadius * pulse * ripple * tip * tail * arrival);
    }

    private static int shimmer(int rgbColor, float time, float phase, int index) {
        float brightness = 1.0F + SHIMMER_DEPTH * Mth.sin(time * SHIMMER_RATE - index * SHIMMER_WAVELENGTH + phase);
        return ARGB.color(OPAQUE,
                channel(ARGB.red(rgbColor) * brightness),
                channel(ARGB.green(rgbColor) * brightness),
                channel(ARGB.blue(rgbColor) * brightness));
    }

    private static int channel(float value) {
        return Mth.clamp(Mth.floor(value), 0, OPAQUE);
    }
}
