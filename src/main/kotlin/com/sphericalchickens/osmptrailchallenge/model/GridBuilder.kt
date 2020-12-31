package com.sphericalchickens.osmptrailchallenge.model

import kotlin.math.ceil

class GridBuilder(private val bounds: LatLngBounds, cellSizeMeters: Int = 200) {
  private val coordinatesToSegments = mutableMapOf<Coordinates, MutableSet<String>>()
  private val minLat: Double
  private val minLng: Double

  private val latDegrees: Double
  private val lngDegrees: Double
  private val nsNumCells: Int
  private val ewNumCells: Int

  init {
    // The height and width in degrees of the bounds
    val (northSouthMeters, eastWestMeters) = bounds.getDimensionsMeters()

    // The number of cells to contain the bounds
    nsNumCells = ceil(northSouthMeters / cellSizeMeters).toInt()
    ewNumCells = ceil(eastWestMeters / cellSizeMeters).toInt()

    val northSouthDegrees = bounds.maxLatitude - bounds.minLatitude
    val eastWestDegrees = bounds.maxLongitude - bounds.minLongitude

    val latDegreesPerCell = northSouthDegrees / nsNumCells
    val lngDegreesPerCell = eastWestDegrees / ewNumCells

    minLat = bounds.minLatitude - latDegreesPerCell
    val maxLat = bounds.maxLatitude + latDegreesPerCell
    minLng = bounds.minLongitude - lngDegreesPerCell
    val maxLng = bounds.maxLongitude + lngDegreesPerCell

    latDegrees = maxLat - minLat
    lngDegrees = maxLng - minLng
  }

  fun addSegmentToCell(location: Location, segmentId: String) {
    val (col, row) = locationToTileCoordinates(location)
    addSegmentToCell(col, row, segmentId)
  }

  fun locationToTileCoordinates(location: Location): Coordinates {
    val y = (((location.lat - minLat) / latDegrees) * nsNumCells).toInt()
    val x = (((location.lng - minLng) / lngDegrees) * ewNumCells).toInt()
    return Coordinates(x = x, y = y)
  }

  private fun addSegmentToCell(x: Int, y: Int, segmentId: String) {
    val coordinates = Coordinates(x = x, y = y)
    if (!coordinatesToSegments.containsKey(coordinates)) {
      coordinatesToSegments[coordinates] = mutableSetOf()
    }
    coordinatesToSegments[coordinates]?.add(segmentId)
  }

  fun build(): Grid {
    val coordsToSegments = coordinatesToSegments.map {
      it.key to it.value.toList()
    }.toMap()
    return Grid(bounds, width = ewNumCells, height = nsNumCells, coordsToSegments)
  }
}
