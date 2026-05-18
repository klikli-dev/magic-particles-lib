// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.entity.branchingrift;

import com.mojang.serialization.Codec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BranchingRiftEntity extends Entity {
    public static final int DEFAULT_SEED = 0;
    public static final int DEFAULT_SKELETON_SIZE = 24;
    public static final float DEFAULT_VOLUME = 1.0F;
    public static final float DEFAULT_VISUAL_INTENSITY = 0.8F;
    public static final int DEFAULT_BRANCH_COUNT = 3;
    public static final float DEFAULT_JAGGEDNESS = 1.0F;
    public static final float DEFAULT_TAPER = 0.6F;

    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(BranchingRiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKELETON_SIZE = SynchedEntityData.defineId(BranchingRiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VOLUME = SynchedEntityData.defineId(BranchingRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VISUAL_INTENSITY = SynchedEntityData.defineId(BranchingRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> BRANCH_COUNT = SynchedEntityData.defineId(BranchingRiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> JAGGEDNESS = SynchedEntityData.defineId(BranchingRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TAPER = SynchedEntityData.defineId(BranchingRiftEntity.class, EntityDataSerializers.FLOAT);

    private BranchingRiftShape shape = BranchingRiftShape.EMPTY;

    public BranchingRiftEntity(EntityType<? extends BranchingRiftEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
        this.regenerateShape();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, DEFAULT_SEED);
        builder.define(SKELETON_SIZE, DEFAULT_SKELETON_SIZE);
        builder.define(VOLUME, DEFAULT_VOLUME);
        builder.define(VISUAL_INTENSITY, DEFAULT_VISUAL_INTENSITY);
        builder.define(BRANCH_COUNT, DEFAULT_BRANCH_COUNT);
        builder.define(JAGGEDNESS, DEFAULT_JAGGEDNESS);
        builder.define(TAPER, DEFAULT_TAPER);
    }

    @Override
    public void tick() {
        this.baseTick();
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("Seed", Codec.INT, this.seed());
        output.store("SkeletonSize", Codec.INT, this.skeletonSize());
        output.store("Volume", Codec.FLOAT, this.volume());
        output.store("VisualIntensity", Codec.FLOAT, this.visualIntensity());
        output.store("BranchCount", Codec.INT, this.branchCount());
        output.store("Jaggedness", Codec.FLOAT, this.jaggedness());
        output.store("Taper", Codec.FLOAT, this.taper());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(SEED, input.read("Seed", Codec.INT).orElse(DEFAULT_SEED));
        int legacySize = input.read("Size", Codec.INT).orElse(DEFAULT_SKELETON_SIZE);
        // Older saves only had a single size field, so use it as the fallback for the renamed skeleton field.
        this.entityData.set(SKELETON_SIZE, input.read("SkeletonSize", Codec.INT).orElse(legacySize));
        this.entityData.set(VOLUME, input.read("Volume", Codec.FLOAT).orElse((float) legacySize / DEFAULT_SKELETON_SIZE));
        this.entityData.set(VISUAL_INTENSITY, input.read("VisualIntensity", Codec.FLOAT).orElse(DEFAULT_VISUAL_INTENSITY));
        this.entityData.set(BRANCH_COUNT, input.read("BranchCount", Codec.INT).orElse(DEFAULT_BRANCH_COUNT));
        this.entityData.set(JAGGEDNESS, input.read("Jaggedness", Codec.FLOAT).orElse(DEFAULT_JAGGEDNESS));
        this.entityData.set(TAPER, input.read("Taper", Codec.FLOAT).orElse(DEFAULT_TAPER));
        this.regenerateShape();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (accessor.equals(SEED) || accessor.equals(SKELETON_SIZE) || accessor.equals(VOLUME) || accessor.equals(VISUAL_INTENSITY) || accessor.equals(BRANCH_COUNT) || accessor.equals(JAGGEDNESS) || accessor.equals(TAPER)) {
            // The branching mesh is derived entirely from synced parameters, so any change invalidates the cached shape.
            this.regenerateShape();
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public void refreshDimensions() {
        super.refreshDimensions();
        this.regenerateShape();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.getType().getDimensions();
    }

    public BranchingRiftShape shape() {
        return this.shape;
    }

    public int seed() {
        return this.entityData.get(SEED);
    }

    public void setSeed(int seed) {
        this.entityData.set(SEED, seed);
        this.regenerateShape();
    }

    public int skeletonSize() {
        return this.entityData.get(SKELETON_SIZE);
    }

    public void setSkeletonSize(int skeletonSize) {
        if (skeletonSize != this.skeletonSize()) {
            this.entityData.set(SKELETON_SIZE, skeletonSize);
            this.refreshDimensions();
        }
    }

    public float volume() {
        return this.entityData.get(VOLUME);
    }

    public void setVolume(float volume) {
        this.entityData.set(VOLUME, volume);
        this.regenerateShape();
    }

    public float visualIntensity() {
        return this.entityData.get(VISUAL_INTENSITY);
    }

    public void setVisualIntensity(float visualIntensity) {
        this.entityData.set(VISUAL_INTENSITY, visualIntensity);
    }

    public int branchCount() {
        return this.entityData.get(BRANCH_COUNT);
    }

    public void setBranchCount(int branchCount) {
        this.entityData.set(BRANCH_COUNT, branchCount);
        this.regenerateShape();
    }

    public float jaggedness() {
        return this.entityData.get(JAGGEDNESS);
    }

    public void setJaggedness(float jaggedness) {
        this.entityData.set(JAGGEDNESS, jaggedness);
        this.regenerateShape();
    }

    public float taper() {
        return this.entityData.get(TAPER);
    }

    public void setTaper(float taper) {
        this.entityData.set(TAPER, taper);
        this.regenerateShape();
    }

    public void initialize(int seed, int skeletonSize, float volume, float visualIntensity, int branchCount, float jaggedness, float taper) {
        this.entityData.set(SEED, seed);
        this.entityData.set(SKELETON_SIZE, skeletonSize);
        this.entityData.set(VOLUME, volume);
        this.entityData.set(VISUAL_INTENSITY, visualIntensity);
        this.entityData.set(BRANCH_COUNT, branchCount);
        this.entityData.set(JAGGEDNESS, jaggedness);
        this.entityData.set(TAPER, taper);
        this.regenerateShape();
    }

    private void regenerateShape() {
        // Cache the generated branch hierarchy until synced inputs change.
        this.shape = BranchingRiftShapeGenerator.generate(this.seed(), this.skeletonSize(), this.volume(), this.branchCount(), this.jaggedness(), this.taper());
        this.updateBoundsFromShape();
    }

    private void updateBoundsFromShape() {
        Vec3 position = this.position();
        if (this.shape.isEmpty()) {
            this.setBoundingBox(new AABB(position, position));
            return;
        }

        // The generated bounds are centered around the entity origin, so move them into world space here.
        this.setBoundingBox(this.shape.bounds().move(position));
    }
}
