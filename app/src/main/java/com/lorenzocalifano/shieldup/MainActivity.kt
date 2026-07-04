package com.lorenzocalifano.shieldup

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.maps.MapsInitializer
import com.lorenzocalifano.shieldup.databinding.ActivityMainBinding
import com.lorenzocalifano.shieldup.utils.SessionManager

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
            val isAuthScreen = destination.id == R.id.loginFragment ||
                    destination.id == R.id.registerFragment

            val isFullScreen = destination.id == R.id.chatRoomFragment

            if (isAuthScreen || isFullScreen) {
                binding.userBottomBar.visibility = View.GONE
                binding.psychologistBottomBar.visibility = View.GONE
                return@addOnDestinationChangedListener
            }

            if (sessionManager.getRole() == "PSYCHOLOGIST") {
                binding.userBottomBar.visibility = View.GONE
                binding.psychologistBottomBar.visibility = View.VISIBLE
            } else {
                binding.userBottomBar.visibility = View.VISIBLE
                binding.psychologistBottomBar.visibility = View.GONE
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

        binding.navHome.setOnClickListener {
            navController.navigateToMainDestination(R.id.homeFragment)
        }

        binding.navMap.setOnClickListener {
            navController.navigateToMainDestination(R.id.redZonesFragment)
        }

        binding.navSupport.setOnClickListener {
            navController.navigateToMainDestination(R.id.supportFragment)
        }

        binding.navChat.setOnClickListener {
            navController.navigateToMainDestination(R.id.chatListFragment)
        }

        binding.navProfile.setOnClickListener {
            navController.navigateToMainDestination(R.id.settingsFragment)
        }

        binding.navPsychDashboard.setOnClickListener {
            navController.navigateToMainDestination(R.id.psychologistDashboardFragment)
        }

        binding.navPsychChat.setOnClickListener {
            navController.navigateToMainDestination(R.id.chatListFragment)
        }

        binding.navPsychProfile.setOnClickListener {
            navController.navigateToMainDestination(R.id.settingsFragment)
        }
    }

    private fun androidx.navigation.NavController.navigateToMainDestination(destinationId: Int) {
        if (currentDestination?.id == destinationId) return

        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()

        navigate(destinationId, null, options)
    }
}