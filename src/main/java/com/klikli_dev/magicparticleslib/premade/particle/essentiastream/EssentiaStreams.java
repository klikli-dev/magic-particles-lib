// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.essentiastream;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Public entry point for spawning essentia streams.
 * <p>
 * Spawning the same endpoints and colour while a stream is still flying extends that stream instead of
 * creating a copy. Servers can send {@link EssentiaStreamParticleOptions} with {@code ServerLevel.sendParticles}.
 */
public final class EssentiaStreams {
    private EssentiaStreams() {
    }

    /**
     * Spawns a stream with the default radius and no extension.
     *
     * @param level the client level to spawn in
     * @param from  start of the stream
     * @param to    destination the stream drains into
     * @param color packed RGB color, usually the aspect color
     */
    public static void spawn(ClientLevel level, Vec3 from, Vec3 to, int color) {
        spawn(level, EssentiaStreamParticleOptions.of(from, to, color));
    }

    /**
     * Spawns a stream with a custom radius, hold and maximum length.
     *
     * @param level     the client level to spawn in
     * @param from      start of the stream
     * @param to        destination the stream drains into
     * @param color     packed RGB color
     * @param radius    body radius in blocks
     * @param extend    hold ticks at the destination after the head arrives
     * @param maxLength maximum stream length in blocks before the tail follows the head as a comet
     */
    public static void spawn(ClientLevel level, Vec3 from, Vec3 to, int color, float radius, int extend, float maxLength) {
        spawn(level, EssentiaStreamParticleOptions.of(from, to, color, radius, extend, maxLength));
    }

    /**
     * Spawns a stream with a custom radius, hold, maximum length and speed.
     *
     * @param level     the client level to spawn in
     * @param from      start of the stream
     * @param to        destination the stream drains into
     * @param color     packed RGB color
     * @param radius    body radius in blocks
     * @param extend    hold ticks at the destination after the head arrives
     * @param maxLength maximum stream length in blocks before the tail follows the head as a comet
     * @param speed     head speed in blocks per second, clamped to the options' range
     */
    public static void spawn(ClientLevel level, Vec3 from, Vec3 to, int color, float radius, int extend, float maxLength, float speed) {
        spawn(level, EssentiaStreamParticleOptions.of(from, to, color, radius, extend, maxLength, speed));
    }

    /**
     * Spawns a stream with full control over its options.
     *
     * @param level   the client level to spawn in
     * @param options the stream options; the source is {@link EssentiaStreamParticleOptions#from()}
     */
    public static void spawn(ClientLevel level, EssentiaStreamParticleOptions options) {
        Vec3 from = options.from();
        level.addParticle(options, from.x, from.y, from.z, 0.0D, 0.0D, 0.0D);
    }

    /**
     * Enables or disables the droplets that spawn along flying streams and at their destinations.
     * Droplets are enabled by default.
     *
     * @param enabled whether droplets should spawn
     */
    public static void setDropletsEnabled(boolean enabled) {
        EssentiaStreamParticle.setDropletsEnabled(enabled);
    }

    public static boolean areDropletsEnabled() {
        return EssentiaStreamParticle.areDropletsEnabled();
    }
}
