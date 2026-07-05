package com.lorenzocalifano.shieldup.ui.redzones

import com.google.android.gms.maps.model.LatLng
import com.lorenzocalifano.shieldup.data.RedZoneDto
import kotlin.math.*

object RouteSafetyUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0

    fun countDangerousIntersections(
        routePoints: List<LatLng>,
        redZones: List<RedZoneDto>,
        redZoneRadiusMeters: Double
    ): Int {
        return redZones.count { zone ->
            val zoneCenter = LatLng(zone.latitude, zone.longitude)

            routePoints.zipWithNext().any { segment ->
                distancePointToSegmentMeters(
                    point = zoneCenter,
                    start = segment.first,
                    end = segment.second
                ) <= redZoneRadiusMeters
            }
        }
    }

    fun routeLengthMeters(points: List<LatLng>): Double {
        return points.zipWithNext().sumOf { distanceMeters(it.first, it.second) }
    }

    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLat = Math.toRadians(b.latitude - a.latitude)
        val deltaLng = Math.toRadians(b.longitude - a.longitude)

        val h = sin(deltaLat / 2).pow(2.0) +
                cos(lat1) * cos(lat2) * sin(deltaLng / 2).pow(2.0)

        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
    }

    private fun distancePointToSegmentMeters(point: LatLng, start: LatLng, end: LatLng): Double {
        val startX = toX(start)
        val startY = toY(start)
        val endX = toX(end)
        val endY = toY(end)
        val pointX = toX(point)
        val pointY = toY(point)

        val dx = endX - startX
        val dy = endY - startY

        if (dx == 0.0 && dy == 0.0) {
            return sqrt((pointX - startX).pow(2.0) + (pointY - startY).pow(2.0))
        }

        val t = (((pointX - startX) * dx) + ((pointY - startY) * dy)) / (dx * dx + dy * dy)
        val clamped = t.coerceIn(0.0, 1.0)

        val closestX = startX + clamped * dx
        val closestY = startY + clamped * dy

        return sqrt((pointX - closestX).pow(2.0) + (pointY - closestY).pow(2.0))
    }

    private fun toX(point: LatLng): Double {
        return Math.toRadians(point.longitude) * EARTH_RADIUS_METERS * cos(Math.toRadians(point.latitude))
    }

    private fun toY(point: LatLng): Double {
        return Math.toRadians(point.latitude) * EARTH_RADIUS_METERS
    }

    fun createAvoidanceWaypoint(
        origin: LatLng,
        destination: LatLng,
        zoneCenter: LatLng,
        offsetMeters: Double = 320.0
    ): LatLng {
        val dx = destination.longitude - origin.longitude
        val dy = destination.latitude - origin.latitude

        val length = kotlin.math.sqrt(dx * dx + dy * dy).takeIf { it > 0.0 } ?: 1.0

        val perpendicularX = -dy / length
        val perpendicularY = dx / length

        val metersPerLat = 111320.0
        val metersPerLng = 111320.0 * kotlin.math.cos(Math.toRadians(zoneCenter.latitude))

        return LatLng(
            zoneCenter.latitude + (perpendicularY * offsetMeters / metersPerLat),
            zoneCenter.longitude + (perpendicularX * offsetMeters / metersPerLng)
        )
    }

    fun getIntersectedZones(
        routePoints: List<LatLng>,
        redZones: List<RedZoneDto>,
        redZoneRadiusMeters: Double
    ): List<RedZoneDto> {
        return redZones.filter { zone ->
            val zoneCenter = LatLng(zone.latitude, zone.longitude)

            routePoints.zipWithNext().any { segment ->
                distancePointToSegmentMeters(
                    point = zoneCenter,
                    start = segment.first,
                    end = segment.second
                ) <= redZoneRadiusMeters
            }
        }
    }

    fun createAvoidanceWaypointsAroundZone(
        zoneCenter: LatLng,
        radiusMeters: Double
    ): List<LatLng> {
        val distance = radiusMeters * 2.6
        val angles = listOf(0, 45, 90, 135, 180, 225, 270, 315)

        return angles.map { angle ->
            val radians = Math.toRadians(angle.toDouble())

            val metersPerLat = 111320.0
            val metersPerLng = 111320.0 * kotlin.math.cos(Math.toRadians(zoneCenter.latitude))

            LatLng(
                zoneCenter.latitude + (kotlin.math.sin(radians) * distance / metersPerLat),
                zoneCenter.longitude + (kotlin.math.cos(radians) * distance / metersPerLng)
            )
        }
    }

    fun evaluateRoute(
        route: List<LatLng>,
        redZones: List<RedZoneDto>,
        redZoneRadiusMeters: Double
    ): SafeRouteResult {
        return SafeRouteResult(
            points = route,
            dangerousIntersections = countDangerousIntersections(
                routePoints = route,
                redZones = redZones,
                redZoneRadiusMeters = redZoneRadiusMeters
            ),
            distanceMeters = routeLengthMeters(route)
        )
    }
}