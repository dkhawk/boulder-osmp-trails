package com.sphericalchickens.osmptrailchallenge.model

import kotlin.math.ceil

class Grid(bounds: LatLngBounds, borderWidth: Int = 1, cellSizeMeters: Int = 200) {
  private val width: Int
  private val height: Int
  private val grid = mutableListOf<Int>()
  private val gridOfSegments = mutableListOf<MutableSet<String>>()
  private var segmentsMap = HashMap<Coordinates, MutableSet<String>>()
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

    width = ewNumCells + (borderWidth * 2)
    height = nsNumCells + (borderWidth * 2)

    repeat(width * height) {
      grid.add(0)
      gridOfSegments.add(mutableSetOf())
    }
  }

  override fun toString(): String {
    // Flip such that y increases going up!
    return gridOfSegments.windowed(width, width).map { row ->
      row.joinToString("") { value ->
        val count = value.size
        if (count > 0) {
          count.coerceAtMost(9).toString().padStart(1, '0')
        } else {
          "."
        }.padEnd(1,' ').padStart(1,' ')
      }
    }.reversed().joinToString("\n")
  }

  private fun increment(x: Int, y: Int) {
    grid[getIndex(x, y)] += 1
  }

  private fun getIndex(x: Int, y: Int): Int {
    check(y < height)
    check(x < width)
    return (y * width) + x
  }

  fun incrementLatLng(location: Location) {
    val coordinates = locationToTileCoordinates(location)
    increment(coordinates.x, coordinates.y)
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
    gridOfSegments[getIndex(x, y)].add(segmentId)
    val coordinates = Coordinates(x = x, y = y)
    if (!segmentsMap.containsKey(coordinates)) {
      segmentsMap[coordinates] = mutableSetOf()
    }
    segmentsMap[coordinates]?.add(segmentId)
  }

  fun getSegmentsAt(coordinates: Coordinates): MutableSet<String>? {
    return segmentsMap[coordinates]
  }
}

data class Coordinates(val x: Int, val y: Int) {
  fun getNeighbors(): List<Coordinates> {
    return Heading8.values().map { heading->
      this + heading.vector
    }
  }

  operator fun plus(vector: Vector): Coordinates = Coordinates(x + vector.dx, y + vector.dy)
  operator fun minus(vector: Vector): Coordinates = Coordinates(x - vector.dx, y - vector.dy)
}

enum class Heading8(val vector: Vector) {
  NORTH(Vector(0, -1)),
  NORTHEAST(Vector(1, -1)),
  EAST(Vector(1, 0)),
  SOUTHEAST(Vector(1, 1)),
  SOUTH(Vector(0, 1)),
  SOUTHWEST(Vector(-1, 1)),
  WEST(Vector(-1, 0)),
  NORTHWEST(Vector(-1, -1));
}

data class Vector(val dx: Int, val dy: Int) {
  operator fun times(scale: Int): Vector {
    return Vector(dx * scale, dy * scale)
  }
}
