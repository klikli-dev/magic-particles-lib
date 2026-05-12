// SPDX-FileCopyrightText: 1991, 1993, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

public record ContourPoint(Vec2d position, Vec2d normal) {
    public ContourPoint {
        normal = normal == null ? Vec2d.ZERO : normal.normalize();
    }

    public static ContourPoint of(double x, double y) {
        return new ContourPoint(new Vec2d(x, y), null);
    }

    public static ContourPoint of(double x, double y, double nx, double ny) {
        return new ContourPoint(new Vec2d(x, y), new Vec2d(nx, ny));
    }

    ContourPoint withNormal(Vec2d value) {
        return new ContourPoint(this.position, value);
    }
}
