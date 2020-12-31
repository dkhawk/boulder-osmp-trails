package com.sphericalchickens.osmptrailchallenge.model

data class Vector(val dx: Int, val dy: Int) {
  operator fun times(scale: Int): Vector {
    return Vector(dx * scale, dy * scale)
  }
}