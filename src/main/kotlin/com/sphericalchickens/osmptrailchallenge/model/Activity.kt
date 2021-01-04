package com.sphericalchickens.osmptrailchallenge.model

import com.sphericalchickens.osmptrailchallenge.loaders.LoaderResult
import java.time.Instant

class Activity(
  val activityId: String,
  val athleteId: String,
  val locations: List<Location>,
  val activityDate: Instant,
  val stravaId: String
) {

  companion object {
    fun fromLoaderResult(
      loaderResult: LoaderResult,
      activityId: String,
      athleteId: String,
      stravaId: String
    ): Activity {
      val locations = loaderResult.segments.flatMap { it.locations }
      val timestamp = loaderResult.attributes.getOrDefault("timestamp", Instant.now()) as Instant
      return Activity(activityId, athleteId, locations, timestamp, stravaId)
    }
  }
}
