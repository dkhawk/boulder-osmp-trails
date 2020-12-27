package com.sphericalchickens.osmptrailchallenge.model

data class Trail(
  var trailId: String = "unknown",
  var segmentId: String = "unknown",
  var name: String = "unknown",
  var length: Int = 0,
  var bounds: LatLngBounds = LatLngBounds(),
  var locations: List<Location> = emptyList()
) {
  companion object {
    fun from(segment: Segment): Trail {
      return Trail(segment.trailId, segment.segmentId, segment.name, segment.length, segment.bounds, segment.locations)
    }
  }
}
