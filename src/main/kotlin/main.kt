import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sphericalchickens.osmptrailchallenge.loaders.GpsLoaderFactory
import com.sphericalchickens.osmptrailchallenge.model.Grid
import com.sphericalchickens.osmptrailchallenge.model.Trail
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicInteger


//////////////////// Note //////////////////////////////
//
// For debugging add to the VM args in the launcher
// -Dorg.slf4j.simpleLogger.defaultLogLevel=trace
//
//////////////////// Note //////////////////////////////

fun main(args: Array<String>) {
  val trailDataFileName = "/Users/dkhawk/Downloads/OSMP_Trails.kml"

  val trailTextFilename = "/Users/dkhawk/Downloads/OSMP-trails.txt"
  val gridTextFilename = "/Users/dkhawk/Downloads/grid.txt"

  val greenRun = "/Users/dkhawk/Downloads/GreenMountainCrownRock.gpx"
  val tellerRun = "/Users/dkhawk/Downloads/Collecting_data.gpx"
  val boulderValleyRanchRun = "/Users/dkhawk/Downloads/Boulder Valley Ranch.gpx"
  val runFile = greenRun

  val loader = GpsLoaderFactory()
  val database = getDatabaseConnection()

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

  controller.processActivity(runFile)

  val completedSegments = controller.processActivity(runFile).map { it.second }

  // This is my athlete id
  val athleteId = 929553
//  val activityId = 4530386447
//  val activityDate = TrailChallengeController.ISO_8601_FORMAT.parse("2020-12-28T14:04:000Z")
  val activityId = 4508595960
  val activityDate = TrailChallengeController.ISO_8601_FORMAT.parse("2020-12-23T11:34:000Z")
  // 5:35 AM on Wednesday, December 23, 2020

  controller.updateSegmentsForAthlete(athleteId, activityId, activityDate, completedSegments)
}

private fun Double.format(digits: Int): String = "%.${digits}f".format(this)

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

fun readGridFromDatabase(database: FirebaseDatabase, callback: () -> Unit) {
  val ref: DatabaseReference = database.getReference("trails")
  val gridRef = ref.child("grid")

  gridRef.addValueEventListener(object : ValueEventListener {
    override fun onDataChange(dataSnapshot: DataSnapshot) {
      val grid = dataSnapshot.getValue(Grid::class.java)
      println(grid)
      callback.invoke()
    }

    override fun onCancelled(databaseError: DatabaseError) {
      println("The read failed: " + databaseError.code)
    }
  })

//  gridRef.addChildEventListener(object : ChildEventListener {
//    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//      val grid = snapshot.getValue(Grid::class.java)
//      println(grid)
//      callback.invoke()
//    }
//
//    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//    }
//
//    override fun onChildRemoved(snapshot: DataSnapshot) {
//    }
//
//    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//    }
//
//    override fun onCancelled(error: DatabaseError?) {
//      println("The read failed: " + error?.code)
//    }
//  })
}

fun writeTrailsToDatabase(database: FirebaseDatabase, trails: Map<String, Trail>, grid: Grid) {
  val ref: DatabaseReference = database.getReference("trails")

  val done = AtomicInteger(2)

  val metadataRef = ref.child("metadata")
  val metadata = trails.map { it.key to it.value.metadata }.subList(0, 1)
  metadataRef.setValue(metadata) { _, _ ->
    done.decrementAndGet()
  }

  val locationsRef = ref.child("locations")
  val locations = trails.map { it.key to it.value.locations }.subList(0, 1)
  locationsRef.setValue(locations) { _, _ ->
    done.decrementAndGet()
  }

//  val gridRef = ref.child("grid")
//  gridRef.setValue(grid) { _, _ ->
//    done.decrementAndGet()
//  }

  while (done.get() > 0) {
    Thread.sleep(100)
  }
}
