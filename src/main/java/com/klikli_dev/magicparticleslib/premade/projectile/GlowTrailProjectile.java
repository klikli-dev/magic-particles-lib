// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.projectile;

import com.klikli_dev.magicparticleslib.premade.glow.GlowParticleOptions;
import com.klikli_dev.magicparticleslib.registry.EntityTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class GlowTrailProjectile extends Entity {
    public static final int DEFAULT_MAX_AGE = 500;
    public static final int DEFAULT_TRAIL_AGE = 50;
    public static final float DEFAULT_SIZE = 0.25F;
    public static final float DEFAULT_SPEED = 0.3F;
    public static final float DEFAULT_ARRIVAL_DISTANCE = 1.0F;
    public static final float DEFAULT_TRAIL_ALPHA = 0.75F;
    public static final float DEFAULT_EXTRA_DESPAWN_DISTANCE = 10.0F;

    private static final Consumer<GlowTrailProjectile> NO_OP_ARRIVAL = projectile -> {
    };

    public static final EntityDataAccessor<Vector3fc> FROM = SynchedEntityData.defineId(GlowTrailProjectile.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Vector3fc> TO = SynchedEntityData.defineId(GlowTrailProjectile.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Integer> START_COLOR = SynchedEntityData.defineId(GlowTrailProjectile.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> END_COLOR = SynchedEntityData.defineId(GlowTrailProjectile.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(GlowTrailProjectile.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> SPAWN_IMPACT_PARTICLES = SynchedEntityData.defineId(GlowTrailProjectile.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> MAX_TRAVEL_DISTANCE = SynchedEntityData.defineId(GlowTrailProjectile.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ARRIVAL_DISTANCE = SynchedEntityData.defineId(GlowTrailProjectile.class, EntityDataSerializers.FLOAT);

    private int age;
    private long spawnTime = -1L;
    private Consumer<GlowTrailProjectile> onArrival = NO_OP_ARRIVAL;
    private boolean autoMaxTravelDistance = true;

    public GlowTrailProjectile(Level level, Vec3 from, Vec3 to) {
        this(EntityTypes.GLOW_TRAIL_PROJECTILE.get(), level);
        this.path(from, to);
    }

    public GlowTrailProjectile(EntityType<? extends GlowTrailProjectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public GlowTrailProjectile path(Vec3 from, Vec3 to) {
        this.entityData.set(FROM, toVector(from));
        this.entityData.set(TO, toVector(to));
        this.setPos(from);
        if (this.autoMaxTravelDistance) {
            this.entityData.set(MAX_TRAVEL_DISTANCE, (float) from.distanceTo(to) + DEFAULT_EXTRA_DESPAWN_DISTANCE);
        }
        return this;
    }

    public GlowTrailProjectile color(int color) {
        return this.colors(color, color);
    }

    public GlowTrailProjectile colors(int startColor, int endColor) {
        return this.startColor(startColor).endColor(endColor);
    }

    public GlowTrailProjectile startColor(int color) {
        this.entityData.set(START_COLOR, color);
        return this;
    }

    public int startColor() {
        return this.entityData.get(START_COLOR);
    }

    public GlowTrailProjectile endColor(int color) {
        this.entityData.set(END_COLOR, color);
        return this;
    }

    public int endColor() {
        return this.entityData.get(END_COLOR);
    }

    public GlowTrailProjectile size(float size) {
        this.entityData.set(SIZE, Math.max(0.0F, size));
        return this;
    }

    public GlowTrailProjectile initialVelocity(Vec3 velocity) {
        this.setDeltaMovement(velocity);
        return this;
    }

    public float size() {
        return this.entityData.get(SIZE);
    }

    public GlowTrailProjectile spawnImpactParticles(boolean spawnImpactParticles) {
        this.entityData.set(SPAWN_IMPACT_PARTICLES, spawnImpactParticles);
        return this;
    }

    public boolean spawnImpactParticles() {
        return this.entityData.get(SPAWN_IMPACT_PARTICLES);
    }

    public GlowTrailProjectile maxTravelDistance(float maxTravelDistance) {
        this.autoMaxTravelDistance = false;
        this.entityData.set(MAX_TRAVEL_DISTANCE, Math.max(0.0F, maxTravelDistance));
        return this;
    }

    public float maxTravelDistance() {
        return this.entityData.get(MAX_TRAVEL_DISTANCE);
    }

    public GlowTrailProjectile arrivalDistance(float arrivalDistance) {
        this.entityData.set(ARRIVAL_DISTANCE, Math.max(0.01F, arrivalDistance));
        return this;
    }

    public GlowTrailProjectile onArrival(Consumer<GlowTrailProjectile> onArrival) {
        this.onArrival = onArrival == null ? NO_OP_ARRIVAL : onArrival;
        return this;
    }

    public float arrivalDistance() {
        return this.entityData.get(ARRIVAL_DISTANCE);
    }

    public Vec3 from() {
        return toVec3(this.entityData.get(FROM));
    }

    public Vec3 to() {
        return toVec3(this.entityData.get(TO));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FROM, new Vector3f());
        builder.define(TO, new Vector3f());
        builder.define(START_COLOR, ARGB.color(255, 255, 25, 180));
        builder.define(END_COLOR, ARGB.color(255, 255, 25, 180));
        builder.define(SIZE, DEFAULT_SIZE);
        builder.define(SPAWN_IMPACT_PARTICLES, false);
        builder.define(MAX_TRAVEL_DISTANCE, DEFAULT_EXTRA_DESPAWN_DISTANCE);
        builder.define(ARRIVAL_DISTANCE, DEFAULT_ARRIVAL_DISTANCE);
    }

    @Override
    public void tick() {
        this.baseTick();

        this.age++;
        if (this.age > DEFAULT_MAX_AGE || this.hasExpired()) {
            this.discard();
            return;
        }

        Vec3 from = this.from();
        Vec3 to = this.to();
        Vec3 motion = this.getDeltaMovement();
        double distanceToTarget = this.position().distanceTo(to);

        if (this.hasArrived(distanceToTarget, motion.length())) {
            this.arriveAt(to);
            return;
        }

        if (this.position().distanceTo(from) > this.maxTravelDistance()) {
            this.discard();
            return;
        }

        Vec3 guidedMotion = this.guideTowards(to, motion);
        this.setPos(this.position().add(guidedMotion));
        this.setDeltaMovement(guidedMotion);

        if (this.level().isClientSide() && this.age > 1) {
            this.spawnTrail(from, to);
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        this.spawnTime = this.level().getGameTime();
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    private boolean hasExpired() {
        return this.spawnTime >= 0L && this.level().getGameTime() - this.spawnTime > DEFAULT_MAX_AGE;
    }

    private boolean hasArrived(double distanceToTarget, double motionLength) {
        return distanceToTarget <= Math.max(this.arrivalDistance(), motionLength);
    }

    private void arriveAt(Vec3 target) {
        this.setPos(target);

        if (this.level().isClientSide() && this.spawnImpactParticles()) {
            this.spawnImpactParticles(target);
        }

        Consumer<GlowTrailProjectile> callback = this.onArrival;
        this.onArrival = NO_OP_ARRIVAL;
        try {
            callback.accept(this);
        } finally {
            this.discard();
        }
    }

    private Vec3 guideTowards(Vec3 target, Vec3 currentMotion) {
        Vec3 toTarget = target.subtract(this.position());
        double length = toTarget.length();
        if (length <= 1.0E-6D) {
            return currentMotion;
        }

        Vec3 targetMotion = toTarget.scale(DEFAULT_SPEED / length);
        double weight = length <= 3.0D ? 0.9D * ((3.0D - length) / 3.0D) : 0.0D;

        return new Vec3(
                (0.9D - weight) * currentMotion.x + (0.1D + weight) * targetMotion.x,
                (0.9D - weight) * currentMotion.y + (0.1D + weight) * targetMotion.y,
                (0.9D - weight) * currentMotion.z + (0.1D + weight) * targetMotion.z
        );
    }

    private void spawnTrail(Vec3 from, Vec3 to) {
        double deltaX = this.getX() - this.xOld;
        double deltaY = this.getY() - this.yOld;
        double deltaZ = this.getZ() - this.zOld;
        float segmentCount = (float) (Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 8.0F);
        if (segmentCount <= 0.0F) {
            return;
        }

        double totalDistance = from.distanceTo(to);
        float progress = totalDistance <= 1.0E-6D
                ? 1.0F
                : (float) Mth.clamp(this.position().distanceTo(from) / totalDistance, 0.0D, 1.0D);
        int trailColor = withAlpha(interpolateColor(this.startColor(), this.endColor(), progress), DEFAULT_TRAIL_ALPHA);

        for (int i = 0; i <= Mth.ceil(segmentCount); i++) {
            float coefficient = i / segmentCount;
            this.level().addParticle(
                    GlowParticleOptions.create(trailColor, true, true, this.size(), DEFAULT_TRAIL_AGE),
                    this.getX() + deltaX * coefficient,
                    this.getY() + deltaY * coefficient,
                    this.getZ() + deltaZ * coefficient,
                    0.0125F * (this.random.nextFloat() - 0.5F),
                    0.0125F * (this.random.nextFloat() - 0.5F),
                    0.0125F * (this.random.nextFloat() - 0.5F)
            );
        }
    }

    private void spawnImpactParticles(Vec3 target) {
        int impactColor = withAlpha(this.endColor(), DEFAULT_TRAIL_ALPHA);
        for (int i = 0; i < 8; i++) {
            this.level().addParticle(
                    GlowParticleOptions.create(impactColor, true, true, this.size(), DEFAULT_TRAIL_AGE),
                    target.x,
                    target.y,
                    target.z,
                    0.05F * (this.random.nextFloat() - 0.5F),
                    0.05F * (this.random.nextFloat() - 0.5F),
                    0.05F * (this.random.nextFloat() - 0.5F)
            );
        }
    }

    private static int interpolateColor(int startColor, int endColor, float progress) {
        float clampedProgress = Mth.clamp(progress, 0.0F, 1.0F);
        return ARGB.color(
                Math.round(Mth.lerp(clampedProgress, ARGB.alpha(startColor), ARGB.alpha(endColor))),
                Math.round(Mth.lerp(clampedProgress, ARGB.red(startColor), ARGB.red(endColor))),
                Math.round(Mth.lerp(clampedProgress, ARGB.green(startColor), ARGB.green(endColor))),
                Math.round(Mth.lerp(clampedProgress, ARGB.blue(startColor), ARGB.blue(endColor)))
        );
    }

    private static int withAlpha(int color, float alpha) {
        return ARGB.color(Mth.clamp((int) (alpha * 255.0F), 0, 255), ARGB.red(color), ARGB.green(color), ARGB.blue(color));
    }

    private static Vector3f toVector(Vec3 vec) {
        return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
    }

    private static Vec3 toVec3(Vector3fc vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }
}
