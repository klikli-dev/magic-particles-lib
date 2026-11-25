// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.essentiastream;

import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A stream of essentia that flies from its source to a destination and drains into it.
 * <p>
 * A moving head is simulated at 20 Hz. Every tick the head position is appended to a trail, so the rendered
 * centre line is the path the head actually took. Once the head has arrived, the trail is removed from its
 * tail end one point per tick, which makes the stream look like it is draining into the destination.
 */
public class EssentiaStreamParticle extends Particle {
    // Head speed in blocks per second, and the same speed in blocks per tick.
    private static final double CRUISE_SPEED_BLOCKS_PER_SECOND = 1.0D;
    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double CRUISE_SPEED = CRUISE_SPEED_BLOCKS_PER_SECOND / TICKS_PER_SECOND;
    // Launch speed when no launch direction is given, as a fraction of cruise speed.
    private static final double RANDOM_LAUNCH_SPEED_FACTOR = 0.5D;
    // Fraction of the velocity error corrected per tick: gentle far away, firm when close, so the head homes in without much overshoot.
    private static final double STEER_FAR = 0.03D;
    private static final double STEER_NEAR = 0.25D;
    private static final double STEER_NEAR_DISTANCE = 2.0D;
    // Per-tick velocity damping, which keeps the path soft.
    private static final double VELOCITY_DAMPING = 0.97D;
    // Small constant upward acceleration per tick, so flights bulge slightly upward.
    private static final double UPWARD_BIAS = 0.0004D;
    // On the final leg, head speed ramps down over this distance so the head doesn't overshoot the destination.
    private static final double SLOW_DISTANCE = 0.6D;
    private static final double SLOW_MIN_FACTOR = 0.15D;
    // Distance at which an intermediate waypoint counts as reached.
    private static final double WAYPOINT_REACH_DISTANCE = 0.4D;
    // Within this distance of the destination the head radius shrinks smoothly to zero.
    private static final double ARRIVE_BLEND_DISTANCE = 1.0D;
    // The head counts as arrived within this distance, and it is snapped onto the destination.
    private static final double ARRIVE_STOP_DISTANCE = 0.05D;
    // The launch hop decays by this factor per tick. Total hop height is the boost divided by (1 - decay).
    private static final double HOP_DECAY = 0.8D;
    // Trail points kept without any extend.
    private static final int BASE_TRAIL_POINTS = 20;
    // Merging a new spawn into a live stream grows its capacity and lifetime by at least this many ticks.
    static final int MERGE_MIN_TICKS = 5;
    // Lifetime safety net: flight time at cruise speed, plus the drain (one point per tick), plus this margin.
    private static final int LIFETIME_MARGIN_TICKS = 20;
    private static final int MIN_LIFETIME_TICKS = 40;
    // Streams with fewer trail points are not rendered yet.
    private static final int MIN_RENDER_POINTS = 3;
    // Body radius variation is gaussian with this sigma (relative), clamped to the limit.
    private static final double RADIUS_VARIATION_SIGMA = 0.15D;
    private static final double RADIUS_VARIATION_LIMIT = 0.3D;
    // Extra bounds padding for curve bulge and other drift the mesh might reach beyond its route.
    private static final double BOUNDS_MARGIN = 2.0D;
    // Droplets: chance per tick while flying, lifetime in ticks, size and alpha.
    private static final float DROPLET_CHANCE = 0.5F;
    private static final int DROPLET_MIN_AGE = 20;
    private static final int DROPLET_MAX_AGE = 30;
    private static final float DROPLET_MIN_SIZE = 0.05F;
    private static final float DROPLET_SIZE_RANGE = 0.06F;
    private static final int DROPLET_ALPHA = 128;
    private static final double DROPLET_DRIFT = 0.01D;
    private static final double DROPLET_BUOYANCY = 0.004D;
    // Droplets spawned while draining start near the destination and are pulled gently into it.
    private static final double DISSOLVE_SPREAD = 0.08D;
    private static final double DISSOLVE_PULL = 0.1D;

    // Global switch for droplets, so callers can turn them off.
    private static volatile boolean dropletsEnabled = true;

    private final Vec3 destination;
    private final List<Vec3> route;
    private final int rgbColor;
    private final float baseRadius;
    private final float phase;
    private final Deque<TrailSample> trail = new ArrayDeque<>();
    private int capacity;
    private Vec3 head;
    private Vec3 velocity;
    private Vec3 hop;
    private int routeIndex = 1;
    private boolean arrived;
    private int dissolveTicks;

    public EssentiaStreamParticle(ClientLevel level, EssentiaStreamParticleOptions options) {
        // Zero motion: the stream is simulated by this class, not by the vanilla particle physics.
        super(level, options.from().x, options.from().y, options.from().z, 0.0D, 0.0D, 0.0D);
        this.destination = options.to();
        this.route = buildRoute(options.from(), options.waypoints(), options.to());
        this.rgbColor = options.color();

        // Everything random about this stream comes from its seed, so the same options look the same.
        RandomSource streamRandom = RandomSource.create(options.seed());
        this.phase = streamRandom.nextFloat() * Mth.TWO_PI;
        double variation = Mth.clamp(streamRandom.nextGaussian() * RADIUS_VARIATION_SIGMA, -RADIUS_VARIATION_LIMIT, RADIUS_VARIATION_LIMIT);
        this.baseRadius = (float) (options.radius() * (1.0D + variation));

        this.capacity = BASE_TRAIL_POINTS + options.extend();
        this.head = options.from();
        this.hop = new Vec3(0.0D, options.verticalBoost() / TICKS_PER_SECOND, 0.0D);
        this.velocity = options.launchDirection()
                .map(direction -> direction.normalize().scale(CRUISE_SPEED))
                .orElseGet(() -> randomDirection(streamRandom).scale(CRUISE_SPEED * RANDOM_LAUNCH_SPEED_FACTOR));

        // Vanilla particle physics must not move the stream.
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;

        this.lifetime = computeLifetime(this.route, this.capacity);
        this.appendSample(this.head, 1.0F);
        this.syncPosition();
        this.setBoundingBox(buildBounds(this.route, this.baseRadius, options.verticalBoost()));
    }

    public static void setDropletsEnabled(boolean enabled) {
        dropletsEnabled = enabled;
    }

    public static boolean areDropletsEnabled() {
        return dropletsEnabled;
    }

    @Override
    public ParticleRenderType getGroup() {
        return RenderTypeRegistry.ESSENTIA_STREAM_GROUP;
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
        if (this.arrived) {
            this.drain();
        } else {
            this.flyTowardsDestination();
        }
    }

    // Whether a new spawn with the same endpoints and colour may extend this stream instead of creating a new one.
    // Only streams whose head is still flying can be extended; a draining stream is left alone.
    boolean canAbsorb() {
        return !this.arrived && this.isAlive();
    }

    // Grows the trail capacity and lifetime so a repeated transfer looks like one longer stream.
    void absorb(int extendTicks) {
        int grow = Math.max(extendTicks, MERGE_MIN_TICKS);
        this.capacity += grow;
        this.lifetime += grow;
    }

    /**
     * Builds the render snapshot for this frame.
     * <p>
     * While the head is flying, the newest trail point is interpolated between the last two ticks using partialTick.
     * Returns null if the stream has too few points to draw.
     */
    public @Nullable ExtrusionMesh renderData(float partialTickTime) {
        if (!this.isAlive() || this.trail.size() < MIN_RENDER_POINTS) {
            return null;
        }

        float time = this.age + partialTickTime;
        float dissolveTime = this.dissolveTicks > 0 ? this.dissolveTicks + partialTickTime : 0.0F;
        return EssentiaStreamMeshBuilder.build(this.snapshot(partialTickTime), this.baseRadius, this.rgbColor, time, this.phase, dissolveTime);
    }

