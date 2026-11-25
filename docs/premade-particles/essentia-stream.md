# Essentia Stream Particle

The essentia stream particle renders a glowing liquid stream: a worm-shaped body that flies from a source to a destination, then drains into it. Typical uses are jars or devices feeding an altar, pipes announcing a transfer, mirrors that relay a stream, or an urn pouring water (use a blue color and a `verticalBoost` for a visible bulge).

## Registry ids

- `magicparticleslib:essentia_stream`: the stream
- `magicparticleslib:bubble`: the droplets the stream spawns (see [Droplets](#droplets))

## Implementation overview

Relevant classes:

- [`EssentiaStreams`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreams.java): public spawn API
- [`EssentiaStreamParticleOptions`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleOptions.java): the options, their codecs, and a `Builder` for ports and waypoints
- [`StreamPort`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/StreamPort.java): exit and entry points on block faces
- [`EssentiaStreamParticleType`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleType.java)
- [`EssentiaStreamParticleProvider`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleProvider.java): merges repeated spawns into live streams
- [`EssentiaStreamParticle`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticle.java): simulates the head, the body and the drain, 20 times per second
- [`EssentiaStreamMeshBuilder`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamMeshBuilder.java): turns the samples into a tube mesh
- [`EssentiaStreamParticleGroup`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamParticleGroup.java) and [`EssentiaStreamRenderState`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/essentiastream/EssentiaStreamRenderState.java)
- [`BubbleParticle`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/particle/bubble/BubbleParticle.java): the droplets

The stream is a single extruded tube (8 sides) swept along the head's path. It is not made of billboard particles. It is drawn with a dedicated translucent pipeline in [`RenderTypeRegistry`](../../src/main/java/com/klikli_dev/magicparticleslib/registry/RenderTypeRegistry.java), with alpha blending, depth test on, depth write off and no backface culling. Like the electric arc, it is registered as a special particle provider and rendered through a particle group.

## How a stream works

A stream is a **body** of samples laid along the head's path. The body is measured in **ticks of travel**, not blocks: it is the part of the path the head flew during the last `length` ticks.

1. **Launch:** the head starts at the source. With a launch direction it leaves along that direction at cruise speed. Without one, it starts almost at rest, pointing in a direction set by the seed. A `verticalBoost` adds an upward velocity on top.
2. **Fly:** the head steers toward its waypoint each tick and lays a sample about every 0.1 blocks of arc length. Each sample records the tick it was laid, so the body is the last `length` ticks behind the head. Early in the flight the body is the whole path from the source, so the stream grows out of the source like a worm.
3. **Arrive:** on the final leg, within 1 block of the destination, the radius of newly laid samples is multiplied by `sin(distance × π / 2)` every tick. The radius therefore shrinks to nothing quickly, and the head snaps onto the destination.
4. **Drain:** once arrived, the back end retracts one tick of body per tick, so the body empties into the destination. The texture keeps flowing through the body as it drains. The stream is removed when the body is gone.

The body is interpolated between ticks, so the motion is smooth.

### Motion

Constants below are per tick. The cruise speed is `speed / 20` blocks per tick.

- **Steering:** each tick the velocity gains `0.2 × cruise / clamp(distance, 0.05, 1)` in the direction of the waypoint. The pull grows as the head gets close, so it snaps into targets rather than easing in. There is no final slow-down.
- **Rise:** a constant upward `0.04 × cruise` is added every tick, so flights bulge slightly up.
- **Damping:** the velocity is multiplied by `0.985` each tick.
- **Clamp:** each axis is clamped to `±cruise` separately, so diagonal motion is a little faster than axis-aligned motion. The vertical limit is the larger of cruise and the vertical boost.
- **Launch:** with a launch direction, the velocity is `direction × cruise` plus a small wobble of `0.1 × cruise`. Without one, it is `(sin(seed / 4), sin(seed / 3), sin(seed / 2)) × 0.3 × cruise`.

### Shape

Each sample's radius is set when it is laid. Then it is shaped when it is drawn:

- **Pulse (frozen):** the sample radius is `radius × (0.75 + 0.25 × sin((seed + birth) / 2))`, where `birth` is the tick the sample was laid. The body is a string of beads that stays where it was laid. The stream's radius also varies by about ±15% per stream.
- **Head:** the first tick at the head is radius zero. The radius then blends up to the full body over about four ticks, so the head is short and rounded.
- **Tail:** the last 10 ticks of the body taper to zero with a cosine. The tail is always tapered, including at the source while the body is still growing.
- **Ripple:** the radius varies by `±20%`, with the phase set by the sample's age in ticks, so the ripples bunch where the head was slow and stretch where it was fast.
- **Wobble:** the centre line moves by up to 0.03 blocks per axis, at a different period per axis.
- **Shimmer:** the colour only darkens, by up to 10%, following the same age-based phase.
- **Texture:** the texture V of each ring is `0.075` per tick since its sample was laid, so the texture follows time, not distance. At the default speed that is about 1.2 repeats per block, and it stays the same when the speed changes. It flows continuously along the body, while flying and while draining.

### Arrival and merges

- A stream is removed once its body has drained. Its lifetime is a safety net only (see below).
- Spawning the same endpoints and colour while a stream is still alive merges into it: the body grows by `max(extend, 5)` ticks, and the lifetime grows by the same amount. A draining stream keeps its back end still for those ticks, so the added body drains after the rest.
- A merge into a draining stream is only accepted while at least 3 ticks of body remain. Otherwise a new stream is created.
- The merged spawn's `length`, `radius` and `speed` are ignored; the existing stream keeps its own.

## Particle Options

`EssentiaStreamParticleOptions` defines the stream.

| Field | Type | Default | Description |
|---|---|---:|---|
| `from` | `Vec3` | `Vec3.ZERO` | Source of the stream. The options carry the source, so the same options work with `sendParticles` |
| `to` | `Vec3` | `Vec3.ZERO` | Destination. The head plunges into it and the stream drains into it |
| `color` | `int` / RGB | required | Packed RGB color, usually the aspect color |
| `seed` | `int` | random | Seed for the launch direction, the radius variation and the pulse |
| `radius` | `float` | `0.15` | Body radius in blocks, before the per-stream variation of about ±15% |
| `extend` | `int` | `0` | Body ticks. When the stream is created, the body is `max(length, extend)` ticks long. Merges add `max(extend, 5)` ticks |
| `verticalBoost` | `float` | `0.0` | Initial upward speed in blocks per second. It bends the path up. Around `4.0` gives a clear bulge at the default speed. Clamped to `±20` |
| `waypoints` | `List<Vec3>` | empty | Points the stream passes through before `to`. Pathfinding around walls is not included, see [Pathfinding](#pathfinding) |
| `launchDirection` | `Optional<Vec3>` | empty | Direction to leave `from` in at cruise speed. Empty means a seeded launch |
| `length` | `int` | `20` | Body length in ticks of travel. At the default speed, 20 ticks is about 1.25 blocks. Between `1` and `400` |
| `speed` | `float` | `1.25` | Head speed in blocks per second. Clamped to `0.25` to `20`. The steering and the launch scale with it |

Use `EssentiaStreamParticleOptions.of(from, to, color)` and its overloads `of(from, to, color, radius, extend)`, `of(from, to, color, radius, extend, length)` or `of(from, to, color, radius, extend, length, speed)` for the common cases. Use the [builder](#ports) for ports and waypoints, or construct the record directly.

### Speed and timings

- At the default speed of 1.25 blocks per second, one tick is 0.0625 blocks, so a 1.25-block body is 20 ticks.
- The drain takes `length` ticks after arrival, so a default stream empties about one second after it arrives (plus any merges).
- Lifetime safety net: `(route length / cruise) × 4 + body ticks + 100` ticks, with a minimum of 40. The 4× slack covers the steering turns. Merges extend it by the ticks they add. A stream that has drained is removed earlier.
- The straight-line flight time is `options.straightFlightTicks()`. Real flights take longer, because the steering turns, so use it as a lower bound for timing rather than an exact arrival time. The simulated flight times were not measured for this version.

### Ports

A `StreamPort` is a point where a stream leaves or enters a block through one of its faces. It has an `anchor` (where the stream starts or ends) and a `clearance` point further out along the face's normal.

- `StreamPort.nozzle(BlockPos pos, Direction facing)`: the anchor is 0.05 blocks outside the face, and the clearance is 0.65 blocks further out. Use it for jars, pipes and anything that spouts.
- `StreamPort.pane(BlockPos pos, Direction facing)`: the anchor is 0.14 blocks in front of the face (the block centre plus `facing × 0.64`), and the clearance is 0.78 blocks in front of the face (`facing × 1.28`). Use it for mirrors and flat faces.
- `StreamPort.of(Vec3 anchor, Vec3 clearance)`: any other point.

An **exit** starts the stream at its anchor and launches it toward its clearance point, which becomes the first waypoint. An **entry** makes the clearance point the last waypoint and the anchor the destination, so the stream dives straight in through the face.

The builder resolves ports into the plain fields, so they need no codec of their own:

```java
EssentiaStreamParticleOptions options = EssentiaStreamParticleOptions.builder(0x2F7BFF)
        .exit(StreamPort.nozzle(sourcePos, Direction.EAST))
        .via(new Vec3(0.5D, 66.5D, 9.5D))
        .entry(StreamPort.pane(mirrorPos, Direction.WEST))
        .radius(0.15F)
        .length(20)
        .build();
```

Builder methods: `from`, `to`, `exit`, `entry`, `via` (adds a waypoint between the exit and entry clearance points), `launchDirection`, `seed`, `radius`, `extend`, `verticalBoost`, `length`, `speed` and `build`. A route shorter than 1.75 blocks, from the exit anchor to the entry anchor, goes straight to the destination, without the clearance points.

`EssentiaStreams.spawn(level, exitPort, entryPort, color)` is shorthand for an exit and an entry with the defaults.

### Pathfinding

Pathfinding is **not** implemented. The stream only follows the waypoints it is given. Waypoints can come from a pathfinder, for example A* over the block grid, with line-of-sight smoothing and about 0.4 blocks of clearance from walls. Pass the result as `waypoints` or with `via`.

## Droplets

The stream spawns `magicparticleslib:bubble` particles, which are small translucent bubbles tinted with the stream's colour:

- **While flying:** with a 50% chance each tick, a droplet comes off one of the three oldest samples in the body, or, half the time, off the second newest. Its drift is a small gaussian velocity.
- **While draining:** one droplet each tick appears at the destination with a small gaussian offset, and then floats up. It is not pulled into the destination.
- A bubble lasts 20 to 29 ticks. Its quad is `scale × 0.1` blocks across, with `scale` between 0.4 and 0.6. It shrinks to half over its life, and in its last two ticks it pops: it flares up and fades out.

Droplets can be turned off globally with `EssentiaStreams.setDropletsEnabled(false)`.

## Behavior

The stream:

- leaves the source with a rounded head and flies at `speed` (default 1.25 blocks per second), curving slightly upward
- lays a body of the last `length` ticks behind its head, so the body is a string of beads
- thins to nothing as it reaches the destination, then drains into it
- is fullbright, translucent and visible from inside
- is removed once it has drained, or when its lifetime ends

## Use in Code

See https://docs.neoforged.net/docs/resources/client/particles/#spawning-particles.

On the client:

```java
Vec3 from = new Vec3(10.5, 65.0, 10.5);
Vec3 to = new Vec3(14.5, 66.0, 10.5);
EssentiaStreams.spawn(level, from, to, 0x7A4CFF);
```

With a custom radius, extension, length and speed:

```java
EssentiaStreams.spawn(level, from, to, 0x7A4CFF, 0.15F, 20, 20, 1.25F);
```

With all options:

```java
EssentiaStreamParticleOptions options = new EssentiaStreamParticleOptions(
        from,
        to,
        0x2F7BFF,
        level.random.nextInt(),
        0.15F,
        0,
        4.0F,
        List.of(new Vec3(12.5, 68.0, 10.5)),
        Optional.empty(),
        20,
        1.25F
);
EssentiaStreams.spawn(level, options);
```

On a server, send the same options to nearby players with `ServerLevel.sendParticles`:

```java
serverLevel.sendParticles(options, from.x, from.y, from.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
```

## Migrating from the earlier stream

- `maxLength` is replaced by `length`, which is in ticks of travel, not blocks.
- `extend` no longer holds a static tube. It sets the body length at creation (`max(length, extend)`), and merges add to the body.
- The default `radius` is `0.15`, and the default `speed` is `1.25` blocks per second (it was `0.12` and `6.0`).
- `verticalBoost` is in blocks per second and is an initial velocity. The earlier decaying hop is gone.

## Command usage

Example helper commands for local testing:

```mcfunction
/mpl spawn essentia_stream
/mpl spawn essentia_stream 20
/mpl spawn essentia_stream 20 0.15
/mpl spawn essentia_stream 20 0.15 20
/mpl spawn essentia_stream 20 0.15 20 1.25
```

Arguments for `essentia_stream`, in order. Each optional argument needs the ones before it:

- `extend`: extra body ticks at creation (default `0`)
- `radius`: body radius in blocks, between `0.01` and `1.0` (default `0.15`)
- `length`: body length in ticks, between `1` and `400` (default `20`)
- `speed`: head speed in blocks per second, between `0.25` and `20` (default `1.25`). The chat message after a spawn shows the values used.

The stream flies from roughly the player's eye to the block or entity being looked at (up to 16 blocks away). Each use picks a random color.
