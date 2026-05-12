// SPDX-FileCopyrightText: 1991, 1993, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

record Vec2d(double x, double y) {
    static final Vec2d ZERO = new Vec2d(0.0, 0.0);

    Vec2d add(Vec2d other) {
        return new Vec2d(this.x + other.x, this.y + other.y);
    }

    Vec2d subtract(Vec2d other) {
        return new Vec2d(this.x - other.x, this.y - other.y);
    }

    Vec2d scale(double factor) {
        return new Vec2d(this.x * factor, this.y * factor);
    }

    double dot(Vec2d other) {
        return this.x * other.x + this.y * other.y;
    }

    double lengthSquared() {
        return this.dot(this);
    }

    Vec2d normalize() {
        double lengthSquared = lengthSquared();
        if (lengthSquared < 1.0E-12) {
            return ZERO;
        }
        double scale = 1.0 / Math.sqrt(lengthSquared);
        return scale(scale);
    }

    Vec2d leftNormal() {
        return new Vec2d(-this.y, this.x);
    }
}
