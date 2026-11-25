// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.essentiastream;

import com.klikli_dev.magicparticleslib.registry.ParticleTypeRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Options for an essentia stream: a tube-shaped comet that flies from {@code from} to {@code to}.
 * <p>
 * The source is {@code from}, not the particle spawn position, so the same options can be used with
 * {@code ServerLevel.sendParticles} as well as {@code ClientLevel.addParticle}.
 *
 * @param from           start of the stream; the tail stays anchored here while the stream is shorter than maxLength
 * @param to             destination the stream flies to and drains into
 * @param color          packed RGB color (the upper 8 bits are ignored)
 * @param seed           seed for launch direction, radius variation and phase
 * @param radius         body radius in blocks
 * @param extend         hold ticks: how long the stream stays connected at the destination after the head arrives, before it drains
 * @param verticalBoost  upward launch pop in blocks per second, decaying quickly; 0 disables it
 * @param waypoints      optional intermediate points the stream passes through before {@code to}
 * @param launchDirection optional direction to leave {@code from} in; empty for a seeded random direction
 * @param maxLength      maximum arc length of the stream in blocks; once reached, the tail is trimmed so the stream acts as a comet of this length
 */
public record EssentiaStreamParticleOptions(Vec3 from, Vec3 to, int color, int seed, float radius, int extend, float verticalBoost, List<Vec3> waypoints, Optional<Vec3> launchDirection, float maxLength) implements ParticleOptions {
    public static final float DEFAULT_RADIUS = 0.12F;
    public static final int DEFAULT_EXTEND = 0;
    public static final float DEFAULT_VERTICAL_BOOST = 0.0F;
    public static final float DEFAULT_MAX_LENGTH = 16.0F;
    // Shorter streams are not useful, and very small values would make the trail degenerate.
    public static final float MIN_MAX_LENGTH = 0.5F;
    private static final int RGB_MASK = 0xFFFFFF;

    public EssentiaStreamParticleOptions {
        from = from == null ? Vec3.ZERO : from;
        to = to == null ? Vec3.ZERO : to;
        color = color & RGB_MASK;
        radius = Math.max(0.0F, Float.isNaN(radius) ? 0.0F : radius);
        extend = Math.max(0, extend);
        verticalBoost = Float.isNaN(verticalBoost) ? 0.0F : verticalBoost;
        waypoints = waypoints == null ? List.of() : List.copyOf(waypoints);
        launchDirection = launchDirection == null ? Optional.empty() : launchDirection;
        maxLength = Float.isNaN(maxLength) ? DEFAULT_MAX_LENGTH : Math.max(MIN_MAX_LENGTH, maxLength);
    }

    public static final MapCodec<EssentiaStreamParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3.CODEC.fieldOf("from").forGetter(EssentiaStreamParticleOptions::from),
            Vec3.CODEC.fieldOf("to").forGetter(EssentiaStreamParticleOptions::to),
            Codec.INT.fieldOf("color").forGetter(EssentiaStreamParticleOptions::color),
            Codec.INT.fieldOf("seed").forGetter(EssentiaStreamParticleOptions::seed),
            Codec.FLOAT.fieldOf("radius").forGetter(EssentiaStreamParticleOptions::radius),
            Codec.INT.fieldOf("extend").forGetter(EssentiaStreamParticleOptions::extend),
            Codec.FLOAT.fieldOf("verticalBoost").forGetter(EssentiaStreamParticleOptions::verticalBoost),
            Vec3.CODEC.listOf().optionalFieldOf("waypoints", List.of()).forGetter(EssentiaStreamParticleOptions::waypoints),
            Vec3.CODEC.optionalFieldOf("launchDirection").forGetter(EssentiaStreamParticleOptions::launchDirection),
            Codec.FLOAT.optionalFieldOf("maxLength", DEFAULT_MAX_LENGTH).forGetter(EssentiaStreamParticleOptions::maxLength)
    ).apply(instance, EssentiaStreamParticleOptions::new));

    // The record has nine fields, more than StreamCodec.composite supports, so the network format is written by hand.
    public static final StreamCodec<RegistryFriendlyByteBuf, EssentiaStreamParticleOptions> STREAM_CODEC = StreamCodec.of(
            EssentiaStreamParticleOptions::encode,
            EssentiaStreamParticleOptions::decode
    );

    public static EssentiaStreamParticleOptions of(Vec3 from, Vec3 to, int color) {
        return of(from, to, color, DEFAULT_RADIUS, DEFAULT_EXTEND);
    }

    public static EssentiaStreamParticleOptions of(Vec3 from, Vec3 to, int color, float radius, int extend) {
        return of(from, to, color, radius, extend, DEFAULT_MAX_LENGTH);
    }

    public static EssentiaStreamParticleOptions of(Vec3 from, Vec3 to, int color, float radius, int extend, float maxLength) {
        return new EssentiaStreamParticleOptions(from, to, color, RandomSource.create().nextInt(), radius, extend, DEFAULT_VERTICAL_BOOST, List.of(), Optional.empty(), maxLength);
    }

    @Override
    public @NonNull ParticleType<EssentiaStreamParticleOptions> getType() {
        return ParticleTypeRegistry.ESSENTIA_STREAM.get();
    }

    private static void encode(RegistryFriendlyByteBuf buf, EssentiaStreamParticleOptions value) {
        Vec3.STREAM_CODEC.encode(buf, value.from());
        Vec3.STREAM_CODEC.encode(buf, value.to());
        ByteBufCodecs.INT.encode(buf, value.color());
        ByteBufCodecs.VAR_INT.encode(buf, value.seed());
        ByteBufCodecs.FLOAT.encode(buf, value.radius());
        ByteBufCodecs.VAR_INT.encode(buf, value.extend());
        ByteBufCodecs.FLOAT.encode(buf, value.verticalBoost());
        buf.writeVarInt(value.waypoints().size());
        for (Vec3 waypoint : value.waypoints()) {
            Vec3.STREAM_CODEC.encode(buf, waypoint);
        }
        buf.writeBoolean(value.launchDirection().isPresent());
        value.launchDirection().ifPresent(direction -> Vec3.STREAM_CODEC.encode(buf, direction));
        ByteBufCodecs.FLOAT.encode(buf, value.maxLength());
    }

    private static EssentiaStreamParticleOptions decode(RegistryFriendlyByteBuf buf) {
        Vec3 from = Vec3.STREAM_CODEC.decode(buf);
        Vec3 to = Vec3.STREAM_CODEC.decode(buf);
        int color = ByteBufCodecs.INT.decode(buf);
        int seed = ByteBufCodecs.VAR_INT.decode(buf);
        float radius = ByteBufCodecs.FLOAT.decode(buf);
        int extend = ByteBufCodecs.VAR_INT.decode(buf);
        float verticalBoost = ByteBufCodecs.FLOAT.decode(buf);
        int waypointCount = buf.readVarInt();
        List<Vec3> waypoints = new ArrayList<>(waypointCount);
        for (int index = 0; index < waypointCount; index++) {
            waypoints.add(Vec3.STREAM_CODEC.decode(buf));
        }
        Optional<Vec3> launchDirection = buf.readBoolean() ? Optional.of(Vec3.STREAM_CODEC.decode(buf)) : Optional.empty();
        float maxLength = ByteBufCodecs.FLOAT.decode(buf);
        return new EssentiaStreamParticleOptions(from, to, color, seed, radius, extend, verticalBoost, waypoints, launchDirection, maxLength);
    }
}
