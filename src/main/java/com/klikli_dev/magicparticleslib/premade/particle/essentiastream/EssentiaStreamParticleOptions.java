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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Options for an essentia stream: a worm-shaped body that flies from {@code from} to {@code to}.
 * <p>
 * The source is {@code from}, not the particle spawn position, so the same options can be used with
 * {@code ServerLevel.sendParticles} as well as {@code ClientLevel.addParticle}.
 * <p>
 * Ports ({@link StreamPort}) are resolved into these plain fields by {@link Builder}, so they need no codec of their own.
 *
 * @param from            start of the stream
 * @param to              destination the stream flies to and drains into
 * @param color           packed RGB color (the upper 8 bits are ignored)
 * @param seed            seed for the launch direction and the radius variation and pulse
 * @param radius          body radius in blocks, before the per-stream variation
 * @param extend          minimum body length in ticks when the stream is created (the body is {@code max(length, extend)});
 *                        a merge into a live stream adds {@code max(extend, 5)} ticks
 * @param verticalBoost   initial upward speed in blocks per second; 0 disables it
 * @param waypoints       optional intermediate points the stream passes through before {@code to}
 * @param launchDirection optional direction to leave {@code from} in; empty for a seeded launch
 * @param length          body length in ticks of travel: the body is the path the head flew in the last {@code length} ticks
 * @param speed           head speed in blocks per second. The steering and the launch scale with it
 */
public record EssentiaStreamParticleOptions(Vec3 from, Vec3 to, int color, int seed, float radius, int extend, float verticalBoost, List<Vec3> waypoints, Optional<Vec3> launchDirection, int length, float speed) implements ParticleOptions {
    public static final float DEFAULT_RADIUS = 0.15F;
    public static final int DEFAULT_EXTEND = 0;
    public static final float DEFAULT_VERTICAL_BOOST = 0.0F;
    // Twenty ticks of travel: about one block at the default speed.
    public static final int DEFAULT_LENGTH = 20;
    public static final int MIN_LENGTH = 1;
    public static final int MAX_LENGTH = 400;
    public static final float DEFAULT_SPEED = 1.25F;
    // Slowest is syrupy but still moves; fastest is a fast streak that the sampling and steering still handle.
    public static final float MIN_SPEED = 0.25F;
    public static final float MAX_SPEED = 20.0F;
    public static final int TICKS_PER_SECOND = 20;
    // Routes shorter than this go straight to the target, without the port clearance points.
    public static final double SHORT_ROUTE_DISTANCE = 1.75D;
    private static final int RGB_MASK = 0xFFFFFF;

