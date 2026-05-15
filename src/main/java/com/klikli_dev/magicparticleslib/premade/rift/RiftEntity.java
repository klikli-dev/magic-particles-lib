// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.rift;

import com.mojang.serialization.Codec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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

public class RiftEntity extends Entity {
    public static final int DEFAULT_SEED = 0;
    public static final int DEFAULT_SIZE = 18;
    public static final float DEFAULT_VISUAL_INTENSITY = 0.8F;
    public static final float MIN_VISUAL_INTENSITY = 0.0F;
    public static final float MAX_VISUAL_INTENSITY = 2.0F;

    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VISUAL_INTENSITY = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.FLOAT);

    private RiftShape shape = RiftShape.EMPTY;

    public RiftEntity(EntityType<? extends RiftEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
        this.regenerateShape();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, DEFAULT_SEED);
        builder.define(SIZE, DEFAULT_SIZE);
        builder.define(VISUAL_INTENSITY, DEFAULT_VISUAL_INTENSITY);
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
        output.store("Size", Codec.INT, this.size());
        output.store("VisualIntensity", Codec.FLOAT, this.visualIntensity());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(SEED, input.read("Seed", Codec.INT).orElse(DEFAULT_SEED));
        this.entityData.set(SIZE, input.read("Size", Codec.INT).orElse(DEFAULT_SIZE));
        this.entityData.set(VISUAL_INTENSITY, clampVisualIntensity(input.read("VisualIntensity", Codec.FLOAT).orElse(DEFAULT_VISUAL_INTENSITY)));
        this.regenerateShape();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (accessor.equals(SEED) || accessor.equals(SIZE) || accessor.equals(VISUAL_INTENSITY)) {
            // The rendered mesh is fully derived from synced parameters, so any change invalidates the cached shape.
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

    public RiftShape shape() {
        return this.shape;
    }

    public int seed() {
        return this.entityData.get(SEED);
    }

    public void setSeed(int seed) {
        this.entityData.set(SEED, seed);
    }

    public int size() {
        return this.entityData.get(SIZE);
    }

    public void setSizeValue(int size) {
        if (size != this.size()) {
            this.entityData.set(SIZE, size);
            this.refreshDimensions();
        }
    }

    public float visualIntensity() {
        return this.entityData.get(VISUAL_INTENSITY);
    }

    public void setVisualIntensity(float visualIntensity) {
        this.entityData.set(VISUAL_INTENSITY, clampVisualIntensity(visualIntensity));
    }

    public void initializeRandomized(int seed, int size, float visualIntensity) {
        this.entityData.set(SEED, seed);
        this.entityData.set(SIZE, size);
        this.entityData.set(VISUAL_INTENSITY, clampVisualIntensity(visualIntensity));
        this.regenerateShape();
    }

    private void regenerateShape() {
        // The entity keeps a precomputed local-space skeleton so rendering does not have to regenerate geometry every frame.
        this.shape = RiftShapeGenerator.generate(this.seed(), this.size());
        this.updateBoundsFromShape();
    }

    private void updateBoundsFromShape() {
        Vec3 position = this.position();
        if (this.shape.isEmpty()) {
            this.setBoundingBox(new AABB(position, position));
            return;
        }

        // The generated shape is local to the entity origin, so shift its bounds into world space here.
        this.setBoundingBox(this.shape.bounds().move(position));
    }

    private static float clampVisualIntensity(float visualIntensity) {
        return Mth.clamp(visualIntensity, MIN_VISUAL_INTENSITY, MAX_VISUAL_INTENSITY);
    }
}
