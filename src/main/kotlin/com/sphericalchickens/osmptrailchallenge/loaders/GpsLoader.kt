package com.sphericalchickens.osmptrailchallenge.loaders

import com.sphericalchickens.osmptrailchallenge.model.Segment
import java.io.InputStream

interface GpsLoader {
    fun load(inputStream: InputStream): LoaderResult
}

data class LoaderResult(
    val segments: List<Segment>,
    val attributes: Map<String, Any> = emptyMap()
)
