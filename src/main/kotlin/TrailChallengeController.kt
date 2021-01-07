import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.sphericalchickens.osmptrailchallenge.loaders.GpsLoaderFactory
import com.sphericalchickens.osmptrailchallenge.model.Activity
import com.sphericalchickens.osmptrailchallenge.model.CompletedSegment
import com.sphericalchickens.osmptrailchallenge.model.Coordinates
import com.sphericalchickens.osmptrailchallenge.model.Grid
import com.sphericalchickens.osmptrailchallenge.model.GridBuilder
import com.sphericalchickens.osmptrailchallenge.model.LatLngBounds
import com.sphericalchickens.osmptrailchallenge.model.TrailSegment
import com.sphericalchickens.osmptrailchallenge.model.TrailLocations
import java.io.File
import java.io.FileWriter

class TrailChallengeController(
  private val gpsLoader: GpsLoaderFactory,
  private val database: Firestore,
) {
  private lateinit var grid: Grid
  private lateinit var segments: Map<String, TrailSegment>
  private val trails = mutableMapOf<String, Trail>()

  fun loadTrailsFromFile(filename: String) {
    segments = File(filename).readLines().map { line ->
      trailFromString(line)
    }.map { trail ->
      trail.segmentId to trail
    }.toMap()
  }

  fun loadGridFromFile(filename: String) {
    val lines = File(filename).readLines()
    val bounds = LatLngBounds.fromString(lines[0].split(","))
    val (width, height) = lines[1].split(",").map(String::toInt)
    val tileMap = lines.subList(2, lines.size).map { line ->
      val parts = line.split(",")
      val coords = Coordinates(parts[0].toInt(), parts[1].toInt())
      val segmentIds = parts.subList(2, parts.size)
      coords to segmentIds
    }.toMap()

    grid = Grid(bounds, width, height, tileMap)
  }

  fun loadTrailsFromKml(trailDataFileName: String) {
    val kmlLoader = gpsLoader.getLoaderByExtension("kml")
    val trailsData = kmlLoader.load(File(trailDataFileName).inputStream())
    segments = trailsData.segments.map {
      val trail = TrailSegment.from(it)
      trail.segmentId to trail
    }.toMap()
  }

  fun createGridFromTrails() {
    val bounds = LatLngBounds.createFromBounds(segments.map { (_, value) -> value.bounds })
    val gridBuilder = GridBuilder(bounds, cellSizeMeters = CELL_SIZE_METERS)
    segments.entries.forEach { (_, trail) ->
      trail.locations.forEach { location ->
        gridBuilder.addSegmentToCell(location, trail.segmentId)
      }
    }
    grid = gridBuilder.build()
  }

  fun processActivity(activity: Activity): List<Pair<Double, TrailSegment>> {
    val candidateSegments = candidateSegments(activity, grid, segments)
    val trailScores = scoreTrails(activity, candidateSegments).sortedByDescending { it.first }

    return trailScores.filter { (score, _) ->
      score >= MATCH_THRESHOLD
    }
  }

  private fun candidateSegments(
    activity: Activity,
    grid: Grid,
    trails: Map<String, TrailSegment>
  ): List<TrailSegment> {
    return activity.locations
      .asSequence()
      .map { location -> grid.locationToTileCoordinates(location) }
      .toSet()
      .asSequence()
      .mapNotNull { coordinates -> grid.getSegmentsAt(coordinates).toList() }
      .flatten()
      .toSet()
      .mapNotNull { trails[it] }
      .toList()
  }

  private fun scoreTrails(
    activity: Activity,
    candidateSegments: List<TrailSegment>
  ): List<Pair<Double, TrailSegment>> {
    // Create a grid with a thicker line, but smaller tiles
    // but add the eight neighbors as well

    // Calculate the bounds of activity
    val activityLocations = activity.locations
    val activityBounds = LatLngBounds.createFromLocations(activityLocations)
    val grid = GridBuilder(activityBounds, cellSizeMeters = COMPLETED_SEGMENT_CELL_SIZE_METERS)
    val activityTiles = activityLocations.map { grid.locationToTileCoordinates(it) }.toMutableSet()
    // Now add the eight neighbors for each tile
    val neighbors = activityTiles.map { it.getNeighbors() }.flatten()
    activityTiles.addAll(neighbors)

    // For each candidate, check the number of points included in the set of tiles for the activity
    // The score is the percentage of locations that land in a tile included in the set

    return candidateSegments.map { trail ->
      val matchingLocations =
        trail.locations.filter { activityTiles.contains(grid.locationToTileCoordinates(it)) }.count()
      val score = matchingLocations.toDouble() / trail.locations.size
      score to trail
    }
  }

  fun saveTrailsToFile(filename: String) {
    FileWriter(filename).use { writer ->
      segments.entries.forEach { trailEntry ->
        writer.write(trailEntry.value.serialize())
        writer.write("\n")
      }
    }
  }

  fun saveGridToFile(filename: String) {
    FileWriter(filename).use { writer ->
      writer.write(grid.bounds.serialize())
      writer.appendLine()
      writer.write("${grid.width},${grid.height}")
      writer.appendLine()
      writer.write(
        grid.coordinatesToSegments.toList()
          .joinToString("\n") {
            "${it.first.serialize()},${it.second.joinToString(",")}"
          }
      )
    }
  }

  private fun trailFromString(line: String): TrailSegment {
    val parts = line.split(",")
    val trailId = parts[0]
    val segmentId = parts[1]
    val name = parts[2].trim('"')
    val length = parts[3].toInt()
    val bounds = LatLngBounds.fromString(parts.subList(4, 4 + 4))
    val locations = TrailLocations.fromString(parts.subList(4 + 4, parts.size))
    return TrailSegment.fromParams(trailId, segmentId, name, length, bounds, locations)
  }

  fun updateSegmentsForAthlete(
    athleteId: String,
    activity: Activity,
    completedSegments: List<TrailSegment>
  ) {
    val completed = completedSegments.map { trail ->
      trail.segmentId to mapToCompletedSegment(activity, trail)
    }.toMap()

    val docRef: DocumentReference = database.collection("athletes").document(athleteId)
    val completedCollection = docRef.collection("completed")
    val results = completed.map {
      completedCollection.document(it.key).set(it.value)
    }
    results.forEach { it.get() }
  }

  companion object {
    const val CELL_SIZE_METERS = 200
    const val COMPLETED_SEGMENT_CELL_SIZE_METERS = 10
    const val MATCH_THRESHOLD = 0.9
  }

  private fun mapToCompletedSegment(
    activity: Activity,
    trailSegment: TrailSegment
  ): MutableMap<String, Any> {
    val data = mutableMapOf<String, Any>()
    data["activityId"] = activity.activityId
    data["segmentId"] = trailSegment.segmentId
    data["trailId"] = trailSegment.trailId
    data["trailName"] = trailSegment.name
    data["timestamp"] = Timestamp.ofTimeSecondsAndNanos(activity.activityDate.epochSecond, 0)
    data["length"] = trailSegment.length
    return data
  }

  fun loadActivity(activityFile: String, athleteId: String, activityId: String, stravaId: String): Activity {
    val gpxLoader = gpsLoader.getLoaderByExtension("gpx")
    return Activity.fromLoaderResult(
      gpxLoader.load(File(activityFile).inputStream()),
      activityId,
      athleteId,
      stravaId)
  }

  fun calculateTrailStats() {
    segments.entries.forEach { (_, segment) ->
      val trail = trails.getOrElse(segment.trailId) {
        val trail = Trail(segment.name, segment.trailId)
        trails[segment.trailId] = trail
        trail
      }
      trail.addSegment(segment)
    }
  }

  fun calculateCompletedStats(completedSegments: List<CompletedSegment>) {
    val trailProgress = mutableMapOf<String, MutableSet<CompletedSegment>>()
    completedSegments.forEach { segment ->
      val cs = trailProgress.getOrElse(segment.trailId) {
        val completed = mutableSetOf<CompletedSegment>()
        trailProgress[segment.trailId] = completed
        completed
      }
      cs.add(segment)
    }

    trailProgress.entries.forEach { (trailId, completed) ->
      val compLen = completed.sumBy { it.length }
      val trailLen = trails[trailId]!!.length
      val percent = (compLen.toDouble() / trailLen) * 100
      println("${trails[trailId]!!.name} ${percent.format(2)}% done")
    }

    println("\nTrails with no progress so far:")

    val noProgress = trails.keys.toSet().minus(trailProgress.keys.toSet())
    noProgress.forEach {
      println(trails[it]!!.name)
    }
  }
}

class Trail(val name: String, val trailId: String) {
  val segments = mutableSetOf<String>()

  var length: Int = 0

  fun addSegment(segment: TrailSegment) {
    if (!segments.contains(segment.segmentId)) {
      segments.add(segment.segmentId)
      length += segment.length
    }
  }

  override fun toString(): String = "$trailId, $name, $length"
}

private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
