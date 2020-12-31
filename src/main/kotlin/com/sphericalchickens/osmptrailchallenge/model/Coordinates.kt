package com.sphericalchickens.osmptrailchallenge.model

data class Coordinates(val x: Int, val y: Int) {
  fun getNeighbors(): List<Coordinates> {
    return Direction.values().map { heading->
      this + heading.vector
    }
  }

  fun serialize() = "$x,$y"

  operator fun plus(vector: Vector): Coordinates = Coordinates(x + vector.dx, y + vector.dy)
  operator fun minus(vector: Vector): Coordinates = Coordinates(x - vector.dx, y - vector.dy)
}