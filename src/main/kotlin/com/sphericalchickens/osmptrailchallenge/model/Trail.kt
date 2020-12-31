package com.sphericalchickens.osmptrailchallenge.model

data class Trail(val metadata: TrailMetadata, val trailLocations: TrailLocations) {
  val segmentId: String
    get() {
      return metadata.segmentId
    }

  val bounds: LatLngBounds
    get() {
      return metadata.bounds
    }

  val trailId: String
    get() {
      return metadata.trailId
    }

  val name: String
    get() {
      return metadata.name
    }

  val length: Int
     get() {
       return metadata.length
     }

  val locations: List<Location>
    get() {
      return trailLocations.locations
    }

  fun serialize(): String {
    return "$trailId,$segmentId,\"$name\",$length,${bounds.serialize()},${locations.serialize()}"
  }

  companion object {
    fun from(segment: Segment): Trail {
      return Trail(
        TrailMetadata(
          segment.trailId,
          segment.segmentId,
          segment.name,
          segment.length,
          segment.bounds
        ),
        TrailLocations(segment.locations)
      )
    }

    fun fromParams(
      trailId: String,
      segmentId: String,
      name: String,
      length: Int,
      bounds: LatLngBounds,
      locations: List<Location>
    ): Trail {
      return Trail(
        TrailMetadata(
          trailId,
          segmentId,
          name,
          length,
          bounds
        ),
        TrailLocations(locations)
      )
    }
  }
}

data class TrailMetadata(
  var trailId: String = "unknown",
  var segmentId: String = "unknown",
  var name: String = "unknown",
  var length: Int = 0,
  var bounds: LatLngBounds = LatLngBounds()
)

data class TrailLocations(
  var locations: List<Location> = emptyList()
) {
  companion object {
    fun fromString(coords: List<String>): List<Location> {
      return coords.windowed(2, 2).map { Location(it[0].toDouble(), it[1].toDouble()) }
    }
  }
}


fun List<Location>.serialize(): String {
  return joinToString(",") { it.serialize() }
}

private fun Location.serialize(): String {
  return "$lat,$lng"
}

