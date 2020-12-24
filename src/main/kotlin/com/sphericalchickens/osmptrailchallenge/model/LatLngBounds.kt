package com.sphericalchickens.osmptrailchallenge.model

class LatLngBounds {
  var minLongitude: Double = 0.0
  var maxLongitude: Double = 0.0
  var minLatitude: Double = 0.0
  var maxLatitude: Double = 0.0

  var first = true

  fun update(location : Location) {
    if (first) {
      first = false
      minLongitude = location.lng
      maxLongitude = location.lng
      minLatitude = location.lat
      maxLatitude = location.lat
      return
    }
    minLongitude = minLongitude.coerceAtMost(location.lng)
    maxLongitude = maxLongitude.coerceAtLeast(location.lng)
    minLatitude = minLatitude.coerceAtMost(location.lat)
    maxLatitude = maxLatitude.coerceAtLeast(location.lat)
  }
}
