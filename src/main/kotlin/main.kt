import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.sphericalchickens.osmptrailchallenge.loaders.GpsLoaderFactory


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
  val sanitasFlagGreen = "/Users/dkhawk/Downloads/Sunrise_ambulation.gpx"
  val activityFile = sanitasFlagGreen

  val database = getFirestoreConnection()


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



  val controller =
    initializeController(trailTextFilename, gridTextFilename, trailDataFileName, database)

  // This is my athlete id
  val athleteId = 929553
  val userName = "dkhawk@gmail.com"
//  val activityId = 4530386447
//  val activityId = 4508595960
  val activityId = 4555758447
  val activityDate = TrailChallengeController.ISO_8601_FORMAT.parse("2021-01-02T11:31:000Z")

  processActivity(controller, activityFile)
}

private fun processActivity(
  controller: TrailChallengeController,
  activityFile: String
) {
  val stravaId = "929553"
  val athleteId = "dkhawk@gmail.com"
  val activityId = "4555758447"
  val activity = controller.loadActivity(activityFile, athleteId, activityId, stravaId)
  val completedSegments = controller.processActivity(activity).map { it.second }

  controller.updateSegmentsForAthlete("dkhawk@gmail.com", activity, completedSegments)
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
  return controller
}

private fun Double.format(digits: Int): String = "%.${digits}f".format(this)

private fun getFirestoreConnection(): Firestore {
  val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
    .setProjectId("boulder-trail-challenge-300611")
    .setCredentials(GoogleCredentials.getApplicationDefault())
    .build()

  return firestoreOptions.service
}

//private fun getDatabaseConnection(): FirebaseDatabase {
////  val serviceAccount = FileInputStream("/Users/dkhawk/Downloads/boulder-trail-challenge-firebase-adminsdk-kz2z3-f3441f4054.json")
//  val serviceAccount = FileInputStream("/Users/dkhawk/Downloads/Boulder Trail Challenge-49847d8ef9a8.json")
//
//  val options = FirebaseOptions.Builder()
//    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//    .build()
//
//  FirebaseApp.initializeApp(options)
//
//  return FirebaseDatabase.getInstance()
//}

//fun readGridFromDatabase(database: FirebaseDatabase, callback: () -> Unit) {
//  val ref: DatabaseReference = database.getReference("trails")
//  val gridRef = ref.child("grid")
//
//  gridRef.addValueEventListener(object : ValueEventListener {
//    override fun onDataChange(dataSnapshot: DataSnapshot) {
//      val grid = dataSnapshot.getValue(Grid::class.java)
//      println(grid)
//      callback.invoke()
//    }
//
//    override fun onCancelled(databaseError: DatabaseError) {
//      println("The read failed: " + databaseError.code)
//    }
//  })
//
////  gridRef.addChildEventListener(object : ChildEventListener {
////    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
////      val grid = snapshot.getValue(Grid::class.java)
////      println(grid)
////      callback.invoke()
////    }
////
////    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
////    }
////
////    override fun onChildRemoved(snapshot: DataSnapshot) {
////    }
////
////    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
////    }
////
////    override fun onCancelled(error: DatabaseError?) {
////      println("The read failed: " + error?.code)
////    }
////  })
//}
//
//fun writeTrailsToDatabase(database: FirebaseDatabase, trails: Map<String, Trail>, grid: Grid) {
//  val ref: DatabaseReference = database.getReference("trails")
//
//  val done = AtomicInteger(2)
//
//  val metadataRef = ref.child("metadata")
//  val metadata = trails.map { it.key to it.value.metadata }.subList(0, 1)
//  metadataRef.setValue(metadata) { _, _ ->
//    done.decrementAndGet()
//  }
//
//  val locationsRef = ref.child("locations")
//  val locations = trails.map { it.key to it.value.locations }.subList(0, 1)
//  locationsRef.setValue(locations) { _, _ ->
//    done.decrementAndGet()
//  }
//
////  val gridRef = ref.child("grid")
////  gridRef.setValue(grid) { _, _ ->
////    done.decrementAndGet()
////  }
//
//  while (done.get() > 0) {
//    Thread.sleep(100)
//  }
//}
