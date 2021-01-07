package com.sphericalchickens.osmptrailchallenge.model

class TrailsSummary(
  val completedDistance: Int,
  val totalDistance: Int,
  val percentDone: Double,
  val trailStats: Map<String, TrailStats>,
) {
  fun getStats(): Map<String, Any> {
    return mapOf(
      "completedDistance" to completedDistance,
      "totalDistance" to totalDistance,
      "percentDone" to percentDone,
    )
  }
}