// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.essentiastream;

import com.klikli_dev.magicparticleslib.extrusion.ExtrusionMesh;
import com.klikli_dev.magicparticleslib.premade.particle.bubble.BubbleParticleOptions;
import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
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
 * A worm-shaped stream of essentia that flies from its source to its destination, then drains into it.
 * <p>
 * The head moves one step per tick, and it leaves a sample on its path about every {@link #TRAIL_POINT_SPACING} blocks.
 * Each sample records the tick it was laid. The body is the part of the path the head flew in the last {@code length}
 * ticks, so its length is measured in time, not in blocks. The back end then trails the head by that many ticks.
 * <ul>
 *     <li><b>Fly:</b> the head accelerates toward its waypoint, with the speed clamped per axis, so diagonal motion is
 *     allowed to be a little faster than the axes. Each tick it lays samples and the body follows at {@code length} ticks.</li>
 *     <li><b>Arrive:</b> on the final leg the radius of newly laid samples thins to nothing, and the head snaps onto the destination.</li>
 *     <li><b>Drain:</b> once arrived, the back end retracts one tick of body per tick, so the stream empties into the destination.</li>
 * </ul>
 */
public class EssentiaStreamParticle extends Particle {
    // Per-tick velocity damping, which keeps the path soft.
    private static final double DAMPING = 0.985D;
    // Steering pull toward the waypoint, as a fraction of cruise speed per tick.
    private static final double PULL_FACTOR = 0.2D;
    // Smallest and largest distance used when scaling the pull, so near targets are snapped into hard.
    private static final double STEER_MIN_DISTANCE = 0.05D;
    private static final double STEER_MAX_DISTANCE = 1.0D;
    // Constant upward push per tick, as a fraction of cruise speed, so flights bulge slightly upward.
    private static final double RISE_FACTOR = 0.04D;
    // Launch speed without a direction, as a fraction of cruise speed, with the axis components set by the seed.
    private static final double LAUNCH_SPEED_FACTOR = 0.3D;
    // Launch wobble with a direction, as a fraction of cruise speed.
    private static final double LAUNCH_WOBBLE_FACTOR = 0.1D;
    // Sampled points on the head's path are this far apart in arc length, in blocks.
    static final double TRAIL_POINT_SPACING = 0.1D;
    // Distance at which an intermediate waypoint counts as reached.
    private static final double WAYPOINT_REACH_DISTANCE = 0.4D;
    // The head counts as arrived within this distance, and it is snapped onto the destination.
    private static final double ARRIVE_STOP_DISTANCE = 0.05D;
    // Within this distance of the destination, newly laid samples thin by sin(distance * pi / 2) per tick.
    private static final double ARRIVE_BLEND_DISTANCE = 1.0D;
    // Radius pulse per sample: radius * (0.75 + 0.25 * sin((seed + birth) / 2)), frozen when the sample is laid.
    private static final double PULSE_MIN = 0.75D;
    private static final double PULSE_RANGE = 0.25D;
    private static final double PULSE_DIVISOR = 2.0D;
    // Merging adds at least this many ticks.
    static final int MERGE_MIN_TICKS = 5;
    // A merge into a draining stream is only allowed while at least this many ticks of body remain.
    private static final double MIN_MERGE_REMAINING_TICKS = 3.0D;
    // Points closer than this to the live head are not drawn separately, to avoid degenerate segments at the tip.
    private static final double MIN_HEAD_SEPARATION = 0.02D;
    // The back end is not placed this close, in ticks, to the sample ahead of it, to avoid degenerate segments.
    private static final double MIN_BACK_SEPARATION_TICKS = 0.01D;
    // Lifetime safety net: flight time at cruise speed, times a slack for steering, plus the body and a margin.
    // The lifetime only has to be generous: a stream that has drained is removed anyway.
    private static final double LIFETIME_FLIGHT_SLACK = 4.0D;
    private static final int LIFETIME_MARGIN_TICKS = 100;
    private static final int MIN_LIFETIME_TICKS = 40;
    // Streams with fewer render points are not drawn yet.
    private static final int MIN_RENDER_POINTS = 3;
    // Body radius variation is gaussian with this sigma (relative), clamped to the limit.
    private static final double RADIUS_VARIATION_SIGMA = 0.15D;
    private static final double RADIUS_VARIATION_LIMIT = 0.3D;
    // Extra bounds padding for curve bulge and other drift the mesh might reach beyond its route.
    private static final double BOUNDS_MARGIN = 2.0D;
    // Droplets: chance per tick while flying, and their lifetime in ticks.
    private static final float DROPLET_CHANCE = 0.5F;
    private static final int DROPLET_MIN_AGE = 20;
    private static final int DROPLET_MAX_AGE = 29;
    // Droplet scale in tenths of a block: quads are scale times 0.1.
    private static final float DROPLET_MIN_SCALE = 0.4F;
    private static final float DROPLET_SCALE_RANGE = 0.2F;
    // Flying droplets drift with a gaussian velocity of this sigma, per axis.
    private static final double DROPLET_DRIFT_SIGMA = 0.005D;
    // Dissolve droplets appear around the destination with this gaussian offset, and then float up.
    private static final double DISSOLVE_SPREAD_SIGMA = 0.075D;

    // Global switch for droplets, so callers can turn them off.
    private static volatile boolean dropletsEnabled = true;

    private final Vec3 destination;
    private final Vec3 origin;
    private final List<Vec3> route;
    private final int rgbColor;
    private final int seed;
    private final float baseRadius;
    // Head speed in blocks per tick, and the per-axis velocity limit (the upward limit also covers the launch boost).
    private final double cruiseSpeed;
    private final double verticalLimit;
    // Committed path samples, newest (head side) first.
    private final Deque<TrailSample> trail = new ArrayDeque<>();
    // Reused every frame to hold the render snapshot.
    private final ArrayList<TrailSample> snapshotBuffer = new ArrayList<>();
    private TrailSample head;
    private TrailSample previousHead;
    // Arc length at which the next sample is laid.
    private double nextCommitArc = TRAIL_POINT_SPACING;
    // Birth time of the back end, in ticks. Samples older than this are not drawn. Only ever increases.
    private double cutTime;
    private double previousCutTime;
    // Base radius of samples laid from now on. It thins on the final leg and is zero once the stream has arrived.
    private double thinning;
    // Body length in ticks. Starts at max(length, extend) and grows with merges.
    private double bodyTicks;
    // Ticks a merge added while draining, during which the back end does not retract.
    private int extraDrainTicks;
    private Vec3 velocity;
    private int routeIndex = 1;
    private boolean arrived;

    public EssentiaStreamParticle(ClientLevel level, EssentiaStreamParticleOptions options) {
        // Zero motion: the stream is simulated by this class, not by the vanilla particle physics.
        super(level, options.from().x, options.from().y, options.from().z, 0.0D, 0.0D, 0.0D);
        this.destination = options.to();
        this.origin = options.from();
        this.route = buildRoute(options.from(), options.waypoints(), options.to());
        this.rgbColor = options.color();
        this.seed = options.seed();
        this.cruiseSpeed = options.speed() / EssentiaStreamParticleOptions.TICKS_PER_SECOND;
        double boostPerTick = options.verticalBoost() / EssentiaStreamParticleOptions.TICKS_PER_SECOND;
        this.verticalLimit = Math.max(this.cruiseSpeed, Math.abs(boostPerTick));

        // Everything random about this stream comes from its seed, so the same options look the same.
        RandomSource streamRandom = RandomSource.create(options.seed());
        double variation = Mth.clamp(streamRandom.nextGaussian() * RADIUS_VARIATION_SIGMA, -RADIUS_VARIATION_LIMIT, RADIUS_VARIATION_LIMIT);
        this.baseRadius = (float) (options.radius() * (1.0D + variation));
        this.thinning = this.baseRadius;
        this.bodyTicks = Math.max(options.length(), options.extend());
        this.velocity = launchVelocity(options, streamRandom, this.cruiseSpeed, boostPerTick);

        // Vanilla particle physics must not move the stream.
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;

        // The source is the first sample, so the body always has an anchor at the source.
        this.head = this.sampleAt(options.from(), 0.0D, 0.0D);
        this.previousHead = this.head;
        this.trail.addFirst(this.head);
        this.previousCutTime = 0.0D;
        this.cutTime = 0.0D;
        this.lifetime = computeLifetime(this.route, this.bodyTicks, this.cruiseSpeed);
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
        this.previousCutTime = this.cutTime;
        if (!this.arrived) {
            this.flyTowardsDestination();
            // The back end is at most bodyTicks behind the head. It never moves backward.
            this.cutTime = Math.max(this.cutTime, this.age - this.bodyTicks);
        } else {
            this.drain();
        }
        this.pruneTrail();
    }

    /**
     * Whether a new spawn with the same endpoints and colour may merge into this stream instead of creating a new one.
     * <p>
     * A flying stream always accepts a merge. A draining stream only accepts one while enough of its body remains.
     */
    boolean canAbsorb() {
        if (!this.isAlive()) {
            return false;
        }
        return !this.arrived || this.remainingBodyTicks() >= MIN_MERGE_REMAINING_TICKS;
    }

    /**
     * Merges a spawn into this stream: the body grows by max(extendTicks, {@link #MERGE_MIN_TICKS}) ticks, and the
     * lifetime grows by the same amount so the stream is never cut early. A draining stream keeps its back end still
     * for that many ticks, so the added body is drained after the rest.
     */
    void absorb(int extendTicks) {
        int grow = Math.max(extendTicks, MERGE_MIN_TICKS);
        this.bodyTicks += grow;
        this.lifetime += grow;
        if (this.arrived) {
            this.extraDrainTicks += grow;
        }
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

        double time = this.age + partialTickTime;
        double back = Mth.lerp(partialTickTime, this.previousCutTime, this.cutTime);
        // Each ring's texture V follows its own birth time, so the texture flows continuously, including while draining.
        return EssentiaStreamMeshBuilder.build(samples, this.origin, time, back, this.rgbColor);
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
        double back = Mth.lerp(partialTickTime, this.previousCutTime, this.cutTime);
        this.snapshotBuffer.add(front);

        for (TrailSample sample : this.trail) {
            // Samples past the live front belong to the segment that is still being interpolated.
            if (sample.arc() > front.arc()) {
                continue;
            }
            // The first sample at or behind the back end is the one the back end is interpolated from.
            if (sample.birth() <= back) {
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

    // Places the back end at birth time x, interpolated between sample (behind it) and the last point drawn.
    private void addBackPoint(TrailSample sample, double x) {
        TrailSample last = this.snapshotBuffer.get(this.snapshotBuffer.size() - 1);
        double span = last.birth() - sample.birth();
        if (span <= 0.0D || last.birth() - x < MIN_BACK_SEPARATION_TICKS) {
            return;
        }
        double fraction = (x - sample.birth()) / span;
        this.snapshotBuffer.add(new TrailSample(
                Mth.lerp(fraction, sample.position(), last.position()),
                Mth.lerp(fraction, sample.arc(), last.arc()),
                x,
                (float) Mth.lerp(fraction, sample.base(), last.base()),
                (float) Mth.lerp(fraction, sample.radius(), last.radius())
        ));
    }

    // The live front is interpolated between the last two ticks, so its birth is too. That keeps its texture V continuous across ticks.
    private TrailSample liveFront(float partialTickTime) {
        return new TrailSample(
                Mth.lerp(partialTickTime, this.previousHead.position(), this.head.position()),
                Mth.lerp(partialTickTime, this.previousHead.arc(), this.head.arc()),
                Mth.lerp(partialTickTime, this.previousHead.birth(), this.head.birth()),
                this.head.base(),
                this.head.radius()
        );
    }

    private double remainingBodyTicks() {
        return this.head.birth() - this.cutTime;
    }

    private void flyTowardsDestination() {
        Vec3 target = this.route.get(this.routeIndex);
        boolean finalLeg = this.isFinalLeg();
        // At high speed the head can move more than the reach distance in one tick, so the reach is at least two ticks of travel.
        double waypointReach = Math.max(WAYPOINT_REACH_DISTANCE, 2.0D * this.cruiseSpeed * Math.sqrt(3.0D));
        while (!finalLeg && this.head.position().distanceTo(target) < waypointReach) {
            this.routeIndex++;
            target = this.route.get(this.routeIndex);
            finalLeg = this.isFinalLeg();
        }

        Vec3 toTarget = target.subtract(this.head.position());
        double distance = toTarget.length();
        // The head also arrives when its last step was longer than the remaining distance, so it cannot pass the destination.
        if (finalLeg && distance <= Math.max(ARRIVE_STOP_DISTANCE, this.velocity.length())) {
            this.arrive();
            return;
        }

        // Pull toward the waypoint. Its strength grows as the head gets close, so it snaps into targets rather than easing in.
        double pull = PULL_FACTOR * this.cruiseSpeed / Mth.clamp(distance, STEER_MIN_DISTANCE, STEER_MAX_DISTANCE);
        Vec3 acceleration = toTarget.normalize().scale(pull);
        Vec3 next = this.velocity.add(acceleration).add(0.0D, RISE_FACTOR * this.cruiseSpeed, 0.0D).scale(DAMPING);
        this.velocity = clampPerAxis(next, this.cruiseSpeed, this.verticalLimit);

        // On the final leg, newly laid samples thin out with the distance, compounding each tick.
        if (finalLeg && distance < ARRIVE_BLEND_DISTANCE) {
            this.thinning *= Math.sin(distance * Math.PI / 2.0D);
        }

        this.moveHead(this.head.position().add(this.velocity));
        this.emitFlightDroplet();
    }

    private void arrive() {
        // The stream is fully thinned on arrival, so the last samples are invisible and the drain starts.
        this.thinning = 0.0D;
        this.moveHead(this.destination);
        this.velocity = Vec3.ZERO;
        this.arrived = true;
    }

    // Moves the head and lays samples at every arc-length multiple of the spacing that the move crossed.
    // Each sample's birth is the tick the head was at that point, so the body is measured in time.
    private void moveHead(Vec3 position) {
        TrailSample from = this.head;
        double arc = from.arc() + from.position().distanceTo(position);
        this.head = this.sampleAt(position, arc, this.age);
        this.syncPosition();

        // nextCommitArc is always greater than the previous head's arc, so the segment has positive length here.
        double segmentLength = this.head.arc() - from.arc();
        while (this.nextCommitArc <= this.head.arc()) {
            double fraction = (this.nextCommitArc - from.arc()) / segmentLength;
            Vec3 committed = Mth.lerp(fraction, from.position(), this.head.position());
            // The head was at "from" at the start of this tick, so the sample's birth is between the two ticks.
            double birth = (this.age - 1) + fraction;
            this.trail.addFirst(this.sampleAt(committed, this.nextCommitArc, birth));
            this.nextCommitArc += TRAIL_POINT_SPACING;
        }
    }

    // A sample at this position, laid at this arc and birth time. Its radius is frozen now: the pulse, times the thinning on the final leg.
    private TrailSample sampleAt(Vec3 position, double arc, double birth) {
        float base = (float) this.thinning;
        float pulsed = (float) (this.thinning * (PULSE_MIN + PULSE_RANGE * Math.sin((this.seed + birth) / PULSE_DIVISOR)));
        return new TrailSample(position, arc, birth, base, pulsed);
    }

    private void drain() {
        if (this.extraDrainTicks > 0) {
            this.extraDrainTicks--;
        } else {
            this.cutTime += 1.0D;
        }
        this.emitDissolveDroplet();
        if (this.cutTime >= this.head.birth()) {
            this.remove();
        }
    }

    // Drops samples that the back end has passed. The back end interpolates from the sample just behind it,
    // so the samples down to a tick below the earlier of the two back ends are kept.
    private void pruneTrail() {
        double keepFrom = Math.min(this.previousCutTime, this.cutTime) - 1.0D;
        while (this.trail.size() > 1 && this.trail.peekLast().birth() < keepFrom) {
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

    private List<TrailSample> bodySamples() {
        ArrayList<TrailSample> body = new ArrayList<>(this.trail.size());
        for (TrailSample sample : this.trail) {
            if (sample.birth() >= this.cutTime) {
                body.add(sample);
            }
        }
        return body;
    }

    private void emitFlightDroplet() {
        if (!dropletsEnabled || this.random.nextFloat() >= DROPLET_CHANCE) {
            return;
        }

        // Half the time, a droplet comes off one of the three oldest samples. Otherwise it comes off the second newest.
        List<TrailSample> body = this.bodySamples();
        if (body.isEmpty()) {
            return;
        }
        TrailSample sample;
        if (this.random.nextBoolean()) {
            int oldest = Math.min(3, body.size());
            sample = body.get(body.size() - 1 - this.random.nextInt(oldest));
        } else {
            sample = body.get(Math.min(1, body.size() - 1));
        }
        this.spawnDroplet(sample.position(), this.gaussianVector(DROPLET_DRIFT_SIGMA));
    }

    private void emitDissolveDroplet() {
        if (!dropletsEnabled) {
            return;
        }

        // Droplets are not pulled in: they start at the destination and float up on their own buoyancy.
        Vec3 position = this.destination.add(this.gaussianVector(DISSOLVE_SPREAD_SIGMA));
        this.spawnDroplet(position, Vec3.ZERO);
    }

    private void spawnDroplet(Vec3 position, Vec3 velocity) {
        int age = DROPLET_MIN_AGE + this.random.nextInt(DROPLET_MAX_AGE - DROPLET_MIN_AGE + 1);
        float scale = DROPLET_MIN_SCALE + this.random.nextFloat() * DROPLET_SCALE_RANGE;
        BubbleParticleOptions options = BubbleParticleOptions.of(this.rgbColor, scale, age);
        this.level.addParticle(options, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
    }

    private Vec3 gaussianVector(double sigma) {
        return new Vec3(
                this.random.nextGaussian() * sigma,
                this.random.nextGaussian() * sigma,
                this.random.nextGaussian() * sigma
        );
    }

    private static Vec3 clampPerAxis(Vec3 velocity, double limit, double verticalLimit) {
        return new Vec3(
                Mth.clamp(velocity.x, -limit, limit),
                Mth.clamp(velocity.y, -verticalLimit, verticalLimit),
                Mth.clamp(velocity.z, -limit, limit)
        );
    }

    // With a launch direction, the head starts along it. Without one, it starts almost at rest, with a seeded direction.
    // The vertical boost is added as initial upward velocity, so it bends the whole path up.
    private static Vec3 launchVelocity(EssentiaStreamParticleOptions options, RandomSource random, double cruiseSpeed, double boostPerTick) {
        Vec3 launch;
        if (options.launchDirection().isPresent()) {
            Vec3 direction = options.launchDirection().get().normalize().scale(cruiseSpeed);
            launch = direction.add(
                    random.nextGaussian() * LAUNCH_WOBBLE_FACTOR * cruiseSpeed,
                    random.nextGaussian() * LAUNCH_WOBBLE_FACTOR * cruiseSpeed,
                    random.nextGaussian() * LAUNCH_WOBBLE_FACTOR * cruiseSpeed
            );
        } else {
            int seed = options.seed();
            double scale = LAUNCH_SPEED_FACTOR * cruiseSpeed;
            launch = new Vec3(Math.sin(seed / 4.0D), Math.sin(seed / 3.0D), Math.sin(seed / 2.0D)).scale(scale);
        }
        return launch.add(0.0D, boostPerTick, 0.0D);
    }

    private static List<Vec3> buildRoute(Vec3 from, List<Vec3> waypoints, Vec3 to) {
        ArrayList<Vec3> route = new ArrayList<>(waypoints.size() + 2);
        route.add(from);
        route.addAll(waypoints);
        route.add(to);
        return List.copyOf(route);
    }

    // Lifetime covers the flight at the stream's speed with slack, the body that drains afterwards, and a margin.
    private static int computeLifetime(List<Vec3> route, double bodyTicks, double cruiseSpeed) {
        double routeLength = 0.0D;
        for (int index = 1; index < route.size(); index++) {
            routeLength += route.get(index - 1).distanceTo(route.get(index));
        }
        int flightTicks = (int) Math.ceil(routeLength / cruiseSpeed * LIFETIME_FLIGHT_SLACK);
        return Math.max(MIN_LIFETIME_TICKS, flightTicks + (int) Math.ceil(bodyTicks) + LIFETIME_MARGIN_TICKS);
    }

    // Covers the route, the body radius, wobble, curve bulge and launch boost, so frustum culling never hides a visible stream.
    private static AABB buildBounds(List<Vec3> route, float radius, float verticalBoost) {
        // Upward boost decays with damping, so it reaches roughly boost / (1 - damping) at most, in blocks per tick.
        double boostReach = Math.abs(verticalBoost) / EssentiaStreamParticleOptions.TICKS_PER_SECOND / (1.0D - DAMPING);
        double padding = radius * 2.0D + EssentiaStreamMeshBuilder.WOBBLE_AMPLITUDE + BOUNDS_MARGIN + boostReach;
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
     * One sample on the head's path.
     *
     * @param position the centre-line position
     * @param arc      distance travelled along the head's path when this point was placed, in blocks
     * @param birth    the tick the sample was laid, in ticks since the stream was created (fractional between ticks)
     * @param base     the stream's base radius when the sample was laid, before the pulse. The head cap blends with it
     * @param radius   the radius of the sample: the pulse times the base at the time it was laid
     */
    record TrailSample(Vec3 position, double arc, double birth, float base, float radius) {
    }
}
