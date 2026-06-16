package com.lorenzocalifano.shieldup.ui.redzones

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.lorenzocalifano.shieldup.R

class RedZonesFragment : Fragment(R.layout.fragment_red_zones) {

    private var googleMap: GoogleMap? = null
    private val anconaCenter = LatLng(43.6158, 13.5189)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()
        setupButtons(view)
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.googleMap) as SupportMapFragment

        mapFragment.getMapAsync { map ->
            googleMap = map

            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false

            enableUserLocationIfAllowed()
            showInitialRedZones()

            moveToCurrentLocation()
        }
    }

    private fun setupButtons(view: View) {
        val addButton = view.findViewById<Button>(R.id.btnAddRedZone)
        val listButton = view.findViewById<Button>(R.id.btnRedZoneList)
        val myLocationButton = view.findViewById<Button>(R.id.btnMyLocation)

        addButton.setOnClickListener {
            addDemoRedZone()
        }

        listButton.setOnClickListener {
            Toast.makeText(requireContext(), "Lista Red Zones da implementare", Toast.LENGTH_SHORT).show()
        }

        myLocationButton.setOnClickListener {
            moveToCurrentLocation()
        }
    }

    private fun enableUserLocationIfAllowed() {
        if (hasLocationPermission()) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    private fun moveToCurrentLocation() {
        if (!hasLocationPermission()) {
            Toast.makeText(requireContext(), "Permesso posizione non concesso", Toast.LENGTH_SHORT).show()
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f))
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val currentPosition = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(currentPosition, 16f)
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Posizione non disponibile, mostro Ancona",
                        Toast.LENGTH_SHORT
                    ).show()
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f))
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nel recupero posizione", Toast.LENGTH_SHORT).show()
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(anconaCenter, 14f))
            }
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    private fun showInitialRedZones() {
        addRedZone(
            position = LatLng(43.6169, 13.5167),
            title = "Zona segnalata",
            description = "Segnalazione community"
        )

        addRedZone(
            position = LatLng(43.6134, 13.5221),
            title = "Area poco sicura",
            description = "Illuminazione scarsa"
        )
    }

    private fun addDemoRedZone() {
        addRedZone(
            position = anconaCenter,
            title = "Nuova Red Zone",
            description = "Segnalazione aggiunta manualmente"
        )

        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(anconaCenter, 15f))
        Toast.makeText(requireContext(), "Red Zone aggiunta", Toast.LENGTH_SHORT).show()
    }

    private fun addRedZone(position: LatLng, title: String, description: String) {
        val map = googleMap ?: return

        map.addMarker(
            MarkerOptions()
                .position(position)
                .title(title)
                .snippet(description)
        )

        map.addCircle(
            CircleOptions()
                .center(position)
                .radius(160.0)
                .strokeColor(0x99FF0000.toInt())
                .fillColor(0x33FF0000)
                .strokeWidth(4f)
        )
    }
}