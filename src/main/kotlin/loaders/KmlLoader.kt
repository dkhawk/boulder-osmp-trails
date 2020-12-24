package com.sphericalchickens.osmptrailchallenge.loaders

import com.sphericalchickens.osmptrailchallenge.model.Location
import com.sphericalchickens.osmptrailchallenge.model.Segment
import com.sphericalchickens.osmptrailchallenge.model.Waypoint
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.InputStream
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

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
        val waypoints = parseWaypoints(doc)

        return LoaderResult(segments, waypoints)
    }

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

    private fun parseSegments(doc: Document): ArrayList<Segment> {
        val segments = ArrayList<Segment>()
        val segmentNodes = doc.getElementsByTagName("LineString")

        for (i in 0 until segmentNodes.length) {
            // find the line segments
            val trackNode = segmentNodes.item(i)
            val children = trackNode.childNodes
            for (j in 0 until children.length) {
                val coords = children.item(j)
                if ("coordinates" == coords.nodeName) {
                    val coordsString = coords.textContent
                    if (coordsString != null) {
                        //                            if (mergeTracks) {
                        //                                parseCoordsString(coordsString!!, locationListBuilder)
                        //                            } else {
                        segments.add(
                            parseCoordsString(
                                coordsString
                            )
                        )
                        //                            }
                    }
                }
            }
        }

        return segments
    }

    private fun parseCoordsString(coordsString: String): Segment {
        val segment = Segment()
        coordsString.split("[\\s+]".toRegex()).forEach { line ->
            val parts = line.split(",")
            if (parts.size >= 2) {
                val (lng, lat, el) = parts.map {
                    it.toDoubleOrNull()
                }
                if (lng != null && lat != null) {
                    segment.locations.add(Location(lat, lng, el))
                }
            }
        }
        return segment
    }
}

