package com.lorenzocalifano.shieldup

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.lorenzocalifano.shieldup.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Per ora non blocchiamo l'app se l'utente nega i permessi.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        findViewById<View>(R.id.navHome).setOnClickListener {
            navController.popBackStack(R.id.homeFragment, false)
        }

        findViewById<View>(R.id.navMap).setOnClickListener {
            navController.navigate(R.id.redZonesFragment)
        }

        findViewById<View>(R.id.navSupport).setOnClickListener {
            navController.navigate(R.id.supportFragment)
        }

        findViewById<View>(R.id.navProfile).setOnClickListener {
            navController.navigate(R.id.settingsFragment)
        }
    }
}