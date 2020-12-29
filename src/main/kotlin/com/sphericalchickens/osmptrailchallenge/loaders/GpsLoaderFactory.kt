package com.sphericalchickens.osmptrailchallenge.loaders

class GpsLoaderFactory {
    companion object {
        private const val KML_EXTENSION = "kml"
        private const val GPX_EXTENSION = "gpx"

        private const val KML_MIME_TYPE = "application/vnd.google-earth.kml+xml"
        private const val GPX_MIME_TYPE = "application/gpx+xml"
        private const val GENERIC_XML_TYPE = "text/xml"
    }

    private val gpxLoader by lazy {
        GpxLoader()
    }

    private val kmlLoader by lazy {
        KmlLoader()
    }

    fun getLoaderByExtension(extension: String) : GpsLoader {
        return when(extension) {
            KML_EXTENSION -> kmlLoader
            GPX_EXTENSION -> gpxLoader
            else -> throw UnsupportedFileFormat()
        }
    }

    class UnsupportedFileFormat : Throwable()
}
