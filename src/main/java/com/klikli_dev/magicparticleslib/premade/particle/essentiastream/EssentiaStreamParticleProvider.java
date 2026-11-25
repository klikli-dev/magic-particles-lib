// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.essentiastream;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class EssentiaStreamParticleProvider implements ParticleProvider<EssentiaStreamParticleOptions> {
    // Live streams, keyed by their floored endpoints and colour. Repeated spawns that match a live stream extend it.
    private static final Map<MergeKey, EssentiaStreamParticle> LIVE_STREAMS = new HashMap<>();
    // The level the map belongs to. It is held weakly, and the map is cleared when a different level is used.
    private static WeakReference<ClientLevel> mergeLevel = new WeakReference<>(null);

    @Override
    public @Nullable Particle createParticle(EssentiaStreamParticleOptions options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
        resetIfLevelChanged(level);
        LIVE_STREAMS.values().removeIf(stream -> !stream.isAlive());

        MergeKey key = MergeKey.of(options);
        EssentiaStreamParticle existing = LIVE_STREAMS.get(key);
        if (existing != null && existing.canAbsorb()) {
            // Returning null tells the particle engine that nothing new was added.
            existing.absorb(options.extend());
            return null;
        }

        EssentiaStreamParticle stream = new EssentiaStreamParticle(level, options);
        LIVE_STREAMS.put(key, stream);
        return stream;
    }

    private static void resetIfLevelChanged(ClientLevel level) {
        if (mergeLevel.get() != level) {
            LIVE_STREAMS.clear();
            mergeLevel = new WeakReference<>(level);
        }
    }

    private record MergeKey(BlockPos from, BlockPos to, int color) {
        static MergeKey of(EssentiaStreamParticleOptions options) {
            return new MergeKey(
                    BlockPos.containing(options.from().x, options.from().y, options.from().z),
                    BlockPos.containing(options.to().x, options.to().y, options.to().z),
                    options.color()
            );
        }
    }
}
