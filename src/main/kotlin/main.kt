import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.sphericalchickens.osmptrailchallenge.loaders.GpsLoaderFactory
import com.sphericalchickens.osmptrailchallenge.model.Grid
import com.sphericalchickens.osmptrailchallenge.model.LatLngBounds
import com.sphericalchickens.osmptrailchallenge.model.Location
import com.sphericalchickens.osmptrailchallenge.model.Trail
import com.sphericalchickens.osmptrailchallenge.model.UnitsUtility
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import org.gavaghan.geodesy.Ellipsoid
import org.gavaghan.geodesy.GeodeticCalculator
import org.gavaghan.geodesy.GlobalCoordinates


//////////////////// Note //////////////////////////////
//
// For debugging add to the VM args in the launcher
// -Dorg.slf4j.simpleLogger.defaultLogLevel=trace
//
//////////////////// Note //////////////////////////////

fun main(args: Array<String>) {
  println("Hello World!")

//  val trailDataFileName = "/Users/dkhawk/Downloads/OSMP_Trails.kml"
//  val trailDataFileName = "/Users/dkhawk/Downloads/OSMP_Trails_truncated.kml"
//  val trailDataFileName = "/Users/dkhawk/Downloads/gregory.kml"

//  val trailTextFilename = "/Users/dkhawk/Downloads/gregory.txt"
  val trailTextFilename = "/Users/dkhawk/Downloads/OSMP-trails.txt"

  val runFileName = "/Users/dkhawk/Downloads/GreenMountainCrownRock.gpx"

  val loader = GpsLoaderFactory()
  val gpxLoader = loader.getLoaderByExtention("gpx")

//  val database = getDatabaseConnection()
  val trails = readTrailsFromFile(trailTextFilename)

//  val trails = readFromKml(loader, trailDataFileName)
//  writeTrailsToFile("/Users/dkhawk/Downloads/gregory.txt", trails)
//  readFromDatabase(database)
//  writeToDatabase(database, trails)
  // TODO: not happy about this sleep for reading from the database.
//  Thread.sleep(5000)

//  val activity = gpxLoader.load(File(runFileName).inputStream())
//  println(activity.segments.first().locations.size)

  // Now calculate the bounds of all the trails
  //  val bounds = LatLngBounds.createFromBounds(trails.map { (key, value) -> value.bounds!! })
  val bounds = LatLngBounds(
    minLatitude = 39.9139860039965,
    minLongitude = -105.406643752203,
    maxLatitude = 40.1164546952824,
    maxLongitude = -105.131874521385
  )
  println(bounds)

//  val ellipseMiles = ellipseKilometers * 0.621371192
//  println(ellipseKilometers)
//  println(ellipseMiles)

  val (northSouthMeters, eastWestMeters) = bounds.getDimensionsMeters()
  println("$northSouthMeters, $eastWestMeters")
  println("${UnitsUtility.metersToMiles(northSouthMeters)}," +
            " ${UnitsUtility.metersToMiles(eastWestMeters)}")

  val gridSizeMeters = 200.0
  val nsGridWidth = ceil(northSouthMeters / gridSizeMeters).toInt()
  val ewGridWidth = ceil(eastWestMeters / gridSizeMeters).toInt()
  println("$ewGridWidth x $nsGridWidth")

  val latSize = bounds.maxLatitude - bounds.minLatitude
  val lngSize = bounds.maxLongitude - bounds.minLongitude

  println("Lat: $latSize, Lng: $lngSize")
  val latGridSizeDegrees = latSize / nsGridWidth
  val lngGridSizeDegrees = lngSize / ewGridWidth

  println("lat: $latGridSizeDegrees, lng: $lngGridSizeDegrees")
  // plus 2 to add a border around the entire grid
  val grid = Grid(width = ewGridWidth + 2, height = nsGridWidth + 2)

  val minLat = bounds.minLatitude - (latGridSizeDegrees / nsGridWidth)
  val maxLat = bounds.maxLatitude + (latGridSizeDegrees / nsGridWidth)
  val minLng = bounds.minLongitude - (lngGridSizeDegrees / ewGridWidth)
  val maxLng = bounds.maxLongitude + (lngGridSizeDegrees / ewGridWidth)
  val newLatSize = maxLat - minLat
  val newLngSize = maxLng - minLng

  // Gregory
//  val segmentId = "266-152-363"
  //  val segmentId = "266-143-151"

  // Buckingham Park
//  val segmentId = "417-665-663"
  // East Boulder
  val segments = listOf("502-539-555", "502-555-554", "501-537-555", "515-534-535", "503-534-536",
                        "503-530-534", "516-531-532", "516-531-533", "503-529-530", "516-529-531",
                        "503-528-529", "503-530-528", "503-527-528")

//  segments.forEach { segmentId ->
//    trails[segmentId]?.locations?.forEach { location ->
//      // Map to the tile
//      val row = (((location.lat - minLat) / newLatSize) * nsGridWidth).toInt()
//      val col = (((location.lng - minLng) / newLngSize) * ewGridWidth).toInt()
//      grid.increment(col, row)
//    }
//  }

  trails.entries.forEach { (_, trail) ->
    trail.locations.forEach { location ->
      // Map to the tile
      val row = (((location.lat - minLat) / newLatSize) * nsGridWidth).toInt()
      val col = (((location.lng - minLng) / newLngSize) * ewGridWidth).toInt()
      grid.increment(col, row)
    }
  }

  println(grid)


}

