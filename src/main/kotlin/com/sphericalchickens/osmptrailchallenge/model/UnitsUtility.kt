package com.sphericalchickens.osmptrailchallenge.model

class UnitsUtility {

    companion object {
        fun milesToMeters(miles : Int) : Double {
            return milesToMeters(miles.toDouble())
        }

        fun milesToMeters(miles : Double) : Double {
            return miles * 1609.344
        }

        fun feetToMeters(feet: Int): Double {
            return feet * 0.3048
        }

        fun metersToMiles(meters: Int): Double {
            return metersToMiles(meters.toDouble())
        }

        fun metersToFeet(meters: Int): Double {
            return metersToFeet(meters.toDouble())
        }

        fun metersToMiles(meters: Double): Double {
            return meters * 0.000621371
        }

        fun metersToFeet(meters: Double): Double {
            return meters * 3.28084
        }
    }
}
