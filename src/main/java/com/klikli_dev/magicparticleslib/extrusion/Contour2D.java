// SPDX-FileCopyrightText: 1991, 1993, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

import java.util.ArrayList;
import java.util.List;

public record Contour2D(List<ContourPoint> points, boolean closed) {
    public Contour2D {
        points = List.copyOf(points);
        if (points.size() < 2) {
            throw new IllegalArgumentException("Contours require at least two points.");
        }
    }

    public static Contour2D of(List<ContourPoint> points, boolean closed) {
        return new Contour2D(withGeneratedNormals(points, closed), closed);
    }

    public static Contour2D circle(double radius, int segments) {
        int safeSegments = Math.max(3, segments);
        List<ContourPoint> points = new ArrayList<>(safeSegments);
        for (int index = 0; index < safeSegments; index++) {
            double angle = Math.PI * 2.0 * index / safeSegments;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            points.add(ContourPoint.of(radius * cos, radius * sin, cos, sin));
        }
        return new Contour2D(List.copyOf(points), true);
    }

    private static List<ContourPoint> withGeneratedNormals(List<ContourPoint> source, boolean closed) {
        List<ContourPoint> points = List.copyOf(source);
        boolean missingNormal = points.stream().anyMatch(point -> point.normal() == null || point.normal().lengthSquared() < 1.0E-12);
        if (!missingNormal) {
            return points;
        }

        ArrayList<ContourPoint> result = new ArrayList<>(points.size());
        for (int index = 0; index < points.size(); index++) {
            Vec2d current = points.get(index).position();
            Vec2d previous = points.get(index == 0 ? (closed ? points.size() - 1 : 0) : index - 1).position();
            Vec2d next = points.get(index == points.size() - 1 ? (closed ? 0 : points.size() - 1) : index + 1).position();

            Vec2d incoming = current.subtract(previous);
            Vec2d outgoing = next.subtract(current);
            Vec2d normal;
            if (!closed && index == 0) {
                normal = outgoing.leftNormal().normalize();
            } else if (!closed && index == points.size() - 1) {
                normal = incoming.leftNormal().normalize();
            } else {
                normal = incoming.leftNormal().normalize().add(outgoing.leftNormal().normalize()).normalize();
                if (normal.lengthSquared() < 1.0E-12) {
                    normal = outgoing.leftNormal().normalize();
                }
            }

            result.add(points.get(index).withNormal(normal));
        }

        return List.copyOf(result);
    }
}
