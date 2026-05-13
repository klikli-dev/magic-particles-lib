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

/**
 * Spawns visual-only entities on the local client.
 * <p>
 * This is intended for effects that should not exist on the server, such as temporary particle-driving entities.
 */
public final class VisualEntitySpawner {
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

            clientLevel.addEntity(entity);
        }
    }
}
