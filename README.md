<!--
SPDX-FileCopyrightText: 2026 klikli-dev

SPDX-License-Identifier: MIT
-->

# Magic Particles Lib

A Minecraft library mod for reusable magic particles based on [glextrusion](https://github.com/linas/glextrusion).

## Documentation

See [docs/README.md](./docs/README.md).

## Maven

See https://cloudsmith.io/~klikli-dev/repos/mods/groups/ for available versions.

```gradle
repositories {

  ...

  maven {
    url "https://dl.cloudsmith.io/public/klikli-dev/mods/maven/"
    content {
        includeGroup "com.klikli_dev"
    }
  }

  ...

}
```

```gradle
dependencies {

    ...

    implementation "com.klikli_dev:magicparticleslib-${minecraft_version}-neoforge:${magic_particles_lib_version}"

    ...

}
```

Alternatively if MPL should be bundled in your mod's jar file:

```gradle
dependencies {
 
    ...
    
        jarJar(implementation(group: "com.klikli_dev", name: "magicparticleslib-${minecraft_version}-neoforge")) {
        version {
            prefer magic_particles_lib_version
        }
    }
    
    ...
    
}
```

## Thanks

[![Hosted By: Cloudsmith](https://img.shields.io/badge/OSS%20hosting%20by-cloudsmith-blue?logo=cloudsmith&style=for-the-badge)](https://cloudsmith.com)

Package repository hosting is graciously provided by [Cloudsmith](https://cloudsmith.com).
Cloudsmith is the only fully hosted, cloud-native, universal package management solution, that
enables your organization to create, store and share packages in any format, to any place, with total
confidence.

## Licensing

Copyright 2026 klikli-dev

Code is licensed under the MIT license, view [LICENSES/MIT](./LICENSES/MIT.txt).
Assets are licensed under the CC-BY-4.0 license, view [LICENSES/CC-BY-4.0](./LICENSES/CC-BY-4.0.txt).

There are third party code and assets in this project which may be under different licenses and copyrights. We follow the [REUSE Standard for Software Licensing](https://reuse.software/), so you can look up the license terms for each file, or use the [REUSE tool](https://github.com/fsfe/reuse-tool) to generate an SPDX report.
