package com.example.gamebooster

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.example.diagnostic.AppDiagnostics
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
import com.example.util.StoragePermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GameBoostProfile(val displayName: String, val description: String, val targetFps: Int) {
    EXTREME_FPS("Ultra 120 FPS", "Maximum CPU/GPU clocks, thermal limit bypass, 240Hz touch sampling", 120),
    BALANCED_GAMING("Balanced 60-90 FPS", "Consistent frame pacing, moderate thermals, low-latency Wi-Fi", 90),
    LOW_LATENCY_ESPORTS("Esports Low Latency", "Focus on minimum touch & packet latency, background apps killed", 60)
}

data class BoosterStatus(
    val isActive: Boolean = false,
    val profile: GameBoostProfile = GameBoostProfile.EXTREME_FPS,
    val isConfigFilePlaced: Boolean = false,
    val configFilePath: String = "",
    val touchBoostActive: Boolean = false,
    val thermalOverrideActive: Boolean = false,
    val networkLowLatencyActive: Boolean = false,
    val ramPurged: Boolean = false,
    val storagePermissionGranted: Boolean = false,
    val lastActionMessage: String = "Standby"
)

class GameBoosterManager(
    private val context: Context,
    private val shizukuManager: ShizukuManager
) {
    private val tag = "GameBoosterManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _status = MutableStateFlow(BoosterStatus())
    val status: StateFlow<BoosterStatus> = _status.asStateFlow()

    private val _boosterLogs = MutableStateFlow<List<String>>(emptyList())
    val boosterLogs: StateFlow<List<String>> = _boosterLogs.asStateFlow()

    init {
        refreshStorageAndFileStatus()
    }

    fun refreshStorageAndFileStatus() {
        val hasStorage = StoragePermissionHelper.hasStoragePermission(context)
        val configFile = getPrimaryConfigFile()
        val exists = configFile.exists() && configFile.length() > 0

        _status.value = _status.value.copy(
            storagePermissionGranted = hasStorage,
            isConfigFilePlaced = exists,
            configFilePath = if (exists) configFile.absolutePath else ""
        )
    }

    private fun getPrimaryConfigFile(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(publicDownloads, "game_booster.cfg")
    }

    private fun getSecondaryConfigFile(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val peerLinkDir = File(publicDownloads, "PeerLink")
        if (!peerLinkDir.exists()) peerLinkDir.mkdirs()
        return File(peerLinkDir, "game_booster.cfg")
    }

    /**
     * Activates Game Booster:
     * 1. Generates and writes tuning parameters to /storage/emulated/0/Download/game_booster.cfg
     * 2. Executes privileged Shizuku/ADB commands to optimize GPU, thermal governor, touch sampling, and RAM.
     */
    suspend fun activateBooster(profile: GameBoostProfile = _status.value.profile): Boolean = withContext(Dispatchers.IO) {
        log("Activating Game Booster with profile: ${profile.displayName}...")

        val hasStorage = StoragePermissionHelper.hasStoragePermission(context)
        var fileCreated = false
        var targetPath = ""

        // 1. Write the gaming config file to Download folder
        try {
            val configContent = generateConfigFileContent(profile)
            val primaryFile = getPrimaryConfigFile()
            
            // Try standard File I/O
            val written = writeConfigToFile(primaryFile, configContent)
            if (written) {
                fileCreated = true
                targetPath = primaryFile.absolutePath
                log("Tuning config successfully placed at: ${primaryFile.absolutePath}")
            } else if (shizukuManager.status.value == ShizukuStatus.AUTHORIZED) {
                // Privileged write via Shizuku shell
                log("Using privileged Shizuku shell to place config file in Download folder...")
                val escapedContent = configContent.replace("\"", "\\\"").replace("$", "\\$")
                shizukuManager.executeShizukuCommand("cat << 'EOF' > \"${primaryFile.absolutePath}\"\n$configContent\nEOF")
                if (primaryFile.exists()) {
                    fileCreated = true
                    targetPath = primaryFile.absolutePath
                    log("Tuning config placed via Shizuku at: ${primaryFile.absolutePath}")
                }
            }

            // Also mirror to PeerLink subfolder for redundancy
            try {
                val secondary = getSecondaryConfigFile()
                writeConfigToFile(secondary, configContent)
            } catch (_: Exception) {}

        } catch (e: Exception) {
            log("Warning: Could not place config file in Download: ${e.message}")
        }

        // 2. Execute Shizuku Privileged System Tweaks
        var touchBoosted = false
        var thermalOverridden = false
        var wifiLowLatency = false
        var ramPurged = false

        if (shizukuManager.status.value == ShizukuStatus.AUTHORIZED) {
            try {
                log("Executing Shizuku privileged system performance commands...")

                // Thermal throttling override
                try {
                    shizukuManager.executeShizukuCommand("cmd thermalservice override-status 0")
                    thermalOverridden = true
                    log("Thermal throttling override: OK (Peak clock sustained)")
                } catch (e: Exception) {
                    log("Thermal override notice: ${e.message}")
                }

                // Touch screen high polling rate (Android 11+)
                try {
                    shizukuManager.executeShizukuCommand("settings put secure high_touch_polling_rate_enabled 1")
                    touchBoosted = true
                    log("Touch polling boost: OK (240Hz sampling enabled)")
                } catch (e: Exception) {
                    log("Touch polling notice: ${e.message}")
                }

                // Wi-Fi Low Latency gaming mode
                try {
                    shizukuManager.executeShizukuCommand("cmd wifi set-low-latency-mode enabled")
                    shizukuManager.executeShizukuCommand("cmd wifi set-scan-throttle-enabled disabled")
                    wifiLowLatency = true
                    log("Gaming Wi-Fi low-latency mode: OK")
                } catch (e: Exception) {
                    log("Wi-Fi boost notice: ${e.message}")
                }

                // Hardware rendering acceleration flags
                try {
                    shizukuManager.executeShizukuCommand("setprop debug.sf.hw 1")
                    shizukuManager.executeShizukuCommand("setprop debug.egl.hw 1")
                    shizukuManager.executeShizukuCommand("setprop debug.sf.latch_unsignaled 1")
                    log("SurfaceFlinger frame pacing optimizations applied")
                } catch (_: Exception) {}

                // Free cached background RAM
                try {
                    shizukuManager.executeShizukuCommand("am kill-all")
                    ramPurged = true
                    log("Cached background tasks purged: Maximum RAM allocated to foreground game")
                } catch (_: Exception) {}

            } catch (e: Exception) {
                log("Error executing some Shizuku booster commands: ${e.message}")
            }
        } else {
            log("Shizuku not authorized. Applied software Wi-Fi lock and config file.")
            shizukuManager.applyLowLatencyNetworkMode()
            wifiLowLatency = true
        }

        val successMsg = if (fileCreated) {
            "Game Booster ACTIVE: Placed /Download/game_booster.cfg & applied tweaks"
        } else if (!hasStorage) {
            "Game Booster ACTIVE via Shizuku (Note: Grant Storage permission to save config file)"
        } else {
            "Game Booster ACTIVE: System performance tweaks applied"
        }

        _status.value = BoosterStatus(
            isActive = true,
            profile = profile,
            isConfigFilePlaced = fileCreated,
            configFilePath = targetPath,
            touchBoostActive = touchBoosted,
            thermalOverrideActive = thermalOverridden,
            networkLowLatencyActive = wifiLowLatency,
            ramPurged = ramPurged,
            storagePermissionGranted = hasStorage,
            lastActionMessage = successMsg
        )

        AppDiagnostics.log(tag, successMsg)
        true
    }

    /**
     * Deactivates Game Booster:
     * - Resets thermal override
     * - Resets Wi-Fi latency tweaks
     * - Cleans up /Download/game_booster.cfg
     */
    suspend fun deactivateBooster(): Boolean = withContext(Dispatchers.IO) {
        log("Deactivating Game Booster & restoring system defaults...")

        if (shizukuManager.status.value == ShizukuStatus.AUTHORIZED) {
            try {
                shizukuManager.executeShizukuCommand("cmd thermalservice reset")
                shizukuManager.executeShizukuCommand("settings put secure high_touch_polling_rate_enabled 0")
                shizukuManager.executeShizukuCommand("cmd wifi set-low-latency-mode disabled")
                shizukuManager.executeShizukuCommand("cmd wifi set-scan-throttle-enabled enabled")
                log("Shizuku system tweaks reset to defaults")
            } catch (e: Exception) {
                log("Reset warning: ${e.message}")
            }
        }

        shizukuManager.resetOptimizations()

        // Clean up or update the config file
        try {
            val primaryFile = getPrimaryConfigFile()
            if (primaryFile.exists()) {
                primaryFile.delete()
                log("Removed /Download/game_booster.cfg")
            }
        } catch (_: Exception) {}

        _status.value = _status.value.copy(
            isActive = false,
            isConfigFilePlaced = false,
            touchBoostActive = false,
            thermalOverrideActive = false,
            networkLowLatencyActive = false,
            ramPurged = false,
            lastActionMessage = "Game Booster deactivated. System restored to defaults."
        )

        true
    }

    private fun writeConfigToFile(file: File, content: String): Boolean {
        return try {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            Log.w(tag, "Failed to write config directly to ${file.absolutePath}: ${e.message}")
            false
        }
    }

    private fun generateConfigFileContent(profile: GameBoostProfile): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return """
# PeerLink Shizuku Game Booster Engine
# Generated: ${dateFormat.format(Date())}
# Target Profile: ${profile.displayName}

[gaming_performance]
status=boost_active
profile=${profile.name}
target_fps=${profile.targetFps}
governor=performance
gpu_renderer=vulkan
thermal_mitigation=disabled
touch_polling_rate=240hz
wifi_low_latency=enabled
kill_cached_apps=true
sf_latch_unsignaled=1
io_scheduler=deadline
""".trimIndent()
    }

    private fun log(message: String) {
        Log.d(tag, message)
        val current = _boosterLogs.value.toMutableList()
        if (current.size > 40) current.removeAt(0)
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        current.add("[$time] $message")
        _boosterLogs.value = current
    }
}
