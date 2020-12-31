import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.sphericalchickens.osmptrailchallenge.loaders.GpsLoaderFactory
import com.sphericalchickens.osmptrailchallenge.loaders.LoaderResult
import com.sphericalchickens.osmptrailchallenge.model.Coordinates
import com.sphericalchickens.osmptrailchallenge.model.Grid
import com.sphericalchickens.osmptrailchallenge.model.GridBuilder
import com.sphericalchickens.osmptrailchallenge.model.LatLngBounds
import com.sphericalchickens.osmptrailchallenge.model.Trail
import com.sphericalchickens.osmptrailchallenge.model.TrailLocations
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

class TrailChallengeController(
  private val gpsLoader: GpsLoaderFactory,
  val database: FirebaseDatabase
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

  fun processActivity(runFile: String): List<Pair<Double, Trail>> {
    val gpxLoader = gpsLoader.getLoaderByExtension("gpx")
    val activity = gpxLoader.load(File(runFile).inputStream())
    val candidateSegments = candidateSegments(activity, grid, trails)
    val trailScores = scoreTrails(activity, candidateSegments).sortedByDescending { it.first }

    return trailScores.filter { (score, _) ->
      score >= 0.9
    }
  }

  private fun candidateSegments(
    activity: LoaderResult,
    grid: Grid,
    trails: Map<String, Trail>
  ): List<Trail> {
    return activity.segments
      .flatMap { it.locations }
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
    activity: LoaderResult,
    candidateSegments: List<Trail>
  ): List<Pair<Double, Trail>> {
    // Create a grid with a thicker line, but smaller tiles
    // but add the eight neighbors as well

    // Calculate the bounds of activity
    val activityLocations = activity.segments.flatMap { it.locations }
    val activityBounds = LatLngBounds.createFromLocations(activityLocations)
    val grid = GridBuilder(activityBounds, cellSizeMeters = 20)
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

  fun updateSegmentsForAthlete(athleteId: Int, activityId: Long, activityDate: Date, completedSegments: List<Trail>) {
    val dateString = ISO_8601_FORMAT.format(activityDate)
    val activityLink = "https://www.strava.com/activities/${activityId}"

    val completed = completedSegments.map { trail ->
      trail.segmentId to CompletedSegment.fromTrail(trail, dateString, activityLink)
    }.toMap()
//    println(athleteId)
//    println(completed.joinToString("\n"))

    val ref: DatabaseReference = database.getReference("athletes/${athleteId}")

    val completedSegmentsRef = ref.child("completed")

    val done = AtomicInteger(1)
    completedSegmentsRef.updateChildren(completed) { _, _ ->
      done.decrementAndGet()
    }

    while (done.get() > 0) {
      Thread.sleep(100)
    }
  }

  companion object {
    const val CELL_SIZE_METERS = 200
    val ISO_8601_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'")
  }
}

data class CompletedSegment(
  val segmentId: String,
  val trailId: String,
  val name: String,
  val date: String,
  val link: String,
  val activityLink: String,
) {
  companion object {
    fun fromTrail(trail: Trail, date: String, activityLink: String) : CompletedSegment {
      return with(trail) {
        val link = "https://maps.bouldercolorado.gov/osmp-trails/?find=${trailId}"
        CompletedSegment(segmentId, trailId, name, date, link, activityLink)
      }
    }
  }
}