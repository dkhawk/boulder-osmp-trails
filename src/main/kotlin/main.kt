import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.sphericalchickens.osmptrailchallenge.loaders.GpsLoaderFactory
import java.io.File
import java.io.FileInputStream
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis


fun main(args: Array<String>) {
  println("Hello World!")

//  val trailDataFileName = "/Users/dkhawk/Downloads/OSMP_Trails.kml"
  val trailDataFileName = "/Users/dkhawk/Downloads/OSMP_Trails_truncated.kml"
  val runFileName = "/Users/dkhawk/Downloads/GreenMountainCrownRock.gpx"

  val loader = GpsLoaderFactory()
  val gpxLoader = loader.getLoaderByExtention("gpx")

  val updateTrails = true
  val serviceAccount = FileInputStream("/Users/dkhawk/Downloads/osmp-trail-challenge-firebase-adminsdk-te5an-d35f4253d6.json")

  val options: FirebaseOptions = FirebaseOptions.Builder()
    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
    .setDatabaseUrl("https://osmp-trail-challenge-default-rtdb.firebaseio.com/")
    .build()

  FirebaseApp.initializeApp(options)

  val database = FirebaseDatabase.getInstance()

  if (updateTrails) {
    val kmlLoader = loader.getLoaderByExtention("kml")
//    val time = measureTimeMillis {
    val trailsData = kmlLoader.load(File(trailDataFileName).inputStream())
    println("Loaded ${trailsData.segments.size} trails")
    val trails = trailsData.segments.map { Trail.from(it) }
    println(trails[0])
//    }
//    println("millis: $time")
    //  println(trailsData.segments.joinToString("\n") { "${it.name}: ${it.getSummary()}" })
    // TODO(dkhawk): write the trails data to something less horribly slow to load
  }

//  var ref: DatabaseReference? = database.getReference("server/saving-data/fireblog")
//
//  val usersRef = ref!!.child("users")
//
//  val users: MutableMap<String, User> = HashMap<String, User>()
//  users["alanisawesome"] = User(date_of_birth = "June 23, 1912", full_name = "Alan Turing")
//  users["gracehop"] = User(date_of_birth = "December 9, 1906", full_name = "Grace Hopper")
//
//  val done = AtomicBoolean(false)
//  usersRef.setValue(users) { _, _ ->
//    done.set(true)
//  }
//
//  while (!done.get()) {
//    Thread.sleep(100)
//  }

//
//  val activity = gpxLoader.load(File(runFileName).inputStream())
//  println(activity.segments.first().locations.size)

  // Now calculate the bounds of the trails
}

data class User(
  var date_of_birth: String? = null,
  var full_name: String? = null,
  var nickname: String? = null
)
