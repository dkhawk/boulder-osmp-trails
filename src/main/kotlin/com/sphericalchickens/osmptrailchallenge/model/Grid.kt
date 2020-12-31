package com.sphericalchickens.osmptrailchallenge.model

data class Grid(
  val bounds: LatLngBounds,
  val width: Int,
  val height: Int,
  val coordinatesToSegments: Map<Coordinates, List<String>>
) {
  private var lngDegrees: Double = bounds.lngDegrees()
  private var latDegrees: Double = bounds.latDegrees()
  private var minLng: Double = bounds.minLongitude
  private var minLat: Double = bounds.minLatitude

  fun locationToTileCoordinates(location: Location): Coordinates {
    val y = (((location.lat - minLat) / latDegrees) * height).toInt()
    val x = (((location.lng - minLng) / lngDegrees) * width).toInt()
    return Coordinates(x, y)
  }

  fun getSegmentsAt(coordinates: Coordinates): List<String> =
    coordinatesToSegments.getOrDefault(coordinates, emptyList())
}
