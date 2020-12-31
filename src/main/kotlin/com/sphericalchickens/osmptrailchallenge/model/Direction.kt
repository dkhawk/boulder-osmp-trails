package com.sphericalchickens.osmptrailchallenge.model

enum class Direction(val vector: Vector) {
  NORTH(Vector(0, -1)),
  NORTHEAST(Vector(1, -1)),
  EAST(Vector(1, 0)),
  SOUTHEAST(Vector(1, 1)),
  SOUTH(Vector(0, 1)),
  SOUTHWEST(Vector(-1, 1)),
  WEST(Vector(-1, 0)),
  NORTHWEST(Vector(-1, -1));
}