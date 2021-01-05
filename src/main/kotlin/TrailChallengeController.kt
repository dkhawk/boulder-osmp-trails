import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.sphericalchickens.osmptrailchallenge.loaders.GpsLoaderFactory
import com.sphericalchickens.osmptrailchallenge.model.Activity
import com.sphericalchickens.osmptrailchallenge.model.Coordinates
import com.sphericalchickens.osmptrailchallenge.model.Grid
import com.sphericalchickens.osmptrailchallenge.model.GridBuilder
import com.sphericalchickens.osmptrailchallenge.model.LatLngBounds
import com.sphericalchickens.osmptrailchallenge.model.Trail
import com.sphericalchickens.osmptrailchallenge.model.TrailLocations
import java.io.File
import java.io.FileWriter

class TrailChallengeController(
  private val gpsLoader: GpsLoaderFactory,
  private val database: Firestore,
) {
  private lateinit var grid: Grid
  private lateinit var trails: Map<String, Trail>

  fun loadTrailsFromFile(filename: String) {
    trails = File(filename).readLines().map { line ->
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
    trails = trailsData.segments.map {
      val trail = Trail.from(it)
      trail.segmentId to trail
    }.toMap()
  }

  fun createGridFromTrails() {
    val bounds = LatLngBounds.createFromBounds(trails.map { (_, value) -> value.bounds })
    val gridBuilder = GridBuilder(bounds, cellSizeMeters = CELL_SIZE_METERS)
    trails.entries.forEach { (_, trail) ->
      trail.locations.forEach { location ->
        gridBuilder.addSegmentToCell(location, trail.segmentId)
      }
    }
    grid = gridBuilder.build()
  }

  fun processActivity(activity: Activity): List<Pair<Double, Trail>> {
    val candidateSegments = candidateSegments(activity, grid, trails)
    val trailScores = scoreTrails(activity, candidateSegments).sortedByDescending { it.first }

    return trailScores.filter { (score, _) ->
      score >= MATCH_THRESHOLD
    }
  }

  private fun candidateSegments(
    activity: Activity,
    grid: Grid,
    trails: Map<String, Trail>
  ): List<Trail> {
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
    candidateSegments: List<Trail>
  ): List<Pair<Double, Trail>> {
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
      trails.entries.forEach { trailEntry ->
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

  private fun trailFromString(line: String): Trail {
    val parts = line.split(",")
    val trailId = parts[0]
    val segmentId = parts[1]
    val name = parts[2].trim('"')
    val length = parts[3].toInt()
    val bounds = LatLngBounds.fromString(parts.subList(4, 4 + 4))
    val locations = TrailLocations.fromString(parts.subList(4 + 4, parts.size))
    return Trail.fromParams(trailId, segmentId, name, length, bounds, locations)
  }

  fun updateSegmentsForAthlete(
    athleteId: String,
    activity: Activity,
    completedSegments: List<Trail>
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
    trail: Trail
  ): MutableMap<String, Any> {
    val data = mutableMapOf<String, Any>()
    data["activityId"] = activity.activityId
    data["segmentId"] = trail.segmentId
    data["trailId"] = trail.trailId
    data["trailName"] = trail.name
    data["timestamp"] = Timestamp.ofTimeSecondsAndNanos(activity.activityDate.epochSecond, 0)
    data["length"] = trail.length
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
}
