package com.sphericalchickens.osmptrailchallenge.loaders

import com.sphericalchickens.osmptrailchallenge.model.Location
import com.sphericalchickens.osmptrailchallenge.model.Segment
import com.sphericalchickens.osmptrailchallenge.model.UnitsUtility
import java.io.InputStream
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.math.roundToInt
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList


class KmlLoader : GpsLoader {
    companion object {
        private val xPath: XPath = XPathFactory.newInstance().newXPath()

        @Throws(ParserConfigurationException::class)
        private fun getXmlFactory(): DocumentBuilder {
            val docBuilderFactory = DocumentBuilderFactory
                .newInstance()
            val docBuilder: DocumentBuilder
            docBuilder = docBuilderFactory.newDocumentBuilder()
            return docBuilder
        }
    }

    override fun load(inputStream: InputStream): LoaderResult {
        val docBuilder = getXmlFactory()
        val doc = docBuilder.parse(inputStream) ?: throw GpxLoader.InvalidGpxFile()

        return readKmlDoc(doc)
    }

    private fun readKmlDoc(doc: Document): LoaderResult {
        // normalize text representation
        doc.documentElement.normalize()

        val segments = parseSegments(doc)
        // val waypoints = parseWaypoints(doc)

        return LoaderResult(segments)
    }

    /*
    private fun parseWaypoints(doc: Document): ArrayList<Waypoint> {
        val waypoints = ArrayList<Waypoint>()

        val waypointList = xPath.compile("//Placemark/Point").evaluate(doc, XPathConstants.NODESET) as NodeList

        for (i in 0 until waypointList.length) {
            val waypoint = Waypoint()
            val waypointNode = waypointList.item(i)
            var valid = false

            // Get the coordinates
            for (j in 0 until waypointNode.childNodes.length) {
                val childNode = waypointNode.childNodes.item(j)
                if ("coordinates" == childNode.nodeName) {
                    childNode.textContent?.let { line ->
                        val parts = line.split(",")
                        if (parts.size >= 2) {
                            val (lng, lat, el) = parts.map {
                                it.toDoubleOrNull()
                            }
                            if (lng != null && lat != null) {
                                waypoint.location = Location(lat, lng, el)
                                valid = true
                            }
                        }
                    }
                }
            }

            // Get the name
            val placemarkChildren = waypointNode.parentNode.childNodes
            for (j in 0 until placemarkChildren.length) {
                val childNode = placemarkChildren.item(j)
                when (childNode.nodeName.toLowerCase()) {
                    "name" -> waypoint.name = childNode.textContent
                    "snippet" -> waypoint.snippet = childNode.textContent
                    "description" -> waypoint.description = childNode.textContent
                }
            }

            if (valid) {
                waypoints.add(waypoint)
            }
        }

//            val waypointNodes = doc.getElementsByTagName("Point")
        return waypoints
    }
    */

    private fun parseSegments(doc: Document): List<Segment> {
        val segments = ArrayList<Segment>()

        val placemarkNodes = doc.getElementsByTagName("Placemark")

        val expression = "ExtendedData/SchemaData/SimpleData"
        val nameExpression = "ExtendedData/SchemaData/SimpleData[@name=\"GISPROD3OSMPTrailsOSMPTRAILNAME\"] " +
          "| ExtendedData/SchemaData/SimpleData[@name=\"SHAPESTLength\"]" +
          "| ExtendedData/SchemaData/SimpleData[@name=\"GISPROD3OSMPTrailsOSMPTRLID\"]"
        val coordinatesExpression = "LineString/coordinates"

        /*
         <ExtendedData><SchemaData schemaUrl="#OSMP_Trails">
                <SimpleData name="GISPROD3OSMPTrailsOSMPOBJECTID">645</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPOWNER">OSMP</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPBICYCLES">No</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPTRAILTYPE">Hiking Trail</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPSEGMENTID">245-059-060</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPHORSES">Yes</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPTRLID">245</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPMILEAGE">0.445</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPMEASUREDFEET">2350</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPTRAILNAME">Enchanted Mesa</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailsOSMPGlobalID">{2F11D894-7828-4B43-B112-6CBCC763BA8A}</SimpleData>
                <SimpleData name="SHAPESTLength">2348.51328473099</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresOBJECTID">467</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresTRAILSTATUS">Open</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresCONTACT">https://bouldercolorado.gov/osmp/temporary-closures</SimpleData>
                <SimpleData name="GISPROD3OSMPTrailClosuresGLOBALID">{DAA4339D-72A8-4275-B461-9DBFFC7B72EF}</SimpleData>
        </SchemaData></ExtendedData>
         */

        val xPathCompiled = xPath.compile(nameExpression)
        return (0 until placemarkNodes.length).map { i ->
//        val segs = (0 until 10).map { i ->
            val placemarkNode = placemarkNodes.item(i)
//            val nodeList =
//                xPath.compile(expression).evaluate(placemarkNode, XPathConstants.NODESET) as NodeList
//            println(nodeList.length)
//            nodeList.item(0).attributes

            val nodeList = xPathCompiled.evaluate(placemarkNode, XPathConstants.NODESET) as NodeList
            val attributes = (0 until nodeList.length).mapNotNull { nodeIndex ->
                val item = nodeList.item(nodeIndex)
                item.attributes.getNamedItem("name").textContent to item.textContent
            }.toMap()

            val coordsNode = xPath.compile(coordinatesExpression).evaluate(placemarkNode, XPathConstants.NODE) as Node
            val coordsString = coordsNode.textContent

            val locations = if (coordsString != null) {
                parseCoordsString(
                    coordsString
                )
            } else {
                null
            }
            attributes to locations
        }.map { (attributes, locations) ->
            val segment = Segment()
            segment.name = attributes["GISPROD3OSMPTrailsOSMPTRAILNAME"] ?: "unknown"
            segment.locations.addAll(locations!!)
            segment.length = UnitsUtility.feetToMeters(attributes["SHAPESTLength"]?.toDouble() ?: 0.0).roundToInt()
            segment
        }

//        println(segs.subList(0, 3).joinToString("\n"))

//        return segs.map { (attributes, locations) ->
//            val segment = Segment()
//            segment.name = attributes["GISPROD3OSMPTrailsOSMPTRAILNAME"] ?: "unknown"
//            segment.locations.addAll(locations!!)
//            segment.length = UnitsUtility.feetToMeters(attributes["SHAPESTLength"]?.toDouble() ?: 0.0).roundToInt()
//            segment
//        }
    }

    private fun parseCoordsString(coordsString: String): MutableList<Location> {
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
        return locations
    }
}