private fun getDatabaseConnection(): FirebaseDatabase {
  val serviceAccount =
    FileInputStream("/Users/dkhawk/Downloads/osmp-trail-challenge-firebase-adminsdk-te5an-d35f4253d6.json")

  val options: FirebaseOptions = FirebaseOptions.Builder()
    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
    .setDatabaseUrl("https://osmp-trail-challenge-default-rtdb.firebaseio.com/")
    .build()

  FirebaseApp.initializeApp(options)

  return FirebaseDatabase.getInstance()
}

fun readTrailsFromFile(filename: String): Map<String, Trail> {
  return File(filename).readLines().map { line ->
    trailFromString(line)
  }.map { trail ->
    trail.segmentId!! to trail
  }.toMap()
}

fun writeTrailsToFile(filename: String, trails: Map<String, Trail>) {
  FileWriter(filename).use { writer ->
    trails.entries.forEach { trailEntry ->
      writer.write(trailEntry.value.serialize())
      writer.write("\n")
    }
  }
}

fun trailFromString(line: String): Trail {
  val parts = line.split(",")
  val trailId = parts[0]
  val segmentId = parts[1]
  val name = parts[2].trim('"')
  val length = parts[3].toInt()
  val bounds = boundsFromString(parts.subList(4, 4 + 4))
  val locations = locationsFromString(parts.subList(4 + 4, parts.size))
  return Trail(trailId, segmentId, name, length, bounds, locations)
}

fun locationsFromString(coords: List<String>): List<Location> {
  return coords.windowed(2, 2).map { Location(it[0].toDouble(), it[1].toDouble()) }
}

fun boundsFromString(boundsCoords: List<String>): LatLngBounds {
  return LatLngBounds(
    boundsCoords[0].toDouble(), boundsCoords[1].toDouble(),
    boundsCoords[2].toDouble(), boundsCoords[3].toDouble()
  )
}

private fun Trail.serialize(): String {
  return "$trailId,$segmentId,\"$name\",$length,${bounds!!.serialize()},${locations!!.serialize()}"
}

private fun List<Location>.serialize(): String {
  return joinToString(",") { it.serialize() }
}

private fun Location.serialize(): String {
  return "$lat,$lng"
}

private fun LatLngBounds.serialize(): String {
  return "$minLatitude,$minLongitude,$maxLatitude,$maxLongitude"
}

fun readFromKml(loader: GpsLoaderFactory, trailDataFileName: String): Map<String, Trail> {
  val kmlLoader = loader.getLoaderByExtention("kml")
//    val time = measureTimeMillis {
  val trailsData = kmlLoader.load(File(trailDataFileName).inputStream())
  println("Loaded ${trailsData.segments.size} trails")
  return trailsData.segments.map {
    val trail = Trail.from(it)
    trail.segmentId!! to trail
  }.toMap()
}

fun readFromDatabase(database: FirebaseDatabase) {
  val ref: DatabaseReference? = database.getReference("osmp/trails/by-segment-id/trails")

//    ref!!.addValueEventListener(object : ValueEventListener {
//      override fun onDataChange(dataSnapshot: DataSnapshot) {
//        val trail = dataSnapshot.getValue(Trail::class.java)
//        println(trail)
//      }
//
//      override fun onCancelled(databaseError: DatabaseError) {
//        println("The read failed: " + databaseError.code)
//      }
//    })

  ref!!.addChildEventListener(object : ChildEventListener {
    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
      val trail = snapshot.getValue(Trail::class.java)
      println(trail)
    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
      TODO("Not yet implemented")
    }

    override fun onChildRemoved(snapshot: DataSnapshot) {
    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
      TODO("Not yet implemented")
    }

    override fun onCancelled(error: DatabaseError?) {
      println("The read failed: " + error?.code)
    }
  })
}

fun writeToDatabase(database: FirebaseDatabase, trails: Map<String, Trail>) {
  val ref: DatabaseReference? = database.getReference("osmp/trails/by-segment-id")
  val usersRef = ref!!.child("trails")

  val done = AtomicBoolean(false)
  usersRef.setValue(trails) { _, _ ->
    done.set(true)
  }

  while (!done.get()) {
    Thread.sleep(100)
  }
}