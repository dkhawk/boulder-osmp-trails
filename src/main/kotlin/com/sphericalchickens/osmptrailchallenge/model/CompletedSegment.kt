package com.sphericalchickens.osmptrailchallenge.model

import com.google.cloud.Timestamp

data class CompletedSegment(
  val segmentId: String,
  val trailId: String,
  val name: String,
  val length: Int,
  val activityId: String,
  val timestamp: Timestamp
) {

}
