// SPDX-FileCopyrightText: 1994, 1995 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

public enum TextureCoordinateMode {
    NONE(0),
    VERTEX_FLAT(1),
    NORMAL_FLAT(2),
    VERTEX_CYLINDER(3),
    NORMAL_CYLINDER(4),
    VERTEX_SPHERE(5),
    NORMAL_SPHERE(6),
    VERTEX_MODEL_FLAT(7),
    NORMAL_MODEL_FLAT(8),
    VERTEX_MODEL_CYLINDER(9),
    NORMAL_MODEL_CYLINDER(10),
    VERTEX_MODEL_SPHERE(11),
    NORMAL_MODEL_SPHERE(12);

    public static final int LEGACY_TEXTURE_ENABLE = 0x10000;

    private final int legacyCode;

    TextureCoordinateMode(int legacyCode) {
        this.legacyCode = legacyCode;
    }

    public int legacyCode() {
        return this.legacyCode;
    }

    public static TextureCoordinateMode fromLegacyBits(int textureBits) {
        if ((textureBits & LEGACY_TEXTURE_ENABLE) == 0) {
            return NONE;
        }

        int style = textureBits & 0xFF;
        for (TextureCoordinateMode mode : values()) {
            if (mode.legacyCode == style) {
                return mode;
            }
        }

        return NONE;
    }
}
