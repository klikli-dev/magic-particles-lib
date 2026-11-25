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
 * A stream of essentia that grows from its source toward its destination, then drains into it.
 * <p>
 * The stream is the part of the head's path between a back arc and the head. The head moves along the route at
 * about one block per second and its path is sampled every {@link #TRAIL_POINT_SPACING} blocks of arc length.
 * <ul>
 *     <li><b>Grow:</b> the back is anchored at the source, so the stream is a continuous tube from the source to the head.</li>
 *     <li><b>Comet:</b> once the stream reaches {@code maxLength}, the back moves with the head, so the stream keeps that length.</li>
 *     <li><b>Hold:</b> after the head arrives, the stream stays connected for the hold ticks, which {@code extend} sets.</li>
 *     <li><b>Drain:</b> the back then moves toward the destination at a constant speed until nothing is left.</li>
 * </ul>
 */
public class EssentiaStreamParticle extends Particle {
    // Head speed in blocks per second, and the same speed in blocks per tick.
    private static final double CRUISE_SPEED_BLOCKS_PER_SECOND = 1.0D;
    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double CRUISE_SPEED = CRUISE_SPEED_BLOCKS_PER_SECOND / TICKS_PER_SECOND;
    // Sampled points on the head's path are this far apart in arc length, in blocks.
    static final double TRAIL_POINT_SPACING = 0.1D;
    // Speed at which the back end retracts toward the destination while draining, in blocks per second.
    private static final double DRAIN_BLOCKS_PER_SECOND = 2.0D;
    // Drain speed in blocks per tick. The mesh builder uses it so the texture scroll matches the drain.
    static final double DRAIN_BLOCKS_PER_TICK = DRAIN_BLOCKS_PER_SECOND / TICKS_PER_SECOND;
    // Merging adds at least this many hold ticks.
    static final int MERGE_MIN_HOLD_TICKS = 5;
    // A merge into a draining stream is only allowed while at least this many sample points remain.
    static final int MIN_MERGE_REMAINING_POINTS = 3;
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
    // Points closer than this to the live head are not drawn separately, to avoid degenerate segments at the tip.
    // Also used for the back end when it is about to vanish.
    private static final double MIN_HEAD_SEPARATION = 0.02D;
    // The launch hop decays by this factor per tick. Total hop height is the boost divided by (1 - decay).
    private static final double HOP_DECAY = 0.8D;
    // Lifetime safety net: flight time at cruise speed, scaled for slow approaches, plus hold, drain and a margin.
    private static final double LIFETIME_FLIGHT_SLACK = 1.25D;
    private static final int LIFETIME_MARGIN_TICKS = 20;
    private static final int MIN_LIFETIME_TICKS = 40;
    // Streams with fewer render points are not drawn yet.
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
    private final Vec3 origin;
    private final List<Vec3> route;
    private final int rgbColor;
    private final float baseRadius;
    private final float phase;
    private final float maxLength;
    // Committed path samples at every TRAIL_POINT_SPACING of arc length, newest (head side) first.
    private final Deque<TrailSample> trail = new ArrayDeque<>();
    // Reused every frame to hold the render snapshot.
    private final ArrayList<TrailSample> snapshotBuffer = new ArrayList<>();
    private TrailSample previousHead;
    private TrailSample head;
    private double previousBackArc;
    // Arc length of the back end. 0 while anchored at the source. Only ever increases.
    private double backArc;
    // Arc length at which the next committed sample is placed.
    private double nextCommitArc;
    private Vec3 velocity;
    private Vec3 hop;
    private int routeIndex = 1;
    private boolean arrived;
    // Ticks left to stay connected at the destination before draining. Merges add to it.
    private int holdRemaining;
    // Number of drain ticks so far. Drives the texture scroll.
    private int dissolveTicks;

    public EssentiaStreamParticle(ClientLevel level, EssentiaStreamParticleOptions options) {
        // Zero motion: the stream is simulated by this class, not by the vanilla particle physics.
        super(level, options.from().x, options.from().y, options.from().z, 0.0D, 0.0D, 0.0D);
        this.destination = options.to();
        this.origin = options.from();
        this.route = buildRoute(options.from(), options.waypoints(), options.to());
        this.rgbColor = options.color();
        this.maxLength = options.maxLength();

        // Everything random about this stream comes from its seed, so the same options look the same.
        RandomSource streamRandom = RandomSource.create(options.seed());
        this.phase = streamRandom.nextFloat() * Mth.TWO_PI;
        double variation = Mth.clamp(streamRandom.nextGaussian() * RADIUS_VARIATION_SIGMA, -RADIUS_VARIATION_LIMIT, RADIUS_VARIATION_LIMIT);
        this.baseRadius = (float) (options.radius() * (1.0D + variation));

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

        // The head starts at the source at arc length 0. The source is committed so the trail always has an anchor.
        this.head = new TrailSample(options.from(), 1.0F, 0.0D);
        this.previousHead = this.head;
        this.backArc = 0.0D;
        this.previousBackArc = 0.0D;
        this.nextCommitArc = TRAIL_POINT_SPACING;
        this.trail.addFirst(this.head);
        this.holdRemaining = options.extend();
        this.lifetime = computeLifetime(this.route, this.holdRemaining, this.maxLength);
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
        this.previousHead = this.head;
        this.previousBackArc = this.backArc;
        if (!this.arrived) {
            this.flyTowardsDestination();
            this.trimTailToMaxLength();
        } else if (this.holdRemaining > 0) {
            this.holdRemaining--;
        } else {
            this.drain();
        }
        this.pruneTrail();
    }

    /**
     * Whether a new spawn with the same endpoints and colour may merge into this stream instead of creating a new one.
     * <p>
     * A flying stream always accepts a merge. A draining stream only accepts one while enough of it remains.
     */
    boolean canAbsorb() {
        if (!this.isAlive()) {
            return false;
        }
        return !this.arrived || this.remainingLength() >= MIN_MERGE_REMAINING_POINTS * TRAIL_POINT_SPACING;
    }

    /**
     * Merges a spawn into this stream: the hold grows by max(extendTicks, {@link #MERGE_MIN_HOLD_TICKS}) ticks,
     * and the lifetime grows by the same amount so the stream is never cut early.
     * While draining, the drain pauses for the added hold.
     */
    void absorb(int extendTicks) {
        int grow = Math.max(extendTicks, MERGE_MIN_HOLD_TICKS);
        this.holdRemaining += grow;
        this.lifetime += grow;
    }

    /**
     * Builds the render snapshot for this frame.
     * <p>
     * The first point is the live head, and the last point is the live back end. Both are interpolated between the last
     * two ticks using partialTick. Returns null if the stream has too few points to draw.
     * The mesh is relative to {@link #renderOrigin()}.
     */
    public @Nullable ExtrusionMesh renderData(float partialTickTime) {
        if (!this.isAlive()) {
            return null;
        }

        List<TrailSample> samples = this.snapshot(partialTickTime);
        if (samples.size() < MIN_RENDER_POINTS) {
            return null;
        }

        float time = this.age + partialTickTime;
        float dissolveTime = this.dissolveTicks > 0 ? this.dissolveTicks + partialTickTime : 0.0F;
        return EssentiaStreamMeshBuilder.build(samples, this.origin, this.baseRadius, this.rgbColor, time, this.phase, dissolveTime);
    }

    /**
     * The point the mesh from {@link #renderData(float)} is relative to. It is constant for the life of the stream.
     */
    public Vec3 renderOrigin() {
        return this.origin;
    }

    private List<TrailSample> snapshot(float partialTickTime) {
        // Reuses one buffer per stream. The mesh builder reads it fully before renderData returns.
        this.snapshotBuffer.clear();
        TrailSample front = this.liveFront(partialTickTime);
        double back = Mth.lerp(partialTickTime, this.previousBackArc, this.backArc);
        this.snapshotBuffer.add(front);

        for (TrailSample sample : this.trail) {
            // Samples past the live front belong to the segment that is still being interpolated.
            if (sample.arc() > front.arc()) {
                continue;
            }
            // The first sample at or behind the back end is the one the back end is interpolated from.
            if (sample.arc() <= back) {
                this.addBackPoint(sample, back);
                break;
            }
            // A sample that nearly coincides with the live front would create a degenerate segment at the tip.
            if (this.snapshotBuffer.size() == 1 && sample.position().distanceTo(front.position()) < MIN_HEAD_SEPARATION) {
                continue;
            }
            this.snapshotBuffer.add(sample);
        }
        return this.snapshotBuffer;
    }

    // Places the back end at arc length x, interpolated on the path between sample (behind it) and the last point drawn.
    private void addBackPoint(TrailSample sample, double x) {
        TrailSample last = this.snapshotBuffer.get(this.snapshotBuffer.size() - 1);
        if (last.arc() - x < MIN_HEAD_SEPARATION) {
            return;
        }
        double fraction = (x - sample.arc()) / (last.arc() - sample.arc());
        Vec3 position = Mth.lerp(fraction, sample.position(), last.position());
        float arrival = Mth.lerp((float) fraction, sample.arrival(), last.arrival());
        this.snapshotBuffer.add(new TrailSample(position, arrival, x));
    }

    private TrailSample liveFront(float partialTickTime) {
        return new TrailSample(
                Mth.lerp(partialTickTime, this.previousHead.position(), this.head.position()),
                Mth.lerp(partialTickTime, this.previousHead.arrival(), this.head.arrival()),
                Mth.lerp(partialTickTime, this.previousHead.arc(), this.head.arc())
        );
    }

    private double remainingLength() {
        return this.head.arc() - this.backArc;
    }

    private void flyTowardsDestination() {
        Vec3 target = this.route.get(this.routeIndex);
        boolean finalLeg = this.isFinalLeg();
        while (!finalLeg && this.head.position().distanceTo(target) < WAYPOINT_REACH_DISTANCE) {
            this.routeIndex++;
            target = this.route.get(this.routeIndex);
            finalLeg = this.isFinalLeg();
        }

        Vec3 toTarget = target.subtract(this.head.position());
        double distance = toTarget.length();
        if (finalLeg && distance <= ARRIVE_STOP_DISTANCE) {
            this.arrive();
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

        Vec3 position = this.head.position().add(this.velocity).add(this.hop);
        this.hop = this.hop.scale(HOP_DECAY);
        float arrival = finalLeg ? arrivalRadiusFactor(distance) : 1.0F;
        this.moveHead(position, arrival, finalLeg);
        this.emitFlightDroplet();
    }

    private void arrive() {
        this.moveHead(this.destination, 0.0F, true);
        // The tip is committed with zero radius, so the stream visibly plunges into the destination.
        this.trail.addFirst(this.head);
        this.velocity = Vec3.ZERO;
        this.hop = Vec3.ZERO;
        this.arrived = true;
    }

    // Moves the head and commits samples at every arc-length multiple of the spacing that the move crossed.
    // previousHead must already be the head from before this move (tick sets it).
    private void moveHead(Vec3 position, float arrival, boolean finalLeg) {
        double arc = this.head.arc() + this.head.position().distanceTo(position);
        this.head = new TrailSample(position, arrival, arc);
        this.syncPosition();

        // nextCommitArc is always greater than the previous head's arc, so the segment has positive length here.
        double segmentLength = this.head.arc() - this.previousHead.arc();
        while (this.nextCommitArc <= this.head.arc()) {
            double fraction = (this.nextCommitArc - this.previousHead.arc()) / segmentLength;
            Vec3 committed = Mth.lerp(fraction, this.previousHead.position(), this.head.position());
            float committedArrival = finalLeg ? arrivalRadiusFactor(committed.distanceTo(this.destination)) : 1.0F;
            this.trail.addFirst(new TrailSample(committed, committedArrival, this.nextCommitArc));
            this.nextCommitArc += TRAIL_POINT_SPACING;
        }
    }

    // Anchored while the stream is shorter than maxLength. Afterwards the back end follows the head at maxLength behind.
    private void trimTailToMaxLength() {
        this.backArc = Math.max(this.backArc, this.head.arc() - this.maxLength);
    }

    private void drain() {
        this.backArc = Math.min(this.backArc + DRAIN_BLOCKS_PER_TICK, this.head.arc());
        this.dissolveTicks++;
        this.emitDissolveDroplet();
        if (this.remainingLength() < MIN_HEAD_SEPARATION) {
            this.remove();
        }
    }

    // Drops samples that the back end has passed. Two spacings of slack keep the back end interpolable.
    private void pruneTrail() {
        double keepFrom = this.backArc - 2.0D * TRAIL_POINT_SPACING;
        while (this.trail.size() > 1 && this.trail.peekLast().arc() < keepFrom) {
            this.trail.removeLast();
        }
    }

    private boolean isFinalLeg() {
        return this.routeIndex == this.route.size() - 1;
    }

    private void syncPosition() {
        this.x = this.head.position().x;
        this.y = this.head.position().y;
        this.z = this.head.position().z;
    }

    private Vec3 trailPosition(int index) {
        int current = 0;
        for (TrailSample sample : this.trail) {
            if (current == index) {
                return sample.position();
            }
            current++;
        }
        return this.head.position();
    }

    private void emitFlightDroplet() {
        if (!dropletsEnabled || this.random.nextFloat() >= DROPLET_CHANCE) {
            return;
        }

        // Either just behind the head (the newest sample), or at a random point along the trail.
        int index = this.random.nextBoolean() ? 0 : this.random.nextInt(this.trail.size());
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

    // The longest drain is a full maxLength of stream, because the back end never trails the head by more than that.
    private static int drainTicks(double length) {
        return (int) Math.ceil(length / DRAIN_BLOCKS_PER_TICK);
    }

    // Lifetime covers the flight, the hold, a full drain, and a margin.
    private static int computeLifetime(List<Vec3> route, int holdTicks, double maxLength) {
        double routeLength = 0.0D;
        for (int index = 1; index < route.size(); index++) {
            routeLength += route.get(index - 1).distanceTo(route.get(index));
        }
        int flightTicks = (int) Math.ceil(routeLength / CRUISE_SPEED * LIFETIME_FLIGHT_SLACK);
        return Math.max(MIN_LIFETIME_TICKS, flightTicks + holdTicks + drainTicks(maxLength) + LIFETIME_MARGIN_TICKS);
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
     * One point on the head's path.
     *
     * @param position the centre-line position
     * @param arrival  radius factor in [0, 1]. It is 1 in flight and shrinks to 0 at the destination
     * @param arc      distance travelled along the head's path when this point was placed, in blocks
     */
    record TrailSample(Vec3 position, float arrival, double arc) {
    }
}