    private List<TrailSample> snapshot(float partialTickTime) {
        ArrayList<TrailSample> samples = new ArrayList<>(this.trail);
        if (this.dissolveTicks == 0 && samples.size() > 1) {
            TrailSample newest = samples.get(0);
            Vec3 previous = samples.get(1).position();
            samples.set(0, new TrailSample(Mth.lerp(partialTickTime, previous, newest.position()), newest.arrival()));
        }
        return samples;
    }

    private void flyTowardsDestination() {
        Vec3 target = this.route.get(this.routeIndex);
        boolean finalLeg = this.isFinalLeg();
        while (!finalLeg && this.head.distanceTo(target) < WAYPOINT_REACH_DISTANCE) {
            this.routeIndex++;
            target = this.route.get(this.routeIndex);
            finalLeg = this.isFinalLeg();
        }

        Vec3 toTarget = target.subtract(this.head);
        double distance = toTarget.length();
        if (finalLeg && distance <= ARRIVE_STOP_DISTANCE) {
            this.arrive(target);
            return;
        }

        double desiredSpeed = CRUISE_SPEED;
        if (finalLeg) {
            desiredSpeed *= Mth.clamp(distance / SLOW_DISTANCE, SLOW_MIN_FACTOR, 1.0D);
        }
        // normalize() returns zero for a zero-length vector, so coincident points cannot produce NaNs here.
        Vec3 desiredVelocity = toTarget.normalize().scale(desiredSpeed);
        double steering = Mth.lerp(Mth.clamp(1.0D - distance / STEER_NEAR_DISTANCE, 0.0D, 1.0D), STEER_FAR, STEER_NEAR);
        Vec3 steered = this.velocity.add(desiredVelocity.subtract(this.velocity).scale(steering));
        Vec3 next = steered.scale(VELOCITY_DAMPING).add(0.0D, UPWARD_BIAS, 0.0D);
        double speed = next.length();
        this.velocity = speed > CRUISE_SPEED ? next.scale(CRUISE_SPEED / speed) : next;

        this.head = this.head.add(this.velocity).add(this.hop);
        this.hop = this.hop.scale(HOP_DECAY);
        float arrival = finalLeg ? arrivalRadiusFactor(distance) : 1.0F;
        this.appendSample(this.head, arrival);
        this.syncPosition();
        this.trimTrail();
        this.emitFlightDroplet();
    }

    private void arrive(Vec3 target) {
        this.head = target;
        this.velocity = Vec3.ZERO;
        this.hop = Vec3.ZERO;
        this.arrived = true;
        // The tip is appended with zero radius, so the stream visibly plunges into the destination.
        this.appendSample(target, 0.0F);
        this.syncPosition();
        this.trimTrail();
    }

    private void drain() {
        if (this.trail.size() <= 1) {
            this.remove();
            return;
        }

        this.trail.removeLast();
        this.dissolveTicks++;
        this.emitDissolveDroplet();
    }

    private boolean isFinalLeg() {
        return this.routeIndex == this.route.size() - 1;
    }

    private void appendSample(Vec3 position, float arrival) {
        this.trail.addFirst(new TrailSample(position, arrival));
    }

    private void trimTrail() {
        while (this.trail.size() > this.capacity) {
            this.trail.removeLast();
        }
    }

    private void syncPosition() {
        this.x = this.head.x;
        this.y = this.head.y;
        this.z = this.head.z;
    }

    private Vec3 trailPosition(int index) {
        int current = 0;
        for (TrailSample sample : this.trail) {
            if (current == index) {
                return sample.position();
            }
            current++;
        }
        return this.head;
    }

