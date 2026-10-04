// PeerLink Production Sync - Active
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        AppDiagnostics.log("MainActivity", "Delayed notification permission result: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDiagnostics.init(applicationContext)
        com.example.util.PeerNotificationHelper.initNotificationChannel(this)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        handleIntent(intent)
        
        // Permissions are requested on-demand as features are needed (Camera for QR/Video, Mic for Calls, Nearby for Radar).
        // Notifications are requested politely after a while instead of bombarding the user on first install.
        scheduleDelayedNotificationPermissionRequest()

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
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val peerId = intent?.getStringExtra("open_chat_peer_id")
        if (!peerId.isNullOrBlank()) {
            AppDiagnostics.log("MainActivity", "Deep linking to chat with peer: $peerId")
            viewModel.requestOpenChat(peerId)
        }
    }

    private fun scheduleDelayedNotificationPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                // Wait for the user to settle into the app (40 seconds) before politely requesting notification permission
                delay(40_000L)
                if (!isFinishing && !isDestroyed) {
                    val isGranted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!isGranted) {
                        AppDiagnostics.log("MainActivity", "Requesting delayed POST_NOTIFICATIONS permission")
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }
}
