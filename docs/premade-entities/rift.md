# Rift

The rift showcases how to use extrusion to get an effect that looks like the rifts used in Thaumcraft. To use it in your mod you most likely should create a copy of the RiftEntity class and modify it to your needs, the premade entity does not have any in-game effects, it only acts as an "anchor" for the rift renderer.

## Registry id

`magicparticleslib:rift`

## Command usage

You can spawn a rift with vanilla `/summon`:

```mcfunction
/summon magicparticleslib:rift ~ ~ ~ {Seed:123,Size:18,VisualIntensity:0.8f}
```

## NBT fields

- `Seed`: controls the generated shape seed
- `Size`: controls the rift size
- `VisualIntensity`: controls visual brightness/intensity

## Example variants

```mcfunction
/summon magicparticleslib:rift ~ ~ ~ {Seed:42,Size:12,VisualIntensity:0.5f}
```
