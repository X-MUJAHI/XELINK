/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: MainActivity.kt
 *
 * Commentary / Architectural Overview:
 * Entry point for the Android application.
 * Responsibilities:
 * - Bootstraps app diagnostics and initializes edge-to-edge system bar window insets.
 * - Enforces Portrait orientation and handles runtime permissions (Wi-Fi, Bluetooth, Camera, Audio, Shizuku).
 * - Connects to MainViewModel and provides responsive display density scaling.
 * - Renders the root Compose hierarchy inside MyApplicationTheme with the Cyberpunk Dark theme environment.
 */

package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.diagnostic.AppDiagnostics
import com.example.ui.navigation.PeerLinkApp
import com.example.ui.theme.LocalUiThemeStyle
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val requiredPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        AppDiagnostics.log("MainActivity", "Initial permissions request finished, checking Shizuku...")
        checkAndRequestShizuku()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDiagnostics.init(applicationContext)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        requestInitialPermissions()

        setContent {
            val uiScaleConfig by viewModel.uiScaleManager.config.collectAsState()
            val uiThemeStyle by viewModel.uiThemeManager.currentStyle.collectAsState()
            val systemDensity = LocalDensity.current
            val configuration = LocalConfiguration.current

            val effectiveScale = remember(uiScaleConfig, systemDensity.density, configuration.screenWidthDp, configuration.screenHeightDp) {
                viewModel.uiScaleManager.computeEffectiveScale(
                    configuration.screenWidthDp,
                    configuration.screenHeightDp,
                    systemDensity.density,
                    systemDensity.fontScale
                )
            }
            val effectiveFontScale = remember(uiScaleConfig, systemDensity.fontScale) {
                viewModel.uiScaleManager.computeEffectiveFontScale(
                    systemDensity.fontScale
                )
            }
            val customDensity = remember(systemDensity.density, effectiveScale, effectiveFontScale) {
                Density(
                    density = systemDensity.density * effectiveScale,
                    fontScale = effectiveFontScale
                )
            }

            CompositionLocalProvider(
                LocalDensity provides customDensity,
                LocalUiThemeStyle provides uiThemeStyle
            ) {
                MyApplicationTheme(darkTheme = true) {
                    PeerLinkApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppDiagnostics.log("MainActivity", "onResume: refreshing subsystem states")
        viewModel.uiScaleManager.refreshDisplayMetrics()
        viewModel.shizukuManager.refreshStatus()
        viewModel.shizukuManager.autoRequestAuthorizationIfPending()
        lifecycleScope.launch {
            delay(500)
            viewModel.shizukuManager.refreshStatus()
        }
    }

    private fun requestInitialPermissions() {
        // Precise and Coarse location must always be requested together on modern Android
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        // Shizuku manager API v23 runtime permission
        permissionsToRequest.add("moe.shizuku.manager.permission.API_V23")

        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            AppDiagnostics.log("MainActivity", "Requesting ${ungranted.size} startup permissions: $ungranted")
            requiredPermissionsLauncher.launch(ungranted.toTypedArray())
        } else {
            AppDiagnostics.log("MainActivity", "All runtime permissions already granted, checking Shizuku...")
            checkAndRequestShizuku()
        }
    }

    private fun checkAndRequestShizuku() {
        try {
            viewModel.shizukuManager.autoRequestAuthorizationIfPending()
        } catch (e: Throwable) {
            AppDiagnostics.log("MainActivity", "checkAndRequestShizuku error: ${e.message}")
        }
    }
}
