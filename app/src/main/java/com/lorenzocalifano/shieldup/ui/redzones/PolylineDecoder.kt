package com.lorenzocalifano.shieldup.ui.redzones

import com.google.android.gms.maps.model.LatLng

object PolylineDecoder {

    fun decode(encoded: String): List<LatLng> {
        val polyline = mutableListOf<LatLng>()
        var index = 0
        var latitude = 0
        var longitude = 0

        while (index < encoded.length) {
            var result = 0
            var shift = 0
            var b: Int

            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)

            latitude += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            result = 0
            shift = 0

            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)

            longitude += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            polyline.add(LatLng(latitude / 100000.0, longitude / 100000.0))
        }

        return polyline
    }
}