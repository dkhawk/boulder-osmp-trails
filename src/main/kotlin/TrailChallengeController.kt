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
import com.sphericalchickens.osmptrailchallenge.model.Location
import com.sphericalchickens.osmptrailchallenge.model.TrailSegment
import com.sphericalchickens.osmptrailchallenge.model.TrailLocations
import com.sphericalchickens.osmptrailchallenge.model.TrailStats
import com.sphericalchickens.osmptrailchallenge.model.TrailsSummary
import java.io.File
import java.io.FileWriter
import kotlin.math.roundToInt

class TrailChallengeController(
  private val gpsLoader: GpsLoaderFactory,
  private val database: Firestore,
) {
  companion object {
    const val CELL_SIZE_METERS = 200
    const val COMPLETED_SEGMENT_CELL_SIZE_METERS = 15
    const val MATCH_THRESHOLD = 0.9
  }

  private lateinit var grid: Grid
  lateinit var segments: Map<String, TrailSegment>
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

  fun calculateCompletedStats(completedSegments: List<CompletedSegment>): TrailsSummary {
    val completedIds = completedSegments.map { it.segmentId }.toSet()

    var totalDistance = 0
    var completedDistance = 0

    val trailStats = trails.map { (trailId, trail) ->
      val data = trail.segments.partition { completedIds.contains(it) }
      val distDone = data.first.sumBy { segments[it]?.length ?: 0 }
      val percentDone = distDone.toDouble() / trail.length

      totalDistance += trail.length
      completedDistance += distDone

      trailId to TrailStats(
        name = trail.name,
        length = trail.length,
        completedDistance = distDone,
        percentDone = percentDone,
        completed = data.first,
        remaining = data.second
      )
    }.toMap()

    val totalPercentDone = completedDistance.toDouble() / totalDistance

    return TrailsSummary(trailStats = trailStats,
                         totalDistance = totalDistance,
                         completedDistance = completedDistance,
                         percentDone = totalPercentDone
    )
  }

  fun writeCompletedTrailStats(completedTrailStats: TrailsSummary, athleteId: String) {
    val docRef: DocumentReference = database.collection("athletes").document(athleteId)
    val stats : Map<String, Any> = completedTrailStats.getStats()
    val r = docRef.set(mapOf("overallStats" to stats))

    val trailsCollection = docRef.collection("trailStats")
    val results = completedTrailStats.trailStats.map {
      trailsCollection.document(it.key).set(it.value)
    }
    results.forEach { it.get() }
    r.get()
  }

  // Writes all of the trail segments to the database.  Use sparingly.
  fun writeSegmentsToDatabase() {
    val segmentsRef = database.collection("segments")
    segments.map { (segmentId, segment) ->
      segmentsRef.document(segment.segmentId).set(EncodedSegment.fromTrailSegment(segment))
    }.forEach { it.get() }
  }
}

data class EncodedSegment(
  val trailId: String,
  val segmentId: String,
  val name: String,
  val length: Int,
  val bounds: LatLngBounds,
  val encodedLocations: String
) {
  companion object {
    fun fromTrailSegment(trailSegment: TrailSegment): EncodedSegment {
      return EncodedSegment(
        trailSegment.trailId,
        trailSegment.segmentId,
        trailSegment.name,
        trailSegment.length,
        trailSegment.bounds,
        encodePolyline(trailSegment.locations)
      )
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

fun encodePolyline(polyline: List<Location>): String {
  /*
    https://developers.google.com/maps/documentation/utilities/polylinealgorithm#example

    Example
    Points: (38.5, -120.2), (40.7, -120.95), (43.252, -126.453)

    Latitude	Longitude	Latitude in E5	Longitude in E5	Change In Latitude	Change In Longitude	Encoded Latitude	Encoded Longitude	Encoded Point
    38.5	-120.2	3850000	-12020000	+3850000	-12020000	_p~iF	~ps|U	_p~iF~ps|U
    40.7	-120.95	4070000	-12095000	+220000	-75000	_ulL	nnqC	_ulLnnqC
    43.252	-126.453	4325200	-12645300	+255200	-550300	_mqN	vxq`@	_mqNvxq`@
  */

  var lastLat = 0.0
  var lastLng = 0.0

  return polyline.map { location ->
    val dLat = location.lat - lastLat
    val dLng = location.lng - lastLng

    lastLat = location.lat
    lastLng = location.lng

    val result = Pair(encodeValue(dLat), encodeValue(dLng))
    result
  }.map { it.first + it.second }.joinToString("")
}

fun encodeValue(value: Double): String {
  val valueScaled = (value * 1e5).roundToInt()
  val isNegative = valueScaled < 0
  val leftShifted = valueScaled shl 1
  val inverted = if (isNegative) {
    leftShifted.inv()
  } else {
    leftShifted
  }
  val fiveBitChunksReversed = inverted.toBinaryArray().toList().reversed().windowed(5, 5)
    .map { it.reversed().toCharArray() }.toMutableList()

  while (fiveBitChunksReversed.size > 1 && fiveBitChunksReversed.last().joinToString("").toInt(2) == 0) {
    fiveBitChunksReversed.removeAt(fiveBitChunksReversed.lastIndex)
  }

  val continuationEncoding = fiveBitChunksReversed.map { chunk ->
    (listOf('1') + chunk.toList()).toCharArray()
  }.toMutableList()

  continuationEncoding.last()[0] = '0'
  return continuationEncoding.map { it.joinToString("").toInt(2) + 63 }.map { it.toChar() }.joinToString("")
}

private fun Int.toBinaryArray(): CharArray {
  var index = 31
  val result = CharArray(32)
  while (index >= 0) {
    result[31 - index] = if (this and (1 shl index) != 0) '1' else '0'
    index -= 1
  }
  return result
}
