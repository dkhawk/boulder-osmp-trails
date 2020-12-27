package com.sphericalchickens.osmptrailchallenge.model

import kotlin.math.ceil

class Grid(bounds: LatLngBounds, borderWidth: Int = 1, cellSizeMeters: Int = 200) {
  private val width: Int
  private val height: Int
  val grid = mutableListOf<Int>()
  val gridOfSegments = mutableListOf<MutableSet<String>>()
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

  fun toStringLocationCounts(): String {
    // Flip such that y increases going up!
    return grid.windowed(width, width).map { row ->
      row.joinToString("") { value ->
        if (value > 0) {
          " ${value.coerceAtMost(99).toString().padStart(2, '0')} "
        } else {
          " .. "
        }
      }
    }.reversed().joinToString("\n")
  }

  override fun toString(): String {
    // Flip such that y increases going up!
    return gridOfSegments.windowed(width, width).map { row ->
      row.joinToString("") { value ->
        val count = value.size
        if (count > 0) {
          "${count.coerceAtMost(9).toString().padStart(1, '0')}"
        } else {
          "."
        }.padEnd(1,' ').padStart(1,' ')
      }
    }.reversed().joinToString("\n")
  }

  fun increment(x: Int, y: Int) {
    val index = getIndex(x, y)
    grid[index] += 1
  }

  private fun getIndex(x: Int, y: Int): Int {
    check(y < height)
    check(x < width)
    return (y * width) + x
  }

  fun incrementLatLng(location: Location) {
    val row = (((location.lat - minLat) / latDegrees) * nsNumCells).toInt()
    val col = (((location.lng - minLng) / lngDegrees) * ewNumCells).toInt()
    increment(col, row)
  }

  fun addSegmentToCell(location: Location, segmentId: String) {
    val row = (((location.lat - minLat) / latDegrees) * nsNumCells).toInt()
    val col = (((location.lng - minLng) / lngDegrees) * ewNumCells).toInt()
    addSegmentToCell(col, row, segmentId)
  }

  private fun addSegmentToCell(x: Int, y: Int, segmentId: String) {
    val index = getIndex(x, y)
    gridOfSegments[index].add(segmentId)
  }
}
