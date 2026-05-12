// SPDX-FileCopyrightText: 1990, 1991, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

public enum JoinStyle {
    RAW,
    ANGLE,
    CUT,
    ROUND;

    static JoinStyle fromLegacyBits(int styleBits) {
        return switch (styleBits & 0xF) {
            case 0x1 -> RAW;
            case 0x3 -> CUT;
            case 0x4 -> ROUND;
            default -> ANGLE;
        };
    }
}
