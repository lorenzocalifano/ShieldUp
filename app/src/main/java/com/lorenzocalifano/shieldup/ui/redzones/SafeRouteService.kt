package com.lorenzocalifano.shieldup.ui.redzones

import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

data class SafeRouteResult(
    val points: List<LatLng>,
    val dangerousIntersections: Int,
    val distanceMeters: Double
)

class SafeRouteService {

    fun calculateWalkingRoutes(
        apiKey: String,
        origin: LatLng,
        destinationAddress: String,
        waypoints: List<LatLng> = emptyList(),
        onSuccess: (List<List<LatLng>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        thread {
            try {
                val originParam = "${origin.latitude},${origin.longitude}"

                val url = buildString {
                    append("https://maps.googleapis.com/maps/api/directions/json?")
                    append("origin=${URLEncoder.encode(originParam, "UTF-8")}")
                    append("&destination=${URLEncoder.encode(destinationAddress, "UTF-8")}")
                    append("&mode=walking")
                    append("&alternatives=true")

                    if (waypoints.isNotEmpty()) {
                        val waypointText = waypoints.joinToString("|") {
                            "${it.latitude},${it.longitude}"
                        }
                        append("&waypoints=${URLEncoder.encode(waypointText, "UTF-8")}")
                    }

                    append("&key=${URLEncoder.encode(apiKey, "UTF-8")}")
                }

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val response = if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }

                val json = JSONObject(response)
                val status = json.optString("status")

                if (status != "OK") {
                    val errorMessage = json.optString("error_message")
                    throw Exception(
                        if (errorMessage.isNotBlank()) {
                            "Directions API: $status - $errorMessage"
                        } else {
                            "Directions API: $status"
                        }
                    )
                }

                val routesJson = json.getJSONArray("routes")
                val routes = mutableListOf<List<LatLng>>()

                for (i in 0 until routesJson.length()) {
                    val points = routesJson
                        .getJSONObject(i)
                        .getJSONObject("overview_polyline")
                        .getString("points")

                    routes.add(PolylineDecoder.decode(points))
                }

                onSuccess(routes)
            } catch (exception: Exception) {
                onError(exception)
            }
        }
    }
}