    private void emitFlightDroplet() {
        if (!dropletsEnabled || this.random.nextFloat() >= DROPLET_CHANCE) {
            return;
        }

        // Either just behind the head, or at a random point along the trail.
        int index = this.random.nextBoolean() ? 1 : this.random.nextInt(this.trail.size());
        Vec3 position = this.trailPosition(index);
        Vec3 drift = randomOffset(DROPLET_DRIFT).add(0.0D, DROPLET_BUOYANCY, 0.0D);
        this.spawnDroplet(position, drift);
    }

    private void emitDissolveDroplet() {
        if (!dropletsEnabled) {
            return;
        }

        Vec3 position = this.destination.add(randomOffset(DISSOLVE_SPREAD));
        Vec3 pull = this.destination.subtract(position).scale(DISSOLVE_PULL);
        this.spawnDroplet(position, pull);
    }

    private void spawnDroplet(Vec3 position, Vec3 velocity) {
        int age = DROPLET_MIN_AGE + this.random.nextInt(DROPLET_MAX_AGE - DROPLET_MIN_AGE + 1);
        float size = DROPLET_MIN_SIZE + this.random.nextFloat() * DROPLET_SIZE_RANGE;
        int color = ARGB.color(DROPLET_ALPHA, ARGB.red(this.rgbColor), ARGB.green(this.rgbColor), ARGB.blue(this.rgbColor));
        // The glow particle is reused for droplets, so no extra particle type is needed.
        GlowParticleOptions options = GlowParticleOptions.of(color).size(size).age(age);
        this.level.addParticle(options, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
    }

    private Vec3 randomOffset(double radius) {
        return new Vec3(
                (this.random.nextDouble() * 2.0D - 1.0D) * radius,
                (this.random.nextDouble() * 2.0D - 1.0D) * radius,
                (this.random.nextDouble() * 2.0D - 1.0D) * radius
        );
    }

    // Smooth shrink of the head radius as it approaches the destination on the final leg.
    private static float arrivalRadiusFactor(double distance) {
        return (float) Mth.smoothstep(Mth.clamp(distance / ARRIVE_BLEND_DISTANCE, 0.0D, 1.0D));
    }

    private static List<Vec3> buildRoute(Vec3 from, List<Vec3> waypoints, Vec3 to) {
        ArrayList<Vec3> route = new ArrayList<>(waypoints.size() + 2);
        route.add(from);
        route.addAll(waypoints);
        route.add(to);
        return List.copyOf(route);
    }

    private static Vec3 randomDirection(RandomSource random) {
        Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
        return direction.lengthSqr() > 0.0D ? direction : new Vec3(0.0D, 1.0D, 0.0D);
    }

    // Lifetime is about one second per block of route, plus the drain, plus a margin.
    private static int computeLifetime(List<Vec3> route, int capacity) {
        double routeLength = 0.0D;
        for (int index = 1; index < route.size(); index++) {
            routeLength += route.get(index - 1).distanceTo(route.get(index));
        }
        int flightTicks = (int) Math.ceil(routeLength / CRUISE_SPEED);
        return Math.max(MIN_LIFETIME_TICKS, flightTicks + capacity + LIFETIME_MARGIN_TICKS);
    }

    // Covers the route, the body radius, wobble, curve bulge and launch hop, so frustum culling never hides a visible stream.
    private static AABB buildBounds(List<Vec3> route, float radius, float verticalBoost) {
        double hopReach = Math.abs(verticalBoost) / TICKS_PER_SECOND / (1.0D - HOP_DECAY);
        double padding = radius * 2.0D + EssentiaStreamMeshBuilder.WOBBLE_AMPLITUDE + BOUNDS_MARGIN + hopReach;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (Vec3 point : route) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }
        return new AABB(minX - padding, minY - padding, minZ - padding, maxX + padding, maxY + padding, maxZ + padding);
    }

    /**
     * One point of the trail.
     *
     * @param position the centre-line position
     * @param arrival  radius factor in [0, 1] that is 1 in flight and shrinks to 0 at the final approach
     */
    record TrailSample(Vec3 position, float arrival) {
    }
}
