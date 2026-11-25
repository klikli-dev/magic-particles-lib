# Essentia Stream Particle

The essentia stream particle renders a glowing, tapered liquid tube. It grows from a source toward a destination, stays connected for a short hold, then drains into the destination. Typical uses are jars or devices feeding an altar, pipes announcing a transfer, or an urn pouring water (use a blue color and a small `verticalBoost` for a launch hop).

## Registry id

`magicparticleslib:essentia_stream`

## Implementation overview

Relevant classes:

- [`EssentiaStreams`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreams.java): public spawn API
- [`EssentiaStreamParticleOptions`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleOptions.java)
- [`EssentiaStreamParticleType`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleType.java)
- [`EssentiaStreamParticleProvider`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleProvider.java): merges repeated spawns into live streams
- [`EssentiaStreamParticle`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticle.java): simulates the head, the back end and the drain at 20 Hz
- [`EssentiaStreamMeshBuilder`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamMeshBuilder.java): turns the samples into a tube mesh
- [`EssentiaStreamParticleGroup`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleGroup.java) and [`EssentiaStreamRenderState`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamRenderState.java)

The stream is a single extruded tube (8 sides) swept along the head's path. It is not made of billboard particles. It is drawn with a dedicated translucent pipeline in [`RenderTypeRegistry`](../../src/main/java/com/klikli_dev/magicparticleslib/registry/RenderTypeRegistry.java), with alpha blending, depth test on, depth write off and no backface culling. Like the electric arc, it is registered as a special particle provider and rendered through a particle group.

## How a stream works

A stream is the part of the head's path between a **back end** and the **head**. The path is sampled every 0.1 blocks of arc length, so the tube is continuous whatever the head's speed.

1. **Grow:** the head starts at the source and flies toward the destination at about 1 block per second, with the same soft steering, launch direction and hop as before. The back end stays **anchored at the source**, so the stream is one continuous tube from the source to the head.
2. **Comet:** once the stream reaches `maxLength`, the back end follows the head at `maxLength` behind it. The stream keeps that length and moves like a comet.
3. **Arrival:** the head radius shrinks to zero within about 1 block of the destination, and the head stops on it.
4. **Hold:** the stream stays connected at the destination for `extend` ticks (see below).
5. **Drain:** the back end then retracts toward the destination at 2 blocks per second. The liquid pours into the target, with a texture scroll that matches the drain speed and droplets at the destination.

The tail is interpolated between ticks, so the drain and the comet trim move smoothly.

### Shape

- The head tip swells from zero radius over about 0.35 blocks.
- The tail tapers to zero over about 1 block. While the back end is still anchored at the source, the taper is only 0.3 blocks, so the tube seems to come out of the source.
- Wobble fades in from the back end, so it is zero at an anchored source.
- If the whole stream is shorter than the tapers, the tapers scale down.
- The body pulses and ripples in thickness, and its colour shimmers subtly. These effects travel along the stream in blocks.
- The texture is 0.075 repeats per 0.1-block point, so 0.75 repeats per block. It stays fixed relative to the head while growing and only scrolls while draining.

## Particle Options

`EssentiaStreamParticleOptions` defines the stream.

| Field | Type | Default | Description |
|---|---|---:|---|
| `from` | `Vec3` | `Vec3.ZERO` | Source of the stream. The back end is anchored here while the stream is shorter than `maxLength`. The options carry the source, so the same options work with `sendParticles` |
| `to` | `Vec3` | `Vec3.ZERO` | Destination. The head plunges into it and the stream drains into it |
| `color` | `int` / RGB | required | Packed RGB color, usually the aspect color |
| `seed` | `int` | random | Seed for launch direction, radius variation and phase |
| `radius` | `float` | `0.12` | Body radius in blocks (roughly 0.1 to 0.15 looks right) |
| `extend` | `int` | `0` | **Hold ticks.** How long the stream stays connected at the destination after the head arrives, before it drains |
| `verticalBoost` | `float` | `0.0` | Upward launch pop in blocks per second, decaying quickly. Around `2.0` gives a visible hop |
| `waypoints` | `List<Vec3>` | empty | Optional points the stream passes through before `to`. Pathfinding around walls is not included |
| `launchDirection` | `Optional<Vec3>` | empty | Optional direction to leave `from` in at cruise speed. Empty means a seeded random direction |
| `maxLength` | `float` | `16.0` | Maximum length of the stream in blocks. Once reached, the tail follows the head as a comet of this length. Minimum `0.5` |

Use `EssentiaStreamParticleOptions.of(from, to, color)`, `of(from, to, color, radius, extend)` or `of(from, to, color, radius, extend, maxLength)` for the common cases. Construct the record directly for waypoints, a launch direction or a vertical boost.

### Default timings

- Flight: about 1 second per block of route.
- Drain: 2 blocks per second. A full 16-block stream drains in about 8 seconds. A 3-block tail drains in about 1.5 seconds.
- Lifetime: flight time (with slack) + hold + a full drain of `maxLength` + a margin, so the stream is never cut early.

## Behavior

The stream:

- leaves the source with a pointed tip and flies at about 1 block per second along a soft curve that bulges slightly upward
- grows from the source as one continuous tube, or trails the head as a comet once it reaches `maxLength`
- spawns small glow droplets while flying and at the destination while draining
- is fullbright, translucent and visible from inside
- is removed once it has drained, or when its lifetime ends

### Merging

Spawning a stream with the same source block, destination block and color while a matching stream is still alive merges into it, and no copy is created. The merge adds `max(extend, 5)` hold ticks to the stream, and it extends the lifetime by the same amount. This keeps one stream connected for continuous transfers:

- If the stream is still flying, the hold applies after it arrives.
- If the stream is draining, the drain pauses for the added hold ticks. Merging only happens while at least 3 sample points (0.3 blocks) remain. Otherwise a new stream is created.

Once the spawns stop, the stream drains normally. The merged spawn's `maxLength` and `radius` are ignored; the existing stream keeps its own.

## Use in Code

See https://docs.neoforged.net/docs/resources/client/particles/#spawning-particles.

On the client:

```java
Vec3 from = new Vec3(10.5, 65.0, 10.5);
Vec3 to = new Vec3(14.5, 66.0, 10.5);
EssentiaStreams.spawn(level, from, to, 0x7A4CFF);
```

With a custom radius, hold and maximum length:

```java
EssentiaStreams.spawn(level, from, to, 0x7A4CFF, 0.15F, 20, 8.0F);
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
        Optional.empty(),
        16.0F
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
/mpl spawn essentia_stream 40 0.15 8
```

Helper command parameters, in order. Each optional argument needs the ones before it:

- `extend`: hold ticks at the destination after arrival (default `0`)
- `radius`: body radius in blocks, between `0.01` and `1.0` (default `0.12`)
- `maxLength`: maximum stream length in blocks, between `0.5` and `64` (default `16`)

The stream flies from roughly the player's eye to the block or entity being looked at (up to 16 blocks away). Each use picks a random color.
