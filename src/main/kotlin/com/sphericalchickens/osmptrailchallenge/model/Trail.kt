package com.sphericalchickens.osmptrailchallenge.model

data class Trail(
  var trailId: String? = null,
  var segmentId: String? = null,
  var name: String? = null,
  var length: Int? = null,
  var bounds: LatLngBounds? = null,
  var locations: List<Location>? = null
) {
  companion object {
    fun from(segment: Segment): Trail {
      return Trail(segment.trailId, segment.segmentId, segment.name, segment.length, segment.bounds, segment.locations)
    }
  }
}