    public EssentiaStreamParticleOptions {
        from = from == null ? Vec3.ZERO : from;
        to = to == null ? Vec3.ZERO : to;
        color = color & RGB_MASK;
        radius = Math.max(0.0F, Float.isNaN(radius) ? 0.0F : radius);
        extend = Math.max(0, extend);
        verticalBoost = Float.isNaN(verticalBoost) ? 0.0F : Mth.clamp(verticalBoost, -MAX_SPEED, MAX_SPEED);
        waypoints = waypoints == null ? List.of() : List.copyOf(waypoints);
        launchDirection = launchDirection == null ? Optional.empty() : launchDirection;
        length = Mth.clamp(length, MIN_LENGTH, MAX_LENGTH);
        speed = Float.isNaN(speed) ? DEFAULT_SPEED : Mth.clamp(speed, MIN_SPEED, MAX_SPEED);
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
            Codec.INT.optionalFieldOf("length", DEFAULT_LENGTH).forGetter(EssentiaStreamParticleOptions::length),
            Codec.FLOAT.optionalFieldOf("speed", DEFAULT_SPEED).forGetter(EssentiaStreamParticleOptions::speed)
    ).apply(instance, EssentiaStreamParticleOptions::new));

    // The record has eleven fields, more than StreamCodec.composite supports, so the network format is written by hand.
    public static final StreamCodec<RegistryFriendlyByteBuf, EssentiaStreamParticleOptions> STREAM_CODEC = StreamCodec.of(
            EssentiaStreamParticleOptions::encode,
            EssentiaStreamParticleOptions::decode
    );

    public static Builder builder(int color) {
        return new Builder(color);
    }

    public static EssentiaStreamParticleOptions of(Vec3 from, Vec3 to, int color) {
        return of(from, to, color, DEFAULT_RADIUS, DEFAULT_EXTEND);
    }

    public static EssentiaStreamParticleOptions of(Vec3 from, Vec3 to, int color, float radius, int extend) {
        return of(from, to, color, radius, extend, DEFAULT_LENGTH);
    }

    public static EssentiaStreamParticleOptions of(Vec3 from, Vec3 to, int color, float radius, int extend, int length) {
        return of(from, to, color, radius, extend, length, DEFAULT_SPEED);
    }

    public static EssentiaStreamParticleOptions of(Vec3 from, Vec3 to, int color, float radius, int extend, int length, float speed) {
        return new EssentiaStreamParticleOptions(from, to, color, RandomSource.create().nextInt(), radius, extend, DEFAULT_VERTICAL_BOOST, List.of(), Optional.empty(), length, speed);
    }

    /**
     * The length of the route from {@link #from()} through the waypoints to {@link #to()}, in blocks.
     */
    public double routeLength() {
        double total = 0.0D;
        Vec3 previous = this.from;
        for (Vec3 point : this.waypoints) {
            total += previous.distanceTo(point);
            previous = point;
        }
        return total + previous.distanceTo(this.to);
    }

    /**
     * An approximate flight time in ticks: the route length divided by the speed per tick. It ignores steering, so it is
     * an estimate, not a bound. Real flights usually take longer because of the steering turns. Diagonal routes can be
     * faster, since the per-axis velocity clamp lets the total speed reach up to about √3 times the speed.
     */
    public int straightFlightTicks() {
        double cruisePerTick = this.speed / TICKS_PER_SECOND;
        return (int) Math.ceil(this.routeLength() / cruisePerTick);
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
        ByteBufCodecs.VAR_INT.encode(buf, value.length());
        ByteBufCodecs.FLOAT.encode(buf, value.speed());
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
        int length = ByteBufCodecs.VAR_INT.decode(buf);
        float speed = ByteBufCodecs.FLOAT.decode(buf);
        return new EssentiaStreamParticleOptions(from, to, color, seed, radius, extend, verticalBoost, waypoints, launchDirection, length, speed);
    }

    /**
     * Builds options with optional ports at either end.
     * <p>
     * An exit port sets the source to its anchor, launches the stream toward its clearance point, and makes that point
     * the first waypoint. An entry port sets the destination to its anchor, and makes its clearance point the last
     * waypoint, so the stream dives straight in through the face. Routes shorter than {@link #SHORT_ROUTE_DISTANCE} go
     * straight to the destination without the port clearance points.
     * <p>
     * Explicit waypoints from {@link #via(Vec3)} go between the exit and entry clearance points. They can come from a
     * pathfinder; this builder does not compute them.
     */
    public static final class Builder {
        private final int color;
        private final List<Vec3> via = new ArrayList<>();
        private Vec3 from = Vec3.ZERO;
        private Vec3 to = Vec3.ZERO;
        private StreamPort exit;
        private StreamPort entry;
        private Vec3 launchDirection;
        private Integer seed;
        private float radius = DEFAULT_RADIUS;
        private int extend = DEFAULT_EXTEND;
        private float verticalBoost = DEFAULT_VERTICAL_BOOST;
        private int length = DEFAULT_LENGTH;
        private float speed = DEFAULT_SPEED;

        private Builder(int color) {
            this.color = color;
        }

        public Builder from(Vec3 from) {
            this.from = from;
            this.exit = null;
            return this;
        }

        public Builder exit(StreamPort port) {
            this.exit = port;
            return this;
        }

        public Builder to(Vec3 to) {
            this.to = to;
            this.entry = null;
            return this;
        }

        public Builder entry(StreamPort port) {
            this.entry = port;
            return this;
        }

        public Builder via(Vec3 waypoint) {
            this.via.add(waypoint);
            return this;
        }

        public Builder launchDirection(Vec3 direction) {
            this.launchDirection = direction;
            return this;
        }

        public Builder seed(int seed) {
            this.seed = seed;
            return this;
        }

        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }

        public Builder extend(int extend) {
            this.extend = extend;
            return this;
        }

        public Builder verticalBoost(float verticalBoost) {
            this.verticalBoost = verticalBoost;
            return this;
        }

        public Builder length(int length) {
            this.length = length;
            return this;
        }

        public Builder speed(float speed) {
            this.speed = speed;
            return this;
        }

        public EssentiaStreamParticleOptions build() {
            Vec3 source = this.exit != null ? this.exit.anchor() : this.from;
            Vec3 destination = this.entry != null ? this.entry.anchor() : this.to;
            boolean straight = source.distanceTo(destination) < SHORT_ROUTE_DISTANCE;

            List<Vec3> waypoints = new ArrayList<>();
            Optional<Vec3> launch = Optional.ofNullable(this.launchDirection);
            if (!straight && this.exit != null) {
                waypoints.add(this.exit.clearance());
                if (launch.isEmpty()) {
                    launch = Optional.of(this.exit.clearance().subtract(this.exit.anchor()));
                }
            }
            waypoints.addAll(this.via);
            if (!straight && this.entry != null) {
                waypoints.add(this.entry.clearance());
            }

            int streamSeed = this.seed != null ? this.seed : RandomSource.create().nextInt();
            return new EssentiaStreamParticleOptions(source, destination, this.color, streamSeed, this.radius, this.extend, this.verticalBoost, waypoints, launch, this.length, this.speed);
        }
    }
}
