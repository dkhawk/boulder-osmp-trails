package com.sphericalchickens.osmptrailchallenge.loaders

import com.sphericalchickens.osmptrailchallenge.model.Location
import com.sphericalchickens.osmptrailchallenge.model.Segment
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants.NODESET
import javax.xml.xpath.XPathFactory

class GpxLoader : GpsLoader {
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
        val doc = docBuilder.parse(inputStream) ?: throw InvalidGpxFile()

        return readGpxDoc(doc)
    }

    private fun readGpxDoc(doc: Document): LoaderResult {
        // normalize text representation
        doc.documentElement.normalize()

        val segments = ArrayList<Segment>()

        val tracksAndRoutesList = xPath.compile("//gpx/trk | //gpx/rte | //gpx/wpt").evaluate(doc, NODESET) as NodeList

        for (i in 0 until tracksAndRoutesList.length) {

            val node = tracksAndRoutesList.item(i) ?: continue

            when (node.nodeName) {
                "trk" -> segments.add(readTrack(node))
                "rte" -> segments.add(readRoute(node))
            }
        }

        return LoaderResult(segments)
    }

    private fun readTrack(trackNode: Node): Segment {
        val segment = Segment()

        val trackPoints = xPath.compile("trkseg/trkpt").evaluate(trackNode, NODESET) as NodeList
        for (j in 0 until trackPoints.length) {
            val point = trackPoints.item(j)
            val lat = point.attributes.getNamedItem("lat").nodeValue.toDouble()
            val lng = point.attributes.getNamedItem("lon").nodeValue.toDouble()
            var ele: Double? = null
//             var timestamp: Calendar? = null
            for (k in 0 until point.childNodes.length) {
                val node = point.childNodes.item(k)
                if (node.nodeName == "ele") {
                    ele = node.textContent.toDouble()
                    break
                }
//                if (false && node.nodeName == "time") {
//                    timestamp = parseTime(node.textContent)
//                }
            }
            val location = Location(lat, lng, ele)
//            if (timestamp != null) {
//                // location.timestamp = timestamp
//            }
            segment.locations.add(location)
        }

        if (trackNode is Element) {
            val nameNodeList = xPath.compile("name").evaluate(trackNode, NODESET) as NodeList
            if (nameNodeList.length > 0) {
                val firstChild = nameNodeList.item(0).firstChild
                if (firstChild != null) {
                    segment.name = firstChild.nodeValue.trim()
                }
            }
        }

        // segment.calculateStats()
        return segment
    }

    fun parseTime(timeString: String): Calendar {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        cal.timeZone = TimeZone.getTimeZone("UTC")

        cal.time = sdf.parse(timeString)

        return cal
    }

    private fun readRoute(routeNode: Node): Segment {
        val segment = Segment()

        val routePoints = xPath.compile("rtept").evaluate(routeNode, NODESET) as NodeList
        for (j in 0 until routePoints.length) {
            val point = routePoints.item(j)
            val lat = point.attributes.getNamedItem("lat").nodeValue.toDouble()
            val lng = point.attributes.getNamedItem("lon").nodeValue.toDouble()
            var ele: Double? = null
            for (k in 0 until point.childNodes.length) {
                val node = point.childNodes.item(k)
                if (node.nodeName == "ele") {
                    ele = node.textContent.toDouble()
                }
            }
            segment.locations.add(Location(lat, lng, ele))
        }

        if (routeNode is Element) {
            val nameNodeList = xPath.compile("name").evaluate(routeNode, NODESET) as NodeList
            if (nameNodeList.length > 0) {
                segment.name = nameNodeList.item(0).firstChild.nodeValue.trim()
            }
        }

        // segment.calculateStats()
        return segment
    }

    class InvalidGpxFile : Throwable()
}
