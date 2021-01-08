import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.sphericalchickens.osmptrailchallenge.loaders.GpsLoaderFactory
import com.sphericalchickens.osmptrailchallenge.model.CompletedSegment
import com.sphericalchickens.osmptrailchallenge.model.Location
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.roundToInt


//////////////////// Note //////////////////////////////
//
// For debugging add to the VM args in the launcher
// -Dorg.slf4j.simpleLogger.defaultLogLevel=trace
//
//////////////////// Note //////////////////////////////

fun main(args: Array<String>) {
////  println(encodeValue(-179.9832104))
////  println(encodeValue(38.5))
////  println(encodeValue(-120.2))
//  val result = encodePolyline(
//    listOf(
//      Location(38.5, -120.2),
//      Location(40.7, -120.95),
//      Location(43.252, -126.453)
//    )
//  )
//  println(result)
//  println(result == "_p~iF~ps|U_ulLnnqC_mqNvxq`@")
//  return


  // Strava athleteId.  Set this to your own Strava ID (or test account).
  val stravaAthleteId = "929553"
  // Username of the user being processed.  Set this to your own account id (or test account).
  val athleteId = "dkhawk@gmail.com"

  // Example activities to load.
  // The gpx file is from Strava and the stravaId here is the strava activity id.
  // It is available in the activity url.
  val southBoulderCreekLoop = TestActivity(
    stravaActivityId = "4539099599",
    filename = "/Users/dkhawk/Downloads/Twilight_Amblers.gpx"
  )
  val wonderLandToEagle = TestActivity(
    stravaActivityId = "4566776467",
    filename = "/Users/dkhawk/Downloads/Wonderland_to_Eagle.gpx"
  )
  val southMesaTowhee = TestActivity(
    stravaActivityId = "4576598089",
    filename = "/Users/dkhawk/Downloads/TMA_South_Mesa_Homestead_Towhee.gpx"
  )
  val activities = listOf(southBoulderCreekLoop, wonderLandToEagle, southMesaTowhee)

  // Everything below this should just work.  But it is possible the current directory isn't as
  // expected.
  // =====================================

  val dataDirectory: Path = Paths.get(System.getProperty("user.dir"), "src/main/resources")
  val trailDataFileName = Paths.get(dataDirectory.toString(), "OSMP_Trails.kml").toString()
  val trailTextFilename = Paths.get(dataDirectory.toString(),"OSMP-trails.txt").toString()
  val gridTextFilename = Paths.get(dataDirectory.toString(),"grid.txt").toString()

  val database = getFirestoreConnection()

  val controller =
    initializeController(trailTextFilename, gridTextFilename, trailDataFileName, database)

  val activity = controller.loadActivity(
    wonderLandToEagle.filename,
    athleteId,
    wonderLandToEagle.stravaActivityId,
    stravaAthleteId)

  val encodedActivity = encodePolyline(activity.locations)
  println(encodedActivity.length)
  println(encodedActivity)

  return
//  activities.forEach { activity ->
//    processActivity(controller, activity.filename, activity.stravaActivityId,
//                    athleteId = athleteId, stravaId = stravaAthleteId)
//  }


  // Get all of the completed segments so far
  val completedSegments = getCompletedSegmentsFor(database, athleteId)

  val completedTrailStats = controller.calculateCompletedStats(completedSegments)

  controller.writeCompletedTrailStats(completedTrailStats, athleteId = athleteId)
}

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

fun getCompletedSegmentsFor(database: Firestore, athleteId: String): List<CompletedSegment> {
  val docRef: DocumentReference = database.collection("athletes").document(athleteId)
  val completedCollection = docRef.collection("completed")

  val things = completedCollection.get().get().map {
    val activityId = it.getString("activityId")
    val segmentId = it.getString("segmentId")
    val trailId = it.getString("trailId")
    val name = it.getString("trailName")
    val timestamp = it.getTimestamp("timestamp")
    val length = it.getLong("length")!!.toInt()

    // println("$name($trailId), segmentId($segmentId) $length")
    CompletedSegment(segmentId!!, trailId!!, name!!, length, activityId!!, timestamp!!)
  }
  return things
}

data class TestActivity(val stravaActivityId: String, val filename: String)

private fun processActivity(
  controller: TrailChallengeController,
  activityFile: String,
  activityId: String,
  athleteId: String,
  stravaId: String,
) {
  val activity = controller.loadActivity(activityFile, athleteId, activityId, stravaId)
  val completedSegments = controller.processActivity(activity).map { it.second }

  controller.updateSegmentsForAthlete(athleteId, activity, completedSegments)
}

private fun initializeController(
  trailTextFilename: String,
  gridTextFilename: String,
  trailDataFileName: String,
  database: Firestore
): TrailChallengeController {
  val loader = GpsLoaderFactory()
  val controller = TrailChallengeController(loader, database)
  controller.loadTrailsFromFile(trailTextFilename)
  controller.loadGridFromFile(gridTextFilename)

  val readFromFiles = true
  if (readFromFiles) {
    controller.loadTrailsFromFile(trailTextFilename)
    controller.loadGridFromFile(gridTextFilename)
  } else {
    controller.loadTrailsFromKml(trailDataFileName)
    controller.createGridFromTrails()
  }

  controller.calculateTrailStats()

  return controller
}

private fun Double.format(digits: Int): String = "%.${digits}f".format(this)

private fun getFirestoreConnection(): Firestore {
  val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
    .setProjectId("boulder-trail-challenge")
    .setCredentials(GoogleCredentials.getApplicationDefault())
    .build()

  return firestoreOptions.service
}

private fun testDatabaseWrite() {
//  val docRef: DocumentReference = database.collection("users").document("alovelace")
//  // Add document data  with id "alovelace" using a hashmap
//  val data: MutableMap<String, Any> = HashMap()
//  data["first"] = "Ada"
//  data["last"] = "Lovelace"
//  data["born"] = 1815
//  //asynchronously write data
//  // val result: ApiFuture<WriteResult> = docRef.set(data)
//  val result = docRef.set(data)
//  // ...
//  // result.get() blocks on response
//  println("Update time : " + result.get().updateTime)
//
//  return
}
