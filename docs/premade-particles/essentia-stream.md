# Essentia Stream Particle

The essentia stream particle renders a glowing, tapered liquid stream that flies from a source to a destination and drains into it. Typical uses are jars or devices feeding an altar, pipes announcing a transfer, or an urn pouring water (use a blue color and a small `verticalBoost` for a launch hop).

## Registry id

`magicparticleslib:essentia_stream`

## Implementation overview

Relevant classes:

- [`EssentiaStreams`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreams.java): public spawn API
- [`EssentiaStreamParticleOptions`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleOptions.java)
- [`EssentiaStreamParticleType`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleType.java)
- [`EssentiaStreamParticleProvider`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleProvider.java): merges repeated spawns into live streams
- [`EssentiaStreamParticle`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticle.java): simulates the head and trail at 20 Hz
- [`EssentiaStreamMeshBuilder`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamMeshBuilder.java): turns the trail into a tube mesh
- [`EssentiaStreamParticleGroup`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleGroup.java) and [`EssentiaStreamRenderState`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamRenderState.java)

The stream is a single extruded tube (8 sides) swept along the trail of a moving head, so it is not made of billboard particles. It is drawn with a dedicated translucent pipeline in [`RenderTypeRegistry`](../../src/main/java/com/klikli_dev/magicparticleslib/registry/RenderTypeRegistry.java), with alpha blending, depth test on, depth write off and no backface culling. Like the electric arc, it is registered as a special particle provider and rendered through a particle group.

## Particle Options

`EssentiaStreamParticleOptions` defines the stream.

| Field | Type | Default | Description |
|---|---|---:|---|
| `from` | `Vec3` | `Vec3.ZERO` | Source of the stream. The options carry the source, so the same options work with `sendParticles` |
| `to` | `Vec3` | `Vec3.ZERO` | Destination. The head plunges into it and the trail drains into it |
| `color` | `int` / RGB | required | Packed RGB color, usually the aspect color |
| `seed` | `int` | random | Seed for launch direction, radius variation and phase |
| `radius` | `float` | `0.12` | Body radius in blocks (roughly 0.1 to 0.15 looks right) |
| `extend` | `int` | `0` | Extra trail length and drain time, in ticks |
| `verticalBoost` | `float` | `0.0` | Upward launch pop in blocks per second. Around `2.0` gives a visible hop |
| `waypoints` | `List<Vec3>` | empty | Optional points the stream passes through before `to`. Pathfinding around walls is not included |
| `launchDirection` | `Optional<Vec3>` | empty | Optional direction to leave `from` in at cruise speed. Empty means a seeded random direction |

Use `EssentiaStreamParticleOptions.of(from, to, color)` or `of(from, to, color, radius, extend)` for the common cases, or construct the record directly for waypoints, launch direction, or a vertical boost.

## Behavior

The stream:

- leaves the source with a pointed tip and flies at about 1 block per second along a soft curve that bulges slightly upward
- has a body that pulses and ripples in thickness, and a brightness shimmer that travels along it
- wobbles its centre line by a few hundredths of a block so it looks alive while the head is idle
- shrinks its head radius over the last block of the flight, so the tip plunges into the destination
- then drains: one trail point is removed per tick from the source end, and the texture scrolls toward the destination
- spawns small glow droplets while flying and at the destination while draining
- is fullbright, translucent and visible from inside
- is removed when its lifetime ends or the trail has fully drained

Lifetime is about one second per block of route, plus the trail length and a margin, so a stream is not cut off before it drains.

### Merging

Spawning a stream with the same source block, destination block and color while a matching stream is still flying does not create a copy. Instead, the existing stream grows its trail capacity and lifetime by `max(extend, 5)` ticks. This makes continuous transfers look like one long stream. Streams that have already arrived and are draining are not merged into; a new stream is created instead.

## Use in Code

See https://docs.neoforged.net/docs/resources/client/particles/#spawning-particles.

On the client:

```java
Vec3 from = new Vec3(10.5, 65.0, 10.5);
Vec3 to = new Vec3(14.5, 66.0, 10.5);
EssentiaStreams.spawn(level, from, to, 0x7A4CFF);
```

With all options:

```java
EssentiaStreamParticleOptions options = new EssentiaStreamParticleOptions(
        from,
        to,
        0x2F7BFF,
        level.random.nextInt(),
        0.12F,
        20,
        2.0F,
        List.of(new Vec3(12.5, 68.0, 10.5)),
        Optional.empty()
);
EssentiaStreams.spawn(level, options);
```

On a server, send the same options to nearby players with `ServerLevel.sendParticles`:

```java
serverLevel.sendParticles(options, from.x, from.y, from.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
```

Droplets can be turned off globally with `EssentiaStreams.setDropletsEnabled(false)`.

## Command usage

Example helper command for easier local testing:

```mcfunction
/mpl spawn essentia_stream
/mpl spawn essentia_stream 40
/mpl spawn essentia_stream 40 0.15
```

Helper command parameters:

- `extend`: extra trail length and drain time in ticks (default `0`)
- `radius`: body radius in blocks, between `0.01` and `1.0` (default `0.12`)

The stream flies from roughly the player's eye to the block or entity being looked at (up to 16 blocks away). Each use picks a random color.
