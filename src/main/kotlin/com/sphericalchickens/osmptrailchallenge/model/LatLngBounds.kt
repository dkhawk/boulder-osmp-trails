package com.sphericalchickens.osmptrailchallenge.model

data class LatLngBounds(
  var minLatitude: Double? = null,
  var minLongitude: Double? = null,
  var maxLatitude: Double? = null,
  var maxLongitude: Double? = null,
) {

  companion object {
    fun createFromLocations(locations: List<Location>): LatLngBounds {
      check(locations.isNotEmpty())
      val first = locations.first()
      var minLongitude = first.lng
      var maxLongitude = first.lng
      var minLatitude = first.lat
      var maxLatitude = first.lat

      locations.forEach { location ->
        minLongitude = minLongitude!!.coerceAtMost(location.lng!!)
        maxLongitude = maxLongitude!!.coerceAtLeast(location.lng!!)
        minLatitude = minLatitude!!.coerceAtMost(location.lat!!)
        maxLatitude = maxLatitude!!.coerceAtLeast(location.lat!!)
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
        minLongitude = minLongitude!!.coerceAtMost(bound.minLongitude!!)
        maxLongitude = maxLongitude!!.coerceAtLeast(bound.maxLongitude!!)
        minLatitude = minLatitude!!.coerceAtMost(bound.minLatitude!!)
        maxLatitude = maxLatitude!!.coerceAtLeast(bound.maxLatitude!!)
      }

      return LatLngBounds(minLatitude, minLongitude, maxLatitude, maxLongitude)
    }
  }
}
