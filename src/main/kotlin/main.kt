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
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicBoolean


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

//  val runFileName = "/Users/dkhawk/Downloads/GreenMountainCrownRock.gpx"
  val runFileName = "/Users/dkhawk/Downloads/Collecting_data.gpx"

  val loader = GpsLoaderFactory()
  val gpxLoader = loader.getLoaderByExtention("gpx")

//  val database = getDatabaseConnection()
  val trails = readTrailsFromFile(trailTextFilename)

//  val trails = readFromKml(loader, trailDataFileName)
//  writeTrailsToFile("/Users/dkhawk/Downloads/gregory.txt", trails)
//  readFromDatabase(database)

  // TODO: before writing to the database, be sure to convert to
//  writeToDatabase(database, trails)

  // TODO: not happy about this sleep for reading from the database.
//  Thread.sleep(5000)

  // Now calculate the bounds of all the trails
    val bounds = LatLngBounds.createFromBounds(trails.map { (_, value) -> value.bounds })
//  val bounds = LatLngBounds(
//    minLatitude = 39.9139860039965,
//    minLongitude = -105.406643752203,
//    maxLatitude = 40.1164546952824,
//    maxLongitude = -105.131874521385
//  )

  val grid = Grid(bounds, borderWidth = 1, cellSizeMeters = 100)

  trails.entries.forEach { (_, trail) ->
    trail.locations.forEach { location ->
      // Map to the tile
      grid.incrementLatLng(location)
      // TODO: make a "lineTo" function!
      grid.addSegmentToCell(location, trail.segmentId)
    }
  }
  //  println(grid)
  //  println(grid.gridOfSegments.count { it.isNotEmpty() })

  val activity = gpxLoader.load(File(runFileName).inputStream())
  //  println(activity.segments.first().locations.size)
  val activityTiles = activity.segments.first().locations.map { location ->
    grid.locationToTileCoords(location)
  }.toSet()
//  println(activityTiles.sortedWith( compareBy({ it.y }, { it.x })).joinToString("\n"))

  val segsId = activityTiles.mapNotNull { coordinates ->
    grid.getSegmentsAt(coordinates)?.toList()
  }.flatten().toSet()

  println(segsId)
  val segs = segsId.mapNotNull { trails[it] }
  println(segsId.map { trails[it]?.name }.joinToString("\n"))

  val bounds2 = LatLngBounds.createFromBounds(segs.map { value -> value.bounds })

  val newGrid = Grid(bounds2)
  segs.forEach { trail ->
    trail.locations.forEach { location ->
      // Map to the tile
      newGrid.incrementLatLng(location)
      // TODO: make a "lineTo" function!
      newGrid.addSegmentToCell(location, trail.segmentId)
    }
  }

  println(newGrid)

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