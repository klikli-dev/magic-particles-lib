// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.nitor;

import com.mojang.serialization.Codec;

public enum NitorCoreType {
    DARK,
    LIGHT;

    public static final Codec<NitorCoreType> CODEC = Codec.STRING.xmap(
            NitorCoreType::fromName,
            NitorCoreType::getSerializedName
    );

    public String getSerializedName() {
        return this == LIGHT ? "light" : "dark";
    }

    public static NitorCoreType fromName(String name) {
        return "light".equals(name) ? LIGHT : DARK;
    }
}