package com.sphericalchickens.osmptrailchallenge.model

class Grid(val width: Int, val height: Int) {
  val grid = mutableListOf<Int>()

  init {
    repeat(width * height) {
      grid.add(0)
    }
  }

  override fun toString(): String {
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

  fun increment(x: Int, y: Int) {
    val index = getIndex(x, y)
    grid[index] += 1
  }

  private fun getIndex(x: Int, y: Int): Int {
    check(y < height)
    check(x < width)
    return (y * width) + x
  }
}
