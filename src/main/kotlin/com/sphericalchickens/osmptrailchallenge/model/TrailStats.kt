package com.sphericalchickens.osmptrailchallenge.model

data class TrailStats(
  val name: String,
  val length: Int,
  val completedDistance: Int,
  val percentDone: Double,
  val completed: List<String>,
  val remaining: List<String>
)