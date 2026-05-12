// SPDX-FileCopyrightText: 1990, 1991, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 1994, 1995 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

public final class Extrusion {
    private static final ExtrusionEngine DEFAULT_ENGINE = new ExtrusionEngine();

    private Extrusion() {
    }

    public static ExtrusionEngine engine() {
        return DEFAULT_ENGINE;
    }

    public static ExtrusionOptions.Builder options() {
        return ExtrusionOptions.builder();
    }
}
