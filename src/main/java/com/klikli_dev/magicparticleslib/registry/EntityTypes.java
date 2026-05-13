// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.registry;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import com.klikli_dev.magicparticleslib.premade.projectile.GlowTrailProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class EntityTypes {
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

    private EntityTypes() {
    }

    private static <T extends Entity> Supplier<EntityType<T>> register(String name, EntityType.Builder<T> builder) {
        return ENTITY_TYPES.register(name, () -> builder.build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, name))));
    }
}
