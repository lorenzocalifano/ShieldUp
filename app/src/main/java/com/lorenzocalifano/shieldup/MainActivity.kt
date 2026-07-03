package com.lorenzocalifano.shieldup

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.lorenzocalifano.shieldup.databinding.ActivityMainBinding
import com.lorenzocalifano.shieldup.utils.SessionManager
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapsInitializer.initialize(
            applicationContext,
            MapsInitializer.Renderer.LEGACY
        ) { renderer ->
            android.util.Log.d("SHIELDUP_MAP", "Renderer Maps usato: $renderer")
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestInitialPermissions()
        setupNavigation()
    }

    private fun requestInitialPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController
        val sessionManager = SessionManager(this)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.customBottomBar.visibility = when (destination.id) {
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.psychologistDashboardFragment -> View.GONE
                else -> View.VISIBLE
            }
        }

        if (sessionManager.isLogged()) {
            val destination = if (sessionManager.getRole() == "PSYCHOLOGIST") {
                R.id.psychologistDashboardFragment
            } else {
                R.id.homeFragment
            }

            navController.navigate(
                destination,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build()
            )
        }

        findViewById<View>(R.id.navHome).setOnClickListener {
            navController.navigateToMainDestination(R.id.homeFragment)
        }

        findViewById<View>(R.id.navMap).setOnClickListener {
            navController.navigateToMainDestination(R.id.redZonesFragment)
        }

        findViewById<View>(R.id.navSupport).setOnClickListener {
            navController.navigateToMainDestination(R.id.supportFragment)
        }

        findViewById<View>(R.id.navProfile).setOnClickListener {
            navController.navigateToMainDestination(R.id.settingsFragment)
        }
    }

    private fun androidx.navigation.NavController.navigateToMainDestination(destinationId: Int) {
        if (currentDestination?.id == destinationId) return

        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.homeFragment, false)
            .build()

        navigate(destinationId, null, options)
    }
}