package com.sphericalchickens.osmptrailchallenge.model

import java.util.*
import kotlin.math.*

class Segment {
    fun getSummary(): String {
        if (summaryString == null) {
            val dist = UnitsUtility.metersToMiles(length)
            val distString = String.format(Locale.getDefault(), "%.02f", dist)

            val gain = UnitsUtility.metersToFeet(gain).roundToInt()
            val loss = UnitsUtility.metersToFeet(loss).roundToInt()
            summaryString = "$distString miles (+$gain/-$loss feet)"
        }

        return summaryString!!
    }

    var trailId: String = ""

    var segmentId: String = ""

    var name: String = ""
    var description: String = ""

    var locations = mutableListOf<Location>()

    // Length in meters
    var length: Int = 0

    // Gain in meters
    private var gain: Int = 0

    // Loss in meters
    private var loss: Int = 0

    var minElevation: Int = Int.MAX_VALUE
    var maxElevation: Int = Int.MIN_VALUE

    val bounds: LatLngBounds by lazy {
        LatLngBounds.createFromLocations(locations)
    }

    var initialized = false

    var orderNumber: Int? = 0

    private var summaryString: String? = null

//    fun addToBounds(llBuilder: LatLngBounds.Builder) {
//        llBuilder.include(bounds.northeast)
//        llBuilder.include(bounds.southwest)
//    }

    /*
    fun calculateStats(force: Boolean = false) {
        if (initialized && !force) {
            return
        }

        if (locations.isEmpty()) {
            initialized = true
            bounds = LatLngBounds(LatLng(0.0, 0.0), LatLng(0.0, 0.0))
            return
        }

        val boundsBuilder = LatLngBounds.builder()
        var gain = 0.0
        var loss = 0.0
        var len = 0.0

        boundsBuilder.include(locations[0].toLatLng())
        locations[0].elevation?.let {
            val elevation = it.toInt()
            minElevation = minElevation.coerceAtMost(elevation)
            maxElevation = maxElevation.coerceAtLeast(elevation)
        }

        locationStats.ensureCapacity(locations.size)
        locationStats.add(LocationStats(0.0, 0.0, 0.0))

        for (i in 1 until locations.size) {
            val previous = locations[i - 1]
            val location = locations[i]
            boundsBuilder.include(location.toLatLng())

            location.elevation?.let {
                val elevation = it.toInt()
                minElevation = minElevation.coerceAtMost(elevation)
                maxElevation = maxElevation.coerceAtLeast(elevation)
                if (previous.elevation != null) {
                    val delta = location.elevation - previous.elevation
                    if (delta > 0) {
                        gain += delta
                    } else {
                        loss += -delta
                    }
                }
            }

            val distance = SphericalUtil.computeDistanceBetween(previous.toLatLng(), location.toLatLng())
            len += distance
            locationStats.add(LocationStats(len, gain, loss))
        }
        // Do not calculate bounds if there are no locations
        this.bounds = boundsBuilder.build()

        this.loss = loss.roundToInt()
        this.gain = gain.roundToInt()
        this.length = len.roundToInt()
        this.initialized = true
    }
     */

    /*
    fun splitAtLocation(splitLocation: Location): ArrayList<Int> {
        var inGroup = false
        var minDist = Double.MAX_VALUE
        var minIndex = 0

        val splitLatLng = splitLocation.toLatLng()

        val breakIndices = ArrayList<Int>()

        for ((locationIndex, location) in locations.withIndex()) {
            val dist = SphericalUtil.computeDistanceBetween(splitLatLng, location.toLatLng())

            if (dist < RouteAndElements.SPLIT_GROUP_MIN && !inGroup) {
                inGroup = true
                minDist = dist
                minIndex = locationIndex
                continue
            }

            if (dist > RouteAndElements.SPLIT_GROUP_MAX && inGroup) {
                inGroup = false
                // Break at min
                breakIndices.add(minIndex)
                continue
            }

            if (inGroup) {
                if (dist < minDist) {
                    minDist = dist
                    minIndex = locationIndex
                }
            }
        }

        // This is a bit comp heavy... :-/
        breakIndices.removeIf { breakIndex ->
            SphericalUtil.computeLength(locations.slice(0 until breakIndex).map { it.toLatLng() }) < RouteAndElements.MIN_DISTANCE_TO_SPLIT ||
                    SphericalUtil.computeLength(locations.slice(breakIndex until locations.size).map { it.toLatLng() }) < RouteAndElements.MIN_DISTANCE_TO_SPLIT
        }

        return breakIndices
    }

     */

    /** TODO(dkhawk): should this be destructive or not? */
    // This is all the decimation code.  Probably don't need it for this application.
    /*
    fun decimate(maxError: Double) {
        Log.i("Segment", "Starting with ${locations.size} locations")
        val firstIndex = 0
        val lastIndex = locations.size - 1

        val pathTree = PathTree(firstIndex, lastIndex)
        calculatePathData(locations, pathTree)

        while (pathTree.percentError > maxError) {
            decimatePath(locations, pathTree, maxError)
        }

        val decimatedPath = ArrayList<Location>()
        pathTreeToLocations(pathTree, decimatedPath)

        locations = decimatedPath

        Log.i("Segment", "Finished with ${decimatedPath.size} locations")
    }
    */

    /*
    private fun pathTreeToLocations(pathTree: PathTree, decimatedPath: ArrayList<Location>) {
        if (pathTree.left != null) {
            pathTreeToLocations(pathTree.left!!, decimatedPath)
        } else {
            decimatedPath.add(locations[pathTree.indexLeft])
        }

        if (pathTree.right != null) {
            pathTreeToLocations(pathTree.right!!, decimatedPath)
        } else {
            decimatedPath.add(locations[pathTree.indexRight])
        }
    }
     */

    /*
    private fun calculatePathData(myLocations: ArrayList<Location>, pathMap: PathTree) {
        pathMap.realDistance = SphericalUtil.computeLength(
            myLocations.subList(
                pathMap.indexLeft,
                pathMap.indexRight + 1
            ).map { it.toLatLng() }
        )
        pathMap.length = SphericalUtil.computeDistanceBetween(
            myLocations[pathMap.indexLeft].toLatLng(),
            myLocations[pathMap.indexRight].toLatLng()
        )
        pathMap.error = pathMap.realDistance - pathMap.length
        pathMap.percentError = abs(pathMap.error / pathMap.realDistance)
        pathMap.maxError = pathMap.error
    }
     */

    /*
    private fun decimatePath(locations: ArrayList<Location>, pathTree: PathTree, maxError: Double) {
        if (pathTree.left == null || pathTree.right == null) {
            splitPath(locations, pathTree)
        } else {
            if (pathTree.left!!.maxError > pathTree.right!!.maxError) {
                decimatePath(locations, pathTree.left!!, maxError)
            } else {
                decimatePath(locations, pathTree.right!!, maxError)
            }
        }

        pathTree.error = pathTree.left!!.error + pathTree.right!!.error
        pathTree.maxError = max(pathTree.left!!.maxError, pathTree.right!!.maxError)
        pathTree.length = pathTree.left!!.length + pathTree.right!!.length
        pathTree.realDistance = pathTree.left!!.realDistance + pathTree.right!!.realDistance
        pathTree.percentError = abs(pathTree.error / pathTree.realDistance)
    }

     */

    /*
    private fun splitPath(locations: ArrayList<Location>, pathMap: PathTree) {
        val result = findLargestError(pathMap, locations)
        val largestErrorIndex = result.first
        // Now split the path at the index of the largest error...

        val left = PathTree(pathMap.indexLeft, largestErrorIndex)
        calculatePathData(locations, left)

        val right = PathTree(largestErrorIndex, pathMap.indexRight)
        calculatePathData(locations, right)

        pathMap.left = left
        pathMap.right = right

        pathMap.error = left.error + right.error
        pathMap.realDistance = left.realDistance + right.realDistance
        pathMap.length = left.length + right.length
        pathMap.percentError = pathMap.error / pathMap.realDistance
    }

     */

    /*
    private fun findLargestError(pathMap: PathTree, locations: java.util.ArrayList<Location>):
            Pair<Int, Double> {
        var furthestDistance = 0.0
        var furthestIndex = -1

        val firstPoint = locations[pathMap.indexLeft]
        val lastPoint = locations[pathMap.indexRight]

        val base = SphericalUtil.computeDistanceBetween(firstPoint.toLatLng(), lastPoint.toLatLng())

        for (i in pathMap.indexLeft + 1 until pathMap.indexRight) {
            val peak = locations[i]
            val side1 = SphericalUtil.computeDistanceBetween(firstPoint.toLatLng(), peak.toLatLng())
            val side2 = SphericalUtil.computeDistanceBetween(lastPoint.toLatLng(), peak.toLatLng())

            val distance = if (side1 * side1 + side2 * side2 > base * base) {
                val perimeter = base + side1 + side2
                val p = perimeter / 2.0
                val area = sqrt(p * (p - base) * (p - side1) * (p - side2))
                2.0 * (area / base)
            } else {
                min(side1, side2)
            }

            if (distance > furthestDistance) {
                furthestDistance = distance
                furthestIndex = i
            }
        }

        return Pair(furthestIndex, furthestDistance)
    }

     */
}

/*
data class PathTree(
    var indexLeft: Int,
    var indexRight: Int
) {
    var maxError: Double = 0.0
    var error: Double = 0.0
    var length: Double = 0.0
    var realDistance: Double = 0.0
    var percentError: Double = 0.0
    var left: PathTree? = null
    var right: PathTree? = null
}
 */
