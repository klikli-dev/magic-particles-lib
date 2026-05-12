// SPDX-FileCopyrightText: 1990, 1991, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

public enum NormalStyle {
    FACET,
    EDGE,
    PATH_EDGE;

    static NormalStyle fromLegacyBits(int styleBits) {
        if ((styleBits & 0x400) != 0) {
            return PATH_EDGE;
        }
        if ((styleBits & 0x100) != 0) {
            return FACET;
        }
        return EDGE;
    }
}
