package com.sphericalchickens.osmptrailchallenge.model

import org.gavaghan.geodesy.Ellipsoid
import org.gavaghan.geodesy.GeodeticCalculator
import org.gavaghan.geodesy.GlobalCoordinates

data class LatLngBounds(
  var minLatitude: Double = 0.0,
  var minLongitude: Double = 0.0,
  var maxLatitude: Double = 0.0,
  var maxLongitude: Double = 0.0,
) {

  fun getDimensionsMeters(): Pair<Double, Double> {
    // instantiate the calculator
    val geoCalc = GeodeticCalculator()

    // select a reference ellipsoid
    val reference = Ellipsoid.WGS84

    val southWest = GlobalCoordinates(minLatitude, minLongitude)
    val northWest = GlobalCoordinates(maxLatitude, minLongitude)
    val southEast = GlobalCoordinates(minLatitude, maxLongitude)

    // calculate the geodetic curve
    val northSouthCurve = geoCalc.calculateGeodeticCurve(reference, southWest, northWest)
    val northSouthDistance = northSouthCurve.ellipsoidalDistance

    val eastWestCurve = geoCalc.calculateGeodeticCurve(reference, southWest, southEast)
    val eastWestDistance = eastWestCurve.ellipsoidalDistance

    return Pair(northSouthDistance, eastWestDistance)
  }

  fun latDegrees(): Double = maxLatitude - minLatitude
  fun lngDegrees(): Double = maxLongitude - minLongitude
  fun serialize(): String {
    return "$minLatitude,$minLongitude,$maxLatitude,$maxLongitude"
  }

  companion object {
    fun createFromLocations(locations: List<Location>): LatLngBounds {
      check(locations.isNotEmpty())
      val first = locations.first()
      var minLongitude = first.lng
      var maxLongitude = first.lng
      var minLatitude = first.lat
      var maxLatitude = first.lat

      locations.forEach { location ->
        minLongitude = minLongitude.coerceAtMost(location.lng)
        maxLongitude = maxLongitude.coerceAtLeast(location.lng)
        minLatitude = minLatitude.coerceAtMost(location.lat)
        maxLatitude = maxLatitude.coerceAtLeast(location.lat)
      }
      return LatLngBounds(minLatitude, minLongitude, maxLatitude, maxLongitude)
    }

    fun createFromBounds(bounds: List<LatLngBounds>): LatLngBounds {
      check(bounds.isNotEmpty())
      val first = bounds.first()
      var minLongitude = first.minLongitude
      var maxLongitude = first.maxLongitude
      var minLatitude = first.minLatitude
      var maxLatitude = first.maxLatitude

      bounds.forEach { bound ->
        minLongitude = minLongitude.coerceAtMost(bound.minLongitude)
        maxLongitude = maxLongitude.coerceAtLeast(bound.maxLongitude)
        minLatitude = minLatitude.coerceAtMost(bound.minLatitude)
        maxLatitude = maxLatitude.coerceAtLeast(bound.maxLatitude)
      }

      return LatLngBounds(minLatitude, minLongitude, maxLatitude, maxLongitude)
    }

    fun fromString(boundsCoords: List<String>): LatLngBounds {
      return LatLngBounds(
        boundsCoords[0].toDouble(), boundsCoords[1].toDouble(),
        boundsCoords[2].toDouble(), boundsCoords[3].toDouble()
      )
    }
  }
}
