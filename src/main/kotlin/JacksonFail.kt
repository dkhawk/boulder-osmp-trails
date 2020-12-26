import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sphericalchickens.osmptrailchallenge.model.Location

val osmpTrailExample = """<?xml version="1.0" encoding="utf-8" ?>
<kml xmlns="http://www.opengis.net/kml/2.2">
<Document id="root_doc">
<Folder><name>OSMP_Trails</name>
  <Placemark>
	<Style><LineStyle><color>ff0000ff</color></LineStyle><PolyStyle><fill>0</fill></PolyStyle></Style>
	<ExtendedData><SchemaData schemaUrl="#OSMP_Trails">
		<SimpleData name="GISPROD3OSMPTrailsOSMPOBJECTID">1279</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPOWNER">OSMP</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPBICYCLES">No</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPTRAILTYPE">Hiking Trail</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPSEGMENTID">201-128-129</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPHORSES">Yes</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPTRLID">201</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPMILEAGE">0.011</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPTRAILNAME">Mesa</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPGlobalID">{C8B68379-4A18-4963-AD7B-7321808BED82}</SimpleData>
		<SimpleData name="SHAPESTLength">59.3034651783743</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailClosuresOBJECTID">578</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailClosuresTRAILSTATUS">Open</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailClosuresCONTACT">https://bouldercolorado.gov/osmp/temporary-closures</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailClosuresGLOBALID">{F906AA24-AE53-4057-A68A-597C255F1168}</SimpleData>
	</SchemaData></ExtendedData>
      <LineString><coordinates>-105.285870911105,39.9855716765493 -105.285867975575,39.9855572056917 -105.285866153371,39.9855457640004 -105.285870529031,39.98553338093 -105.285873039427,39.9855229001786 -105.285881751521,39.9855110016086 -105.285897306275,39.9854900596705 -105.285956375212,39.9854299372237</coordinates></LineString>
  </Placemark>
  <Placemark>
        <Style><LineStyle><color>ff0000ff</color></LineStyle><PolyStyle><fill>0</fill></PolyStyle></Style>
        <ExtendedData><SchemaData schemaUrl="#OSMP_Trails">
                <SimpleData name="GISPROD3OSMPTrailsOSMPOBJECTID">1280</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPOWNER">NIST</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPBICYCLES">No</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPTRAILTYPE">Hiking Trail</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPSEGMENTID">336-089-092</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPHORSES">No</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPTRLID">336</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPMILEAGE">0.048</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPTRAILNAME">NIST Service Rd Connector</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPGlobalID">{389E83A0-9DCF-44F3-93F2-7DBEDD2B1EC1}</SimpleData>
                <SimpleData name="SHAPESTLength">251.335645037628</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresOBJECTID">565</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresTRAILSTATUS">Open</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresCONTACT">https://bouldercolorado.gov/osmp/temporary-closures</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresGLOBALID">{59EE28EB-D459-470D-89E4-365B93F558BC}</SimpleData>
        </SchemaData></ExtendedData>
      <LineString><coordinates>-105.271429163898,39.9899444866293 -105.271414847951,39.9899589128412 -105.271380065054,39.9899857184842 -105.271364005822,39.9899965836492 -105.271350732583,39.9900034905592 -105.271
337544305,39.9900088043791 -105.271308950775,39.9900192087576 -105.271267454579,39.9900321552945 -105.271234937006,39.9900433417425 -105.271208477467,39.9900521678817 -105.271182624854,39.9900601774254 -105.2711671
34054,39.9900656325617 -105.271139356068,39.9900772435922 -105.271091478926,39.9900990758601 -105.271056223179,39.9901139017678 -105.271023010623,39.9901269970632 -105.270989536027,39.9901407132798 -105.27095072090
8,39.990154762926 -105.270910987251,39.9901721053263 -105.270890924793,39.9901804514468 -105.270866034882,39.9901901164337 -105.270853400719,39.9901946306181 -105.270847105811,39.9901963869903 -105.27083791161,39.9
901981718238 -105.270825906682,39.9901994142793 -105.270813732665,39.9901997530413 -105.270768749111,39.990197764125 -105.270730866786,39.9901951975843 -105.270707935424,39.9901952204273 -105.270652777748,39.990196
7165752 -105.270626433216,39.9902050780722</coordinates></LineString>
  </Placemark>
</Folder>
</Document>
</kml>
"""

