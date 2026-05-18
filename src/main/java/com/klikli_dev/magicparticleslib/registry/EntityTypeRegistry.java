// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.registry;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import com.klikli_dev.magicparticleslib.premade.entity.branchingrift.BranchingRiftEntity;
import com.klikli_dev.magicparticleslib.premade.entity.rift.RiftEntity;
import com.klikli_dev.magicparticleslib.premade.projectile.glowtrail.GlowTrailProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class EntityTypeRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MagicParticlesLib.MODID);

    public static final Supplier<EntityType<GlowTrailProjectile>> GLOW_TRAIL_PROJECTILE = register(
            "glow_trail_projectile",
            EntityType.Builder.<GlowTrailProjectile>of(GlowTrailProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .noSummon()
                    .noSave()
                    .noLootTable()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
    );

    public static final Supplier<EntityType<RiftEntity>> RIFT = register(
            "rift",
            EntityType.Builder.<RiftEntity>of(RiftEntity::new, MobCategory.MISC)
                    .sized(4.0F, 4.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
    );

    public static final Supplier<EntityType<BranchingRiftEntity>> BRANCHING_RIFT = register(
            "branching_rift",
            EntityType.Builder.<BranchingRiftEntity>of(BranchingRiftEntity::new, MobCategory.MISC)
                    .sized(6.0F, 6.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
    );

    private EntityTypeRegistry() {
    }

    private static <T extends Entity> Supplier<EntityType<T>> register(String name, EntityType.Builder<T> builder) {
        return ENTITY_TYPES.register(name, () -> builder.build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, name))));
    }
}
