package com.lorenzocalifano.shieldup.ui.redzones

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.telephony.SmsManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.lorenzocalifano.shieldup.BuildConfig
import com.lorenzocalifano.shieldup.R
import com.lorenzocalifano.shieldup.data.FirebaseRepository
import com.lorenzocalifano.shieldup.data.RedZoneDto
import com.lorenzocalifano.shieldup.utils.SessionManager
import com.lorenzocalifano.shieldup.ui.followme.FollowMeManager
import com.lorenzocalifano.shieldup.ui.followme.FollowMeRepository
import com.lorenzocalifano.shieldup.ui.followme.FollowMeSessionDto

class RedZonesFragment : Fragment(R.layout.fragment_red_zones) {

    private val repository = FirebaseRepository()
    private val safeRouteService = SafeRouteService()

    private var googleMap: GoogleMap? = null
    private var routePolyline: Polyline? = null

    private val activeRedZones = mutableListOf<RedZoneDto>()

    private val anconaCenter = LatLng(43.6158, 13.5189)
    private val redZoneDurationMs = 60L * 60L * 1000L
    private val redZoneRadiusMeters = 160.0

    private var safeRouteMode = false

    private var followMeMode = false

    private lateinit var followMeManager: FollowMeManager
    private val followMeRepository = FollowMeRepository()

    private var currentLiveSessionId: String? = null

    companion object {
        private const val TAG = "SHIELDUP_MAP"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        followMeManager = FollowMeManager(requireContext())
        safeRouteMode = arguments?.getBoolean("safeRouteMode") == true

        setupMap()
        setupButtons(view)

        followMeMode = arguments?.getBoolean("followMeMode") == true

        if (followMeMode) {
            showFollowMeDestinationDialog()
        }

        if (arguments?.getBoolean("openReportDialog") == true) {
            view.postDelayed({
                if (isAdded) {
                    showAddRedZoneDialog()
                    arguments?.remove("openReportDialog")
                }
            }, 500)
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.googleMap) as? SupportMapFragment

        if (mapFragment == null) {
            Toast.makeText(requireContext(), "Errore caricamento mappa", Toast.LENGTH_LONG).show()
            return
        }

        mapFragment.getMapAsync { map ->
            googleMap = map

            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false

            enableUserLocationIfAllowed()
            moveToCurrentLocation()
            loadRedZonesFromFirebase()

            if (safeRouteMode) {
                showDestinationDialog()
            }
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.btnAddRedZone).setOnClickListener {
            showAddRedZoneDialog()
        }

        view.findViewById<Button>(R.id.btnRedZoneList).setOnClickListener {
            findNavController().navigate(R.id.redZoneListFragment)
        }

        view.findViewById<Button>(R.id.btnMyLocation).setOnClickListener {
            moveToCurrentLocation()
        }
    }

    private fun showDestinationDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Inserisci indirizzo di destinazione"
            setSingleLine(false)
            minLines = 1
            maxLines = 3
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Calcola percorso sicuro")
            .setMessage("Inserisci la via o l'indirizzo da raggiungere.")
            .setView(input)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Calcola", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val destinationAddress = input.text.toString().trim()

