# Follow Projectile

The follow projectile is a premade visual-only entity effect that steers from a start position toward a target position while leaving a glow-particle trail behind it.

Its primary use is client-side cosmetic playback.

## Registry id

`magicparticleslib:follow_projectile`

## Implementation overview

Relevant classes:

- [`FollowProjectile`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/projectile/FollowProjectile.java)
- [`FollowProjectileRenderer`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/projectile/FollowProjectileRenderer.java)
- [`VisualEntitySpawner`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/projectile/VisualEntitySpawner.java)
- [`EntityTypes`](../../src/main/java/com/klikli_dev/magicparticleslib/registry/EntityTypes.java)

The projectile trail uses [`GlowParticleOptions`](../../src/main/java/com/klikli_dev/magicparticleslib/premade/glow/GlowParticleOptions.java).

## Behavior

The projectile:

- stores `from` and `to` positions in synced entity data
- gradually steers toward the target instead of moving in a straight line
- interpolates trail color from a start color to an end color
- emits glow particles on the client while moving
- can optionally emit a small glow burst when it arrives
- is intended for visual effects, not gameplay collision or damage

## Use in Code

Create a projectile with start and end positions, optional color gradient, and an initial velocity:

```java
Vec3 from = ...;
Vec3 to = ...;

FollowProjectile projectile = new FollowProjectile(level, from, to, 0xFFFF19B4, 0xFF00FFFF, 0.1F)
        .arrivalDistance(0.3F)
        .spawnImpactParticles(true);

Vec3 initialVelocity = to.subtract(from).normalize().scale(0.3F);
projectile.setDeltaMovement(initialVelocity);
```

### Client-only usage for visuals

If the effect is purely cosmetic, use `VisualEntitySpawner` to spawn it only on the local client:

```java
VisualEntitySpawner.spawn(level, projectile);
```

This is useful for transient visuals that should not create server-side entities.

`VisualEntitySpawner.spawn(level, projectile, true)` additionally skips spawning in chunks that are not currently ticking on the client.

### Server-spawned usage

You can also add the entity to the level normally, but this is mainly useful when you explicitly want entity tracking.
For purely visual playback, prefer `VisualEntitySpawner`.

## Notes

- `VisualEntitySpawner` is the intended default for visual purposes.
- The `DistHelper` indirection is kept so common-side callers can safely invoke the helper without dedicated-server crashes.
- The projectile renderer is intentionally empty because the visible output comes from the glow-particle trail rather than rendered model geometry.
