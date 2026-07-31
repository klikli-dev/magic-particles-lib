// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.projectile;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spawns visual-only entities on the local client.
 * <p>
 * This is intended for effects that should not exist on the server, such as temporary particle-driving entities.
 */
public final class VisualEntitySpawner {
    private static final AtomicInteger CLIENT_ENTITY_COUNTER = new AtomicInteger();

    private VisualEntitySpawner() {
    }

    public static void spawn(Level level, Entity entity) {
        spawn(level, entity, true);
    }

    public static void spawn(Level level, Entity entity, boolean onlyInTickingChunks) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return;
        }

        DistHelper.spawn(level, entity, onlyInTickingChunks);
    }

    private static final class DistHelper {
        private DistHelper() {
        }

        private static void spawn(Level level, Entity entity, boolean onlyInTickingChunks) {
            if (!(level instanceof ClientLevel clientLevel)) {
                return;
            }

            var sectionPos = SectionPos.asLong(entity.blockPosition());
            var section = clientLevel.entityStorage.sectionStorage.getOrCreateSection(sectionPos);
            if (onlyInTickingChunks && !section.getStatus().isTicking()) {
                return;
            }

            // MC 26.2: ClientLevel.getNextEntityId() returns 0, which is the sentinel for "unassigned".
            // Entity.getId() throws if id == 0, so we must assign a valid client-side ID before adding.
            entity.setId(CLIENT_ENTITY_COUNTER.incrementAndGet());
            clientLevel.addEntity(entity);
        }
    }
}