                        if (destinationAddress.isEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                "Inserisci una destinazione",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        dismiss()
                        calculateSafeRoute(destinationAddress)
                    }
                }
            }
            .show()
    }

    private fun calculateSafeRoute(destinationAddress: String) {
        if (!hasLocationPermission()) {
            Toast.makeText(
                requireContext(),
                "Permesso posizione necessario per calcolare il percorso",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!hasLocationPermission()) {
            return
        }

        try {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireActivity())

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    val origin = if (location != null) {
                        LatLng(location.latitude, location.longitude)
                    } else {
                        googleMap?.cameraPosition?.target ?: anconaCenter
                    }

                    Toast.makeText(requireContext(), "Calcolo percorso sicuro...", Toast.LENGTH_SHORT).show()

                    safeRouteService.calculateWalkingRoutes(
                        apiKey = BuildConfig.MAPS_API_KEY,
                        origin = origin,
                        destinationAddress = destinationAddress,
                        onSuccess = { routes ->
                            requireActivity().runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                chooseAndDrawSafestRoute(
                                    routes = routes,
                                    origin = origin,
                                    destinationAddress = destinationAddress
                                )
                            }
                        },
                        onError = { exception ->
                            requireActivity().runOnUiThread {
                                if (!isAdded) return@runOnUiThread

                                Log.e(TAG, "Errore calcolo percorso sicuro", exception)

                                Toast.makeText(
                                    requireContext(),
                                    exception.message ?: "Errore calcolo percorso",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Errore recupero posizione", exception)

                    Toast.makeText(
                        requireContext(),
                        "Errore recupero posizione",
                        Toast.LENGTH_LONG
                    ).show()
                }

        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Permesso posizione non disponibile",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun chooseAndDrawSafestRoute(
        routes: List<List<LatLng>>,
        origin: LatLng,
        destinationAddress: String
    ) {
        if (routes.isEmpty()) {
            Toast.makeText(requireContext(), "Nessun percorso trovato", Toast.LENGTH_LONG).show()
            return
        }

        val firstEvaluation = routes
            .map {
                RouteSafetyUtils.evaluateRoute(
                    route = it,
                    redZones = activeRedZones,
                    redZoneRadiusMeters = redZoneRadiusMeters
                )
            }
            .sortedWith(
                compareBy<SafeRouteResult> { it.dangerousIntersections }
                    .thenBy { it.distanceMeters }
            )

        val bestInitialRoute = firstEvaluation.first()

        if (bestInitialRoute.dangerousIntersections == 0) {
            drawSafeRoute(bestInitialRoute.points)
            Toast.makeText(requireContext(), "Percorso sicuro trovato", Toast.LENGTH_LONG).show()
            return
        }

        val dangerousZones = RouteSafetyUtils.getIntersectedZones(
            routePoints = bestInitialRoute.points,
            redZones = activeRedZones,
            redZoneRadiusMeters = redZoneRadiusMeters
        ).take(2)

        if (dangerousZones.isEmpty()) {
            drawSafeRoute(bestInitialRoute.points)
            Toast.makeText(requireContext(), "Percorso migliore disponibile", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(requireContext(), "Ricerca deviazione sicura...", Toast.LENGTH_SHORT).show()

        val candidateWaypointSets = mutableListOf<List<LatLng>>()

        dangerousZones.forEach { zone ->
            val center = LatLng(zone.latitude, zone.longitude)
            RouteSafetyUtils.createAvoidanceWaypointsAroundZone(
                zoneCenter = center,
                radiusMeters = redZoneRadiusMeters
            ).forEach { waypoint ->
                candidateWaypointSets.add(listOf(waypoint))
            }
        }

        if (dangerousZones.size >= 2) {
            val firstZoneWaypoints = RouteSafetyUtils.createAvoidanceWaypointsAroundZone(
                zoneCenter = LatLng(dangerousZones[0].latitude, dangerousZones[0].longitude),
                radiusMeters = redZoneRadiusMeters
            )

            val secondZoneWaypoints = RouteSafetyUtils.createAvoidanceWaypointsAroundZone(
                zoneCenter = LatLng(dangerousZones[1].latitude, dangerousZones[1].longitude),
                radiusMeters = redZoneRadiusMeters
            )

            firstZoneWaypoints.take(4).forEach { first ->
                secondZoneWaypoints.take(4).forEach { second ->
                    candidateWaypointSets.add(listOf(first, second))
                }
            }
        }

        calculateCandidateRoutes(
            origin = origin,
            destinationAddress = destinationAddress,
            waypointSets = candidateWaypointSets,
            collectedRoutes = routes.toMutableList(),
            index = 0,
            fallbackRoute = bestInitialRoute.points
        )
    }

    private fun drawSafeRoute(points: List<LatLng>) {
        val map = googleMap ?: return

        routePolyline?.remove()

        routePolyline = map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(10f)
                .color(0xFF2E7D32.toInt())
                .geodesic(true)
        )

        if (points.isNotEmpty()) {
            map.addMarker(
                MarkerOptions()
                    .position(points.last())
                    .title("Destinazione")
            )

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 15f))
        }
    }

    private fun showAddRedZoneDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_red_zone, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.etRedZoneTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.etRedZoneDescription)
        val positionSwitch = dialogView.findViewById<Switch>(R.id.switchCurrentLocation)
        val infoText = dialogView.findViewById<TextView>(R.id.txtLocationInfo)

        positionSwitch.isChecked = true
        infoText.text = "La segnalazione verrà inserita nella tua posizione GPS attuale."

        positionSwitch.setOnCheckedChangeListener { _, isChecked ->
            infoText.text = if (isChecked) {
                "La segnalazione verrà inserita nella tua posizione GPS attuale."
            } else {
                "La segnalazione verrà inserita nel centro della mappa. Sposta la mappa sul punto da segnalare prima di salvare."
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Salva", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()

                if (title.isEmpty() || description.isEmpty()) {
                    Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (positionSwitch.isChecked) {
                    saveUsingCurrentPosition(title, description)
                } else {
                    saveUsingMapCenter(title, description)
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun saveUsingCurrentPosition(title: String, description: String) {
        if (!hasLocationPermission()) {
            Toast.makeText(
                requireContext(),
                "Permesso posizione non concesso. Uso il centro della mappa.",
                Toast.LENGTH_LONG
            ).show()
            saveUsingMapCenter(title, description)
            return
        }

        if (!hasLocationPermission()) {
            return
        }

        try {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireActivity())

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    val position = if (location != null) {
                        LatLng(location.latitude, location.longitude)
                    } else {
                        googleMap?.cameraPosition?.target ?: anconaCenter
                    }

                    saveRedZoneToFirebase(title, description, position)
                }
                .addOnFailureListener {
                    val position = googleMap?.cameraPosition?.target ?: anconaCenter
                    saveRedZoneToFirebase(title, description, position)
                }

        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Permesso posizione non disponibile",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveUsingMapCenter(title: String, description: String) {
        val position = googleMap?.cameraPosition?.target ?: anconaCenter
        saveRedZoneToFirebase(title, description, position)
    }

    private fun saveRedZoneToFirebase(title: String, description: String, position: LatLng) {
        val sessionManager = SessionManager(requireContext())
        val currentUserId = sessionManager.getUserId().ifBlank { "demo_user" }

        val redZone = RedZoneDto(
            title = title,
            description = description,
            latitude = position.latitude,
            longitude = position.longitude,
            createdAt = System.currentTimeMillis(),
            userId = currentUserId,
            type = "Pericolo"
        )

        repository.saveRedZone(
            redZone = redZone,
            onSuccess = {
                if (!isAdded) return@saveRedZone

                Toast.makeText(requireContext(), "Segnalazione salvata", Toast.LENGTH_SHORT).show()
                googleMap?.clear()
                routePolyline = null
                loadRedZonesFromFirebase()
            },
            onError = { exception ->
                if (!isAdded) return@saveRedZone

                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Errore salvataggio Red Zone",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun loadRedZonesFromFirebase() {
        repository.loadRedZones(
            onSuccess = { zones ->
                if (!isAdded || context == null) return@loadRedZones

                val now = System.currentTimeMillis()
                val activeZones = zones.filter { now - it.createdAt < redZoneDurationMs }

                activeRedZones.clear()
                activeRedZones.addAll(activeZones)

                googleMap?.clear()
                routePolyline = null
                enableUserLocationIfAllowed()

                activeZones.forEach { addRedZoneToMap(it) }
            },
            onError = { exception ->
                if (!isAdded || context == null) return@loadRedZones

                Toast.makeText(
                    requireContext(),
                    exception.message ?: "Errore caricamento Red Zones",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun addRedZoneToMap(redZone: RedZoneDto) {
        val map = googleMap ?: return
        val position = LatLng(redZone.latitude, redZone.longitude)

        map.addMarker(
            MarkerOptions()
                .position(position)
                .title(redZone.title)
                .snippet(redZone.description)
        )

        map.addCircle(
            CircleOptions()
                .center(position)
                .radius(redZoneRadiusMeters)
                .strokeColor(0x99FF0000.toInt())
                .fillColor(0x33FF0000)
                .strokeWidth(4f)
        )
    }

    private fun enableUserLocationIfAllowed() {
        if (!isAdded || context == null) return

        if (hasLocationPermission()) {
            try {
                googleMap?.isMyLocationEnabled = true
            } catch (exception: SecurityException) {
                Log.e(TAG, "Errore permesso posizione", exception)
            }
        }
    }

    private fun moveToCurrentLocation() {
        if (!hasLocationPermission()) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f))
            return
        }

        if (!hasLocationPermission()) {
            return
        }

        try {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireActivity())

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (!isAdded) return@addOnSuccessListener

                    if (location != null) {
                        val currentPosition = LatLng(location.latitude, location.longitude)
                        googleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(currentPosition, 16f)
                        )
                    } else {
                        googleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f)
                        )
                    }
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f))
                }

        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Permesso posizione non disponibile",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun hasLocationPermission(): Boolean {
        if (!isAdded || context == null) return false

        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    private fun drawBestRouteAfterDetour(
        detourRoutes: List<List<LatLng>>,
        fallbackRoute: List<LatLng>
    ) {
        if (detourRoutes.isEmpty()) {
            drawSafeRoute(fallbackRoute)
            return
        }

        val evaluatedRoutes = detourRoutes.map { route ->
            SafeRouteResult(
                points = route,
                dangerousIntersections = RouteSafetyUtils.countDangerousIntersections(
                    routePoints = route,
                    redZones = activeRedZones,
                    redZoneRadiusMeters = redZoneRadiusMeters
                ),
                distanceMeters = RouteSafetyUtils.routeLengthMeters(route)
            )
        }

        val bestRoute = evaluatedRoutes
            .sortedWith(
                compareBy<SafeRouteResult> { it.dangerousIntersections }
                    .thenBy { it.distanceMeters }
            )
            .first()

        drawSafeRoute(bestRoute.points)

        val message = if (bestRoute.dangerousIntersections == 0) {
            "Deviazione sicura trovata"
        } else {
            "Percorso migliore trovato, ma vicino a ${bestRoute.dangerousIntersections} zone rosse"
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun calculateCandidateRoutes(
        origin: LatLng,
        destinationAddress: String,
        waypointSets: List<List<LatLng>>,
        collectedRoutes: MutableList<List<LatLng>>,
        index: Int,
        fallbackRoute: List<LatLng>
    ) {
        if (index >= waypointSets.size) {
            drawBestCandidateRoute(
                routes = collectedRoutes,
                fallbackRoute = fallbackRoute
            )
            return
        }

        safeRouteService.calculateWalkingRoutes(
            apiKey = BuildConfig.MAPS_API_KEY,
            origin = origin,
            destinationAddress = destinationAddress,
            waypoints = waypointSets[index],
            onSuccess = { newRoutes ->
                requireActivity().runOnUiThread {
                    if (!isAdded) return@runOnUiThread

                    collectedRoutes.addAll(newRoutes)

                    calculateCandidateRoutes(
                        origin = origin,
                        destinationAddress = destinationAddress,
                        waypointSets = waypointSets,
                        collectedRoutes = collectedRoutes,
                        index = index + 1,
                        fallbackRoute = fallbackRoute
                    )
                }
            },
            onError = {
                requireActivity().runOnUiThread {
                    if (!isAdded) return@runOnUiThread

                    calculateCandidateRoutes(
                        origin = origin,
                        destinationAddress = destinationAddress,
                        waypointSets = waypointSets,
                        collectedRoutes = collectedRoutes,
                        index = index + 1,
                        fallbackRoute = fallbackRoute
                    )
                }
            }
        )
    }

    private fun drawBestCandidateRoute(
        routes: List<List<LatLng>>,
        fallbackRoute: List<LatLng>
    ) {
        val evaluatedRoutes = routes
            .filter { it.size >= 2 }
            .map {
                RouteSafetyUtils.evaluateRoute(
                    route = it,
                    redZones = activeRedZones,
                    redZoneRadiusMeters = redZoneRadiusMeters
                )
            }
            .sortedWith(
                compareBy<SafeRouteResult> { it.dangerousIntersections }
                    .thenBy { it.distanceMeters }
            )

        if (evaluatedRoutes.isEmpty()) {
            drawSafeRoute(fallbackRoute)
            Toast.makeText(requireContext(), "Percorso migliore disponibile", Toast.LENGTH_LONG).show()
            return
        }

        val bestRoute = evaluatedRoutes.first()
        drawSafeRoute(bestRoute.points)

        val message = if (bestRoute.dangerousIntersections == 0) {
            "Deviazione sicura trovata"
        } else {
            "Percorso migliore trovato, ma vicino a ${bestRoute.dangerousIntersections} zone rosse"
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showFollowMeDestinationDialog() {

        val input = EditText(requireContext())

        AlertDialog.Builder(requireContext())
            .setTitle("Seguimi a casa")
            .setMessage("Inserisci la destinazione")
            .setView(input)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Avvia", null)
            .create()
            .also { dialog ->

                dialog.setOnShowListener {

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {

                            val destination = input.text.toString().trim()

                            if (destination.isEmpty()) {
                                input.error = "Inserisci una destinazione"
                                return@setOnClickListener
                            }

                            startFollowMe(destination)

                            dialog.dismiss()
                        }

                }

            }
            .show()

    }

    private fun startFollowMe(destination: String) {

        val session = SessionManager(requireContext())

        val dto = FollowMeSessionDto(
            userId = session.getUserId(),
            userName = session.getName(),
            destination = destination
        )

        followMeRepository.createSession(

            session = dto,
            onSuccess = { sessionId ->

                currentLiveSessionId = sessionId
                followMeManager.startTracking(sessionId)

                val followMeLink = buildFollowMeLink(sessionId)

                Log.d("SHIELDUP_FOLLOWME", "Sessione FollowMe: $sessionId")
                Log.d("SHIELDUP_FOLLOWME", "Link FollowMe: $followMeLink")

                sendFollowMeLinkToEmergencyContacts(followMeLink)

                showStopFollowMeButton()

                requireActivity().runOnUiThread {

                    Toast.makeText(
                        requireContext(),
                        "Condivisione posizione avviata",
                        Toast.LENGTH_LONG
                    ).show()

                    calculateSafeRoute(destination)

                }

            },

            onError = {

                requireActivity().runOnUiThread {

                    Toast.makeText(
                        requireContext(),
                        "Errore avvio Follow Me",
                        Toast.LENGTH_LONG
                    ).show()

                }
            }
        )
    }

    private fun buildFollowMeLink(sessionId: String): String {
        return "https://shieldup-925d1.web.app/followme.html?id=$sessionId"
    }

    private fun sendFollowMeLinkToEmergencyContacts(link: String) {
        val session = SessionManager(requireContext())

        repository.loadEmergencyContacts(
            userId = session.getUserId(),
            onSuccess = { contacts ->
                if (!isAdded) return@loadEmergencyContacts

                if (contacts.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Nessun contatto di emergenza salvato",
                        Toast.LENGTH_LONG
                    ).show()

                    Log.d("SHIELDUP_FOLLOWME", "Nessun contatto. Link: $link")
                    return@loadEmergencyContacts
                }

                val message = "${session.getName()} ha avviato Seguimi a casa.\n\nLink sessione:\n$link"

                contacts.forEach { contact ->
                    sendSmsToContact(contact.phone, message)
                }

                Toast.makeText(
                    requireContext(),
                    "Link FollowMe inviato ai contatti",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("SHIELDUP_FOLLOWME", "Messaggio inviato/loggato: $message")
            },
            onError = { exception ->
                if (!isAdded) return@loadEmergencyContacts

                Toast.makeText(
                    requireContext(),
                    "Errore caricamento contatti emergenza",
                    Toast.LENGTH_LONG
                ).show()

                Log.e("SHIELDUP_FOLLOWME", "Errore contatti", exception)
                Log.d("SHIELDUP_FOLLOWME", "Link FollowMe: $link")
            }
        )
    }

    private fun sendSmsToContact(phone: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)

            smsManager.sendMultipartTextMessage(
                phone,
                null,
                parts,
                null,
                null
            )

            Log.d("SHIELDUP_FOLLOWME", "SMS inviato a $phone")
        } catch (exception: Exception) {
            Log.e("SHIELDUP_FOLLOWME", "SMS non inviato a $phone", exception)
            Log.d("SHIELDUP_FOLLOWME", "Messaggio per $phone: $message")
        }
    }

    private fun showStopFollowMeButton() {
        val root = view as? FrameLayout ?: return

        if (root.findViewWithTag<Button>("STOP_FOLLOW_ME_BUTTON") != null) return

        val button = Button(requireContext()).apply {
            tag = "STOP_FOLLOW_ME_BUTTON"
            text = "Termina condivisione"
            setTextColor(resources.getColor(R.color.white, null))
            setBackgroundResource(R.drawable.bg_red_button)

            setOnClickListener {
                stopFollowMe()
                root.removeView(this)
            }
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            60.dp()
        ).apply {
            leftMargin = 22.dp()
            rightMargin = 22.dp()
            bottomMargin = 92.dp()
            gravity = android.view.Gravity.BOTTOM
        }

        root.addView(button, params)
    }

    private fun stopFollowMe() {
        followMeManager.stopTracking()

        currentLiveSessionId?.let { sessionId ->
            followMeRepository.stopSession(sessionId)
            Log.d("SHIELDUP_FOLLOWME", "Sessione FollowMe terminata: $sessionId")
        }

        currentLiveSessionId = null

        Toast.makeText(
            requireContext(),
            "Condivisione posizione terminata",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        currentLiveSessionId?.let { sessionId ->
            followMeRepository.stopSession(sessionId)
        }

        followMeManager.stopTracking()

        super.onDestroyView()
    }
}