val osmpTrailExample2 = """<?xml version="1.0" encoding="utf-8" ?>
<kml xmlns="http://www.opengis.net/kml/2.2">
<Document id="root_doc">
<Folder><name>OSMP_Trails</name>
  <Placemark>
	<Style><LineStyle><color>ff0000ff</color></LineStyle><PolyStyle><fill>0</fill></PolyStyle></Style>
	<ExtendedData><SchemaData schemaUrl="#OSMP_Trails">
		<SimpleData name="GISPROD3OSMPTrailsOSMPOBJECTID">1279</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPOWNER">OSMP</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPBICYCLES">No</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPTRAILTYPE">Hiking Trail</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPSEGMENTID">201-128-129</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPHORSES">Yes</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPTRLID">201</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPMILEAGE">0.011</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPTRAILNAME">Mesa</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailsOSMPGlobalID">{C8B68379-4A18-4963-AD7B-7321808BED82}</SimpleData>
		<SimpleData name="SHAPESTLength">59.3034651783743</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailClosuresOBJECTID">578</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailClosuresTRAILSTATUS">Open</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailClosuresCONTACT">https://bouldercolorado.gov/osmp/temporary-closures</SimpleData>
		<SimpleData name="GISPROD3OSMPTrailClosuresGLOBALID">{F906AA24-AE53-4057-A68A-597C255F1168}</SimpleData>
	</SchemaData></ExtendedData>
      <LineString><coordinates>-1,2 -3,4 -5,6</coordinates></LineString>
  </Placemark>
  <Placemark>
        <Style><LineStyle><color>ff0000ff</color></LineStyle><PolyStyle><fill>0</fill></PolyStyle></Style>
        <ExtendedData><SchemaData schemaUrl="#OSMP_Trails">
                <SimpleData name="GISPROD3OSMPTrailsOSMPOBJECTID">1280</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPOWNER">NIST</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPBICYCLES">No</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPTRAILTYPE">Hiking Trail</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPSEGMENTID">336-089-092</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPHORSES">No</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPTRLID">336</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPMILEAGE">0.048</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPTRAILNAME">NIST Service Rd Connector</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPGlobalID">{389E83A0-9DCF-44F3-93F2-7DBEDD2B1EC1}</SimpleData>
                <SimpleData name="SHAPESTLength">251.335645037628</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresOBJECTID">565</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresTRAILSTATUS">Open</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresCONTACT">https://bouldercolorado.gov/osmp/temporary-closures</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresGLOBALID">{59EE28EB-D459-470D-89E4-365B93F558BC}</SimpleData>
        </SchemaData></ExtendedData>
      <LineString><coordinates>-7,8 -9,10 -10,12</coordinates></LineString>
  </Placemark>
</Folder>
</Document>
</kml>
"""

class JacksonFail {
  fun wtf() {
    val locations = mutableListOf<Location>()
    locations.add(Location(12.0, 13.0))
    locations.add(Location(14.0, 15.0))
    val polyline = Polyline(locations)

    val folder = Folder()
    folder.trails.add(Trail(LineString(polyline)))
    val kml = Kml(Document(folder))


    val kotlinXmlMapper = XmlMapper()
      .registerModule(KotlinModule())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

    val xmlString: String = kotlinXmlMapper.writeValueAsString(kml)

    // write to the console
    println(xmlString)


//  val trail = kotlinXmlMapper.readValue<Trail>(osmpTrailExample, Trail::class.java)
//  println(trail)

//  val value = kotlinXmlMapper.readValue(osmpTrailExample2, Kml::class.java)
//  println(value)

  }
}


@JsonRootName("kml")
data class Kml(
  var document: Document
)

@JsonRootName("Document")
data class Document(
  var folder: Folder
)

@JsonRootName("Folder")
data class Folder(
//  var name: String? = null,
  @JacksonXmlProperty(localName = "Placemark")
  @JacksonXmlElementWrapper(useWrapping = false)
  var trails: MutableList<Trail> = mutableListOf()
)

@JsonRootName("Placemark")
data class Trail(
//  @JacksonXmlProperty(localName = "ExtendedData")
////  @set:JsonProperty("ExtendedData")
//  var extendedData: ExtendedData? = null,

  @JacksonXmlProperty(localName = "LineString")
//  @set:JsonProperty("LineString")
  var lineString: LineString? = null
)

@JsonRootName("ExtendedData")
data class ExtendedData(
  @JacksonXmlProperty(localName = "SchemaData")
  var schemaData: SchemaData
)

@JsonRootName("SchemaData")
data class SchemaData(
  @JacksonXmlProperty(localName = "SimpleData")
  @JacksonXmlElementWrapper(useWrapping = false)
  var simpleData: List<String>
)

@JsonRootName("LineString")
data class LineString(
  @JacksonXmlProperty(localName = "coordinates")
  var polyline: Polyline
)

class CoordinatesDeserializer : StdDeserializer<Polyline>(Polyline::class.java) {
  override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Polyline {
    val coordsString = jsonParser.text
    val locations = mutableListOf<Location>()
    coordsString.split("[\\s+]".toRegex()).forEach { line ->
      val parts = line.split(",")
      if (parts.size == 2) {
        val (lng, lat) = parts.map {
          it.toDoubleOrNull()
        }
        if (lng != null && lat != null) {
          locations.add(Location(lat, lng))
        }
      } else if (parts.size == 3) {
        val (lng, lat, el) = parts.map {
          it.toDoubleOrNull()
        }
        if (lng != null && lat != null) {
          locations.add(Location(lat, lng, el))
        }
      }
    }
    return Polyline(locations)
  }
}

@JsonDeserialize(using = CoordinatesDeserializer::class)
data class Polyline(
  @JacksonXmlCData
  val locations: List<Location>
)

@JsonDeserialize(using = AttributesDeserializer::class)
class TrailAttributes {
  val attribMap = mutableMapOf<String, String>()
}

class AttributesDeserializer : StdDeserializer<TrailAttributes>(TrailAttributes::class.java) {
  override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): TrailAttributes {
    val trailAttributes = TrailAttributes()
    trailAttributes.attribMap["name"] = "Foo"
    return trailAttributes
  }
}
