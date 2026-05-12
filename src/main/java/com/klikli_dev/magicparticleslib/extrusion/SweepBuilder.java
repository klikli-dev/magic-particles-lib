// SPDX-FileCopyrightText: 1990, 1991, 2003 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 1994, 1995 Linas Vepstas <linas@linas.org>
// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: LicenseRef-IBM-SESCL

package com.klikli_dev.magicparticleslib.extrusion;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SweepBuilder {
    private final Contour2D contour;
    private final List<PathPoint> path;
    private final ExtrusionOptions options;
    private final ArrayList<ExtrusionQuad> quads = new ArrayList<>();
    private final ArrayList<ExtrusionTriangle> triangles = new ArrayList<>();

    SweepBuilder(Contour2D contour, List<PathPoint> path, ExtrusionOptions options) {
        this.contour = contour;
        this.path = sanitizePath(path, options.degeneracyTolerance());
        this.options = options;
    }

    ExtrusionMesh build() {
        if (this.path.size() < 2) {
            return ExtrusionMesh.EMPTY;
        }

        List<Ring> rings = createRings();
        if (rings.size() < 2) {
            return ExtrusionMesh.EMPTY;
        }

        for (int index = 0; index < rings.size() - 1; index++) {
            connectRings(rings.get(index), rings.get(index + 1));
            if (index < rings.size() - 2) {
                emitJoin(rings.get(index), rings.get(index + 1), rings.get(index + 2));
            }
        }

        if (this.options.capEnds()) {
            emitCap(rings.getFirst(), true);
            emitCap(rings.getLast(), false);
        }

        return new ExtrusionMesh(List.copyOf(this.quads), List.copyOf(this.triangles));
    }

    private List<Ring> createRings() {
        ArrayList<Ring> rings = new ArrayList<>(this.path.size());
        double[] pathDistances = computePathDistances();
        Vec3 up = ExtrusionMath.perpendicularComponent(this.options.upVector(), firstDirection()).lengthSqr() < 1.0E-10
                ? fallbackUp(firstDirection())
                : ExtrusionMath.perpendicularComponent(this.options.upVector(), firstDirection()).normalize();

        for (int index = 0; index < this.path.size(); index++) {
            Vec3 position = this.path.get(index).position();
            Vec3 tangent = tangentAt(index);
            Vec3 bisector = bisectorAt(index);
            if (index > 0) {
                up = ExtrusionMath.reflectAcrossPlane(up, bisector);
            }

            ExtrusionMath.Frame frame = ExtrusionMath.frameFromTangent(position, tangent, up, this.options.degeneracyTolerance());
            ArrayList<RingVertex> vertices = new ArrayList<>(this.contour.points().size());
            for (int contourIndex = 0; contourIndex < this.contour.points().size(); contourIndex++) {
                ContourPoint contourPoint = this.contour.points().get(contourIndex);
                AffineTransform2D transform = this.path.get(index).effectiveTransform();
                Vec2d transformedPoint = transform.transformPoint(contourPoint.position());
                Vec2d transformedNormal = contourPoint.normal() == null ? Vec2d.ZERO : transform.transformNormal(contourPoint.normal());
                Vec3 worldPosition = frame.toWorld(transformedPoint);
                Vec3 worldNormal = computeWorldNormal(frame, transformedNormal, transformedPoint, contourIndex, index, rings);
                TextureCoordinates uv = textureCoordinates(transformedPoint, transformedNormal, contourIndex, pathDistances[index]);
                vertices.add(new RingVertex(worldPosition, worldNormal, uv, this.path.get(index).color()));
            }
            rings.add(new Ring(frame, List.copyOf(vertices), pathDistances[index], bisector));
        }

        return List.copyOf(rings);
    }

    private Vec3 computeWorldNormal(ExtrusionMath.Frame frame, Vec2d transformedNormal, Vec2d transformedPoint, int contourIndex, int pathIndex, List<Ring> rings) {
        Vec3 direction;
        if (transformedNormal.lengthSquared() > 1.0E-12) {
            direction = frame.directionToWorld(transformedNormal);
        } else {
            direction = frame.directionToWorld(transformedPoint.normalize());
        }

        direction = ExtrusionMath.normalizeOrFallback(direction, frame.normal(), this.options.degeneracyTolerance());
        if (this.options.normalStyle() == NormalStyle.PATH_EDGE) {
            Vec3 plane = bisectorAt(pathIndex);
            Vec3 perpendicular = ExtrusionMath.perpendicularComponent(direction, plane);
            direction = ExtrusionMath.normalizeOrFallback(perpendicular, direction, this.options.degeneracyTolerance());
        }

        if (this.options.normalStyle() == NormalStyle.FACET && this.contour.closed()) {
            int nextIndex = (contourIndex + 1) % this.contour.points().size();
            Vec3 current = frame.toWorld(this.path.get(pathIndex).effectiveTransform().transformPoint(this.contour.points().get(contourIndex).position()));
            Vec3 next = frame.toWorld(this.path.get(pathIndex).effectiveTransform().transformPoint(this.contour.points().get(nextIndex).position()));
            Vec3 edge = next.subtract(current);
            Vec3 facet = frame.tangent().cross(edge).normalize();
            if (facet.lengthSqr() > 1.0E-10) {
                direction = facet;
            }
        }

        return direction;
    }

    private TextureCoordinates textureCoordinates(Vec2d transformedPoint, Vec2d transformedNormal, int contourIndex, double pathDistance) {
        TextureCoordinateMode mode = this.options.textureMode();
        if (mode == TextureCoordinateMode.NONE) {
            return new TextureCoordinates(0.0F, 0.0F);
        }

        double scaledPathDistance = this.options.textureLengthOffset() + pathDistance * this.options.textureLengthScale();

        Vec2d sample = switch (mode) {
            case NORMAL_FLAT, NORMAL_CYLINDER, NORMAL_SPHERE, NORMAL_MODEL_FLAT, NORMAL_MODEL_CYLINDER, NORMAL_MODEL_SPHERE -> transformedNormal.lengthSquared() > 1.0E-12 ? transformedNormal.normalize() : transformedPoint.normalize();
            case VERTEX_MODEL_FLAT, VERTEX_MODEL_CYLINDER, VERTEX_MODEL_SPHERE -> this.contour.points().get(contourIndex).position();
            default -> transformedPoint;
        };

        double u;
        double v;
        switch (mode) {
            case VERTEX_CYLINDER, NORMAL_CYLINDER, VERTEX_MODEL_CYLINDER, NORMAL_MODEL_CYLINDER -> {
                u = 0.5 * Math.atan2(sample.x(), sample.y()) / Math.PI + 0.5;
                v = scaledPathDistance;
            }
            case VERTEX_SPHERE, NORMAL_SPHERE, VERTEX_MODEL_SPHERE, NORMAL_MODEL_SPHERE -> {
                double z = Math.sqrt(Math.max(0.0, 1.0 - Math.min(1.0, sample.lengthSquared())));
                u = 0.5 * Math.atan2(sample.x(), sample.y()) / Math.PI + 0.5;
                v = 1.0 - Math.acos(z) / Math.PI;
            }
            default -> {
                u = sample.x();
                v = scaledPathDistance;
            }
        }
        return new TextureCoordinates((float) u, (float) v);
    }

    private void connectRings(Ring front, Ring back) {
        int contourSize = this.contour.points().size();
        int limit = this.contour.closed() ? contourSize : contourSize - 1;
        for (int index = 0; index < limit; index++) {
            int next = (index + 1) % contourSize;
            RingVertex frontA = front.vertices().get(index);
            RingVertex frontB = front.vertices().get(next);
            RingVertex backA = back.vertices().get(index);
            RingVertex backB = back.vertices().get(next);

            if (frontA.position().distanceToSqr(frontB.position()) < 1.0E-12 || backA.position().distanceToSqr(backB.position()) < 1.0E-12) {
                continue;
            }

            if (this.options.normalStyle() == NormalStyle.FACET) {
                Vec3 faceNormal = backA.position().subtract(frontA.position()).cross(frontB.position().subtract(frontA.position())).normalize();
                if (faceNormal.lengthSqr() < 1.0E-10) {
                    faceNormal = frontA.normal();
                }
                addQuad(frontA.withNormal(faceNormal), frontB.withNormal(faceNormal), backB.withNormal(faceNormal), backA.withNormal(faceNormal));
            } else {
                addQuad(frontA, frontB, backB, backA);
            }
        }
    }

    private void emitJoin(Ring previous, Ring current, Ring next) {
        if (this.options.joinStyle() == JoinStyle.ANGLE || this.options.joinStyle() == JoinStyle.RAW) {
            return;
        }

        int contourSize = this.contour.points().size();
        int limit = this.contour.closed() ? contourSize : contourSize - 1;
        for (int index = 0; index < limit; index++) {
            int nextIndex = (index + 1) % contourSize;

            RingVertex currentA = current.vertices().get(index);
            RingVertex currentB = current.vertices().get(nextIndex);
            RingVertex previousA = previous.vertices().get(index);
            RingVertex previousB = previous.vertices().get(nextIndex);
            RingVertex nextA = next.vertices().get(index);
            RingVertex nextB = next.vertices().get(nextIndex);

            if (this.options.joinStyle() == JoinStyle.CUT) {
                addTriangle(currentA, currentB, previousA.withUv(currentA.uv()));
                addTriangle(currentB, previousB.withUv(currentB.uv()), previousA.withUv(currentA.uv()));
                addTriangle(nextA.withUv(currentA.uv()), nextB.withUv(currentB.uv()), currentA);
                addTriangle(nextB.withUv(currentB.uv()), currentB, currentA);
            } else if (this.options.joinStyle() == JoinStyle.ROUND) {
                emitRoundJoin(previousA, currentA, nextA, previousB, currentB, nextB);
            }
        }
    }

    private void emitRoundJoin(RingVertex previousA, RingVertex currentA, RingVertex nextA, RingVertex previousB, RingVertex currentB, RingVertex nextB) {
        for (int step = 0; step < this.options.roundJoinSegments(); step++) {
            double startT = (double) step / this.options.roundJoinSegments();
            double endT = (double) (step + 1) / this.options.roundJoinSegments();
            RingVertex fromA = interpolateJoin(previousA, currentA, nextA, startT);
            RingVertex toA = interpolateJoin(previousA, currentA, nextA, endT);
            RingVertex fromB = interpolateJoin(previousB, currentB, nextB, startT);
            RingVertex toB = interpolateJoin(previousB, currentB, nextB, endT);
            addQuad(fromA, fromB, toB, toA);
        }
    }

    private RingVertex interpolateJoin(RingVertex previous, RingVertex current, RingVertex next, double t) {
        if (t <= 0.5) {
            double local = t * 2.0;
            Vec3 position = ExtrusionMath.interpolate(previous.position(), current.position(), local);
            Vec3 normal = ExtrusionMath.interpolate(previous.normal(), current.normal(), local).normalize();
            return new RingVertex(position, normal, current.uv(), current.color());
        }

        double local = (t - 0.5) * 2.0;
        Vec3 position = ExtrusionMath.interpolate(current.position(), next.position(), local);
        Vec3 normal = ExtrusionMath.interpolate(current.normal(), next.normal(), local).normalize();
        return new RingVertex(position, normal, current.uv(), current.color());
    }

    private void emitCap(Ring ring, boolean front) {
        List<RingVertex> vertices = ring.vertices();
        if (vertices.size() < 3) {
            return;
        }

        Vec3 normal = front ? ring.frame().tangent() : ring.frame().tangent().reverse();

        List<Integer> order = triangulateCap(vertices, ring.frame(), front);
        RingVertex anchor = vertices.get(order.getFirst()).withNormal(normal);
        for (int index = 1; index < order.size() - 1; index++) {
            RingVertex b = vertices.get(order.get(index)).withNormal(normal);
            RingVertex c = vertices.get(order.get(index + 1)).withNormal(normal);
            if (front) {
                addTriangle(anchor, b, c);
            } else {
                addTriangle(anchor, c, b);
            }
        }
    }

    private List<Integer> triangulateCap(List<RingVertex> vertices, ExtrusionMath.Frame frame, boolean front) {
        ArrayList<Integer> indices = new ArrayList<>(vertices.size());
        for (int index = 0; index < vertices.size(); index++) {
            indices.add(index);
        }

        Vec3 center = Vec3.ZERO;
        for (RingVertex vertex : vertices) {
            center = center.add(vertex.position());
        }
        center = center.scale(1.0 / vertices.size());
        Vec3 centerFinal = center;

        indices.sort(Comparator.comparingDouble(index -> {
            Vec3 offset = vertices.get(index).position().subtract(centerFinal);
            double x = offset.dot(frame.normal());
            double y = offset.dot(frame.binormal());
            return Math.atan2(y, x);
        }));

        if (!front) {
            ArrayList<Integer> reversed = new ArrayList<>(indices);
            java.util.Collections.reverse(reversed);
            return List.copyOf(reversed);
        }

        return List.copyOf(indices);
    }

    private Vec3 firstDirection() {
        for (int index = 0; index < this.path.size() - 1; index++) {
            Vec3 direction = this.path.get(index + 1).position().subtract(this.path.get(index).position());
            if (direction.lengthSqr() > this.options.degeneracyTolerance() * this.options.degeneracyTolerance()) {
                return direction.normalize();
            }
        }
        return ExtrusionMath.Z_AXIS;
    }

    private Vec3 tangentAt(int index) {
        Vec3 tangent;
        if (index == 0) {
            tangent = this.path.get(1).position().subtract(this.path.getFirst().position());
        } else if (index == this.path.size() - 1) {
            tangent = this.path.getLast().position().subtract(this.path.get(index - 1).position());
        } else {
            tangent = this.path.get(index + 1).position().subtract(this.path.get(index - 1).position());
        }
        return ExtrusionMath.normalizeOrFallback(tangent, firstDirection(), this.options.degeneracyTolerance());
    }

    private Vec3 bisectorAt(int index) {
        if (index == 0 || index == this.path.size() - 1) {
            return tangentAt(index);
        }
        return ExtrusionMath.bisectingPlane(this.path.get(index - 1).position(), this.path.get(index).position(), this.path.get(index + 1).position(), this.options.degeneracyTolerance());
    }

    private double[] computePathDistances() {
        double[] distances = new double[this.path.size()];
        double accumulated = 0.0;
        distances[0] = 0.0;
        for (int index = 1; index < this.path.size(); index++) {
            accumulated += this.path.get(index).position().distanceTo(this.path.get(index - 1).position());
            distances[index] = accumulated;
        }
        return distances;
    }

    private Vec3 fallbackUp(Vec3 tangent) {
        return Math.abs(tangent.dot(ExtrusionMath.Y_AXIS)) > 0.98 ? ExtrusionMath.X_AXIS : ExtrusionMath.Y_AXIS;
    }

    private void addQuad(RingVertex first, RingVertex second, RingVertex third, RingVertex fourth) {
        this.quads.add(new ExtrusionQuad(first.toVertex(), second.toVertex(), third.toVertex(), fourth.toVertex()));
    }

    private void addTriangle(RingVertex first, RingVertex second, RingVertex third) {
        this.triangles.add(new ExtrusionTriangle(first.toVertex(), second.toVertex(), third.toVertex()));
    }

    private static List<PathPoint> sanitizePath(List<PathPoint> path, double tolerance) {
        ArrayList<PathPoint> result = new ArrayList<>(path.size());
        for (PathPoint point : path) {
            if (result.isEmpty() || !ExtrusionMath.isDegenerate(result.getLast().position(), point.position(), tolerance)) {
                result.add(point);
            }
        }
        return List.copyOf(result);
    }

    private record Ring(ExtrusionMath.Frame frame, List<RingVertex> vertices, double pathDistance, Vec3 bisector) {
    }

    private record RingVertex(Vec3 position, Vec3 normal, TextureCoordinates uv, int color) {
        RingVertex withNormal(Vec3 value) {
            return new RingVertex(this.position, value, this.uv, this.color);
        }

        RingVertex withUv(TextureCoordinates value) {
            return new RingVertex(this.position, this.normal, value, this.color);
        }

        ExtrusionVertex toVertex() {
            return new ExtrusionVertex(
                    (float) this.position.x,
                    (float) this.position.y,
                    (float) this.position.z,
                    (float) this.normal.x,
                    (float) this.normal.y,
                    (float) this.normal.z,
                    this.uv.u(),
                    this.uv.v(),
                    this.color
            );
        }
    }

    private record TextureCoordinates(float u, float v) {
    }
}
