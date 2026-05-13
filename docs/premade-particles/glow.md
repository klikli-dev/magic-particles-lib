<!--
SPDX-FileCopyrightText: 2026 klikli-dev

SPDX-License-Identifier: MIT
-->

# Glow Particle

The glow particle is a premade translucent particle with full-bright rendering, configurable ARGB color, optional no-depth rendering, and optional shrinking over its lifetime.

## Registry id

`magicparticleslib:glow`

Registered in [`src/main/java/com/klikli_dev/magicparticleslib/registry/ParticleTypes.java`](../../src/main/java/com/klikli_dev/magicparticleslib/registry/ParticleTypes.java).

## Implementation overview

Relevant classes:

- [`GlowParticleOptions`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/glow/GlowParticleOptions.java)
- [`GlowParticleType`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/glow/GlowParticleType.java)
- [`GlowParticleProvider`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/glow/GlowParticleProvider.java)
- [`GlowParticle`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/glow/GlowParticle.java)

Client registration is done in [`MagicParticlesLibClient`](../../src/main/java/com/klikli_dev/magicparticleslib/MagicParticlesLibClient.java).

Particle description datagen is done in [`MagicParticlesLibParticleDescriptionProvider`](../../src/main/java/com/klikli_dev/magicparticleslib/datagen/MagicParticlesLibParticleDescriptionProvider.java).

## Options

`GlowParticleOptions` uses a packed ARGB color similar to vanilla `ColorParticleOption`, plus a few particle-specific fields.

| Field | Type | Default | Description |
|---|---|---:|---|
| `color` | `int` / ARGB color codec | `#FFFFFFFF` | Packed particle color including alpha |
| `disableDepthTest` | `boolean` | `false` | Uses a no-depth translucent render layer |
| `shrinkWithAge` | `boolean` | `true` | Shrinks size linearly over lifetime |
| `size` | `float` | `0.25f` | Initial quad size |
| `age` | `int` | `36` | Lifetime in ticks, clamped to at least `1` internally |

Helper methods are exposed on `GlowParticleOptions` for constructing options in code.

## Behavior

The particle:

- is full-bright
- is translucent
- fades alpha linearly over its lifetime
- can optionally shrink linearly over its lifetime
- rotates over time
- uses a sprite-set based particle description
- can optionally disable depth writes/testing behavior for overlapping translucent visuals

## Command usage

Example with shrinking enabled:

```mcfunction
/particle magicparticleslib:glow{color:-65281,disableDepthTest:0b,shrinkWithAge:1b,size:0.25f,age:36} ~ ~1 ~ 0 0 0 0 1
```

Example with constant size:

```mcfunction
/particle magicparticleslib:glow{color:-2130706433,disableDepthTest:0b,shrinkWithAge:0b,size:0.4f,age:60} ~ ~1 ~ 0 0 0 0 1
```

Use integer ARGB values in commands.

`color` uses ARGB format internally:

- `#AARRGGBB`
- `#FFFF00FF` = opaque magenta
- `#80FFFFFF` = 50% alpha white

Equivalent command-safe integer examples:

- `-65281` = `#FFFF00FF`
- `-2130706433` = `#80FFFFFF`

## Datagen

The particle description is generated, not hand-authored.

Generated output:

- [`src/generated/resources/assets/magicparticleslib/particles/glow.json`](../../src/generated/resources/assets/magicparticleslib/particles/glow.json)

Texture:

- [`src/main/resources/assets/magicparticleslib/textures/particle/particle_glow.png`](../../src/main/resources/assets/magicparticleslib/textures/particle/particle_glow.png)

The datagen provider lives in the `datagen` package, not the older `data.client` location.

## Integration notes

- Use `ParticleTypes.GLOW.get()` when constructing or sending the particle from code.
- Register the sprite-set provider on the client using `RegisterParticleProvidersEvent#registerSpriteSet`.
- Keep `runData` up to date if the particle description or texture naming changes.
