package com.sphericalchickens.osmptrailchallenge.loaders

class GpsLoaderFactory {
    companion object {
        private val KML_EXTENSION = "kml"
        private val GPX_EXTENSION = "gpx"

        private val KML_MIME_TYPE = "application/vnd.google-earth.kml+xml"
        private val GPX_MIME_TYPE = "application/gpx+xml"
        private val GENERIC_XML_TYPE = "text/xml"
    }

    val gpxLoader by lazy {
        GpxLoader()
    }

    val kmlLoader by lazy {
        KmlLoader()
    }

    fun getLoaderByExtention(extension: String) : GpsLoader {
        return when(extension) {
            KML_EXTENSION -> kmlLoader
            GPX_EXTENSION -> gpxLoader
            else -> throw UnsupportedFileFormat()
        }
    }

    class UnsupportedFileFormat : Throwable()
}
