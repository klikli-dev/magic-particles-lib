<!--
SPDX-FileCopyrightText: 2026 klikli-dev

SPDX-License-Identifier: MIT
-->

# Particle Datagen

MPL generates particle description JSONs through NeoForge particle description datagen.

## Current provider

- [`src/main/java/com/klikli_dev/magicparticleslib/datagen/MagicParticlesLibParticleDescriptionProvider.java`](../../src/main/java/com/klikli_dev/magicparticleslib/datagen/MagicParticlesLibParticleDescriptionProvider.java)

## Event hookup

The provider is registered from:

- [`src/main/java/com/klikli_dev/magicparticleslib/MagicParticlesLib.java`](../../src/main/java/com/klikli_dev/magicparticleslib/MagicParticlesLib.java)

## Note on package naming

The particle description provider lives in `com.klikli_dev.magicparticleslib.datagen`.

If older notes reference `data.client`, they are stale.
