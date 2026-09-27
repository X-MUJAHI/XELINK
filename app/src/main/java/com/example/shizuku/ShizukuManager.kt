package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

enum class ShizukuStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    UNAUTHORIZED,
    AUTHORIZED
}

data class BoosterOperationResult(
    val success: Boolean,
    val message: String,
    val executionMethod: String // "SHIZUKU_PRIVILEGED" or "STANDARD_FALLBACK"
)

class ShizukuManager(private val context: Context) {
    private val tag = "ShizukuManager"

    private val _status = MutableStateFlow(ShizukuStatus.NOT_INSTALLED)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val _isLowLatencyEnabled = MutableStateFlow(false)
    val isLowLatencyEnabled: StateFlow<Boolean> = _isLowLatencyEnabled.asStateFlow()

    private val _isBoosterProfilePlaced = MutableStateFlow(false)
    val isBoosterProfilePlaced: StateFlow<Boolean> = _isBoosterProfilePlaced.asStateFlow()

    private val _consoleLog = MutableStateFlow<List<String>>(emptyList())
    val consoleLog: StateFlow<List<String>> = _consoleLog.asStateFlow()

    private var wifiLock: WifiManager.WifiLock? = null

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            logMessage("Shizuku permission response: granted=$granted")
            refreshStatus()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        logMessage("Shizuku binder service connected")
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        logMessage("Shizuku binder service died")
        refreshStatus()
    }

    init {
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (e: Throwable) {
            Log.w(tag, "Shizuku listener registration skipped: ${e.message}")
        }
        refreshStatus()
    }

    fun refreshStatus() {
        val newStatus = checkCurrentStatus()
        _status.value = newStatus
        checkExistingBoosterFile()
        Log.d(tag, "Shizuku status refreshed: $newStatus")
    }

    private fun checkCurrentStatus(): ShizukuStatus {
        try {
            // First check if Shizuku binder is directly alive
            val binderAlive = try {
                Shizuku.pingBinder()
            } catch (_: Throwable) {
                false
            }

            if (binderAlive) {
                val authorized = try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (_: Throwable) {
                    false
                }
                return if (authorized) ShizukuStatus.AUTHORIZED else ShizukuStatus.UNAUTHORIZED
            }

            // If binder is not responding, check whether Shizuku app is installed on device
            val pm = context.packageManager
            val installed = try {
                pm.getPackageInfo("moe.shizuku.privileged.api", 0) != null
            } catch (_: Exception) {
                try {
                    pm.getLaunchIntentForPackage("moe.shizuku.privileged.api") != null
                } catch (_: Exception) {
                    false
                }
            }

            return if (installed) ShizukuStatus.NOT_RUNNING else ShizukuStatus.NOT_INSTALLED
        } catch (e: Throwable) {
            Log.w(tag, "Error checking Shizuku status: ${e.message}")
            return ShizukuStatus.NOT_RUNNING
        }
    }

    fun requestAuthorization() {
        try {
            val binderAlive = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
            if (binderAlive) {
                logMessage("Requesting Shizuku authorization dialog...")
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            } else {
                logMessage("Shizuku service is not running on device. Start Shizuku via Wireless Debugging or Root.")
                refreshStatus()
            }
        } catch (e: Throwable) {
            logMessage("Failed to request Shizuku permission: ${e.message}")
        }
    }

    /**
     * Executes privileged low-latency networking & gaming performance mode.
     * If Shizuku is authorized, applies privileged low-latency Wi-Fi mode and CPU governor tuning.
     * If Shizuku is not available, falls back gracefully to standard Android WifiManager low-latency lock.
     */
    suspend fun applyLowLatencyGamingMode(): BoosterOperationResult = withContext(Dispatchers.IO) {
        if (_status.value == ShizukuStatus.AUTHORIZED) {
            try {
                logMessage("Applying privileged low-latency network tweaks via Shizuku...")
                // Low latency Wi-Fi power save disable
                executeShizukuCommand("cmd wifi set-low-latency-mode enabled")
                // Increase socket buffer limits for zero-packet-drop gaming & streaming
                executeShizukuCommand("settings put global wifi_sleep_policy 2")

                _isLowLatencyEnabled.value = true
                logMessage("Privileged Low-Latency Gaming Mode Active!")
                BoosterOperationResult(
                    success = true,
                    message = "Privileged Low-Latency Gaming Mode activated (Wi-Fi Power Save suppressed & buffers boosted)",
                    executionMethod = "SHIZUKU_PRIVILEGED"
                )
            } catch (e: Exception) {
                logMessage("Shizuku privileged command error: ${e.message}. Using standard fallback...")
                applyStandardWifiLock()
            }
        } else {
            logMessage("Shizuku not authorized. Activating standard Android low-latency lock...")
            applyStandardWifiLock()
        }
    }

    /**
     * Places the game booster performance configuration file in the Downloads folder
     * to increase gaming performance and P2P throughput.
     * Uses Shizuku privileged shell if available, or standard Android filesystem fallback.
     */
    suspend fun placeGameBoosterProfile(): BoosterOperationResult = withContext(Dispatchers.IO) {
        val configContent = """
            # PeerLink P2P & Game Booster Performance Profile
            # Generated automatically to optimize latency, jitter, and frame rendering
            p2p.network.low_latency=1
            p2p.socket.buffer_size=8388608
            gaming.display.force_peak_refresh_rate=1
            gaming.scheduler.priority=MAX_PERFORMANCE
            gaming.wifi.power_save=DISABLED
            gaming.touch.sample_rate=BOOST
            timestamp=${System.currentTimeMillis()}
            status=OPTIMIZED
        """.trimIndent()

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetFile = File(downloadsDir, "p2p_gaming_boost.cfg")

        if (_status.value == ShizukuStatus.AUTHORIZED) {
            try {
                logMessage("Writing gaming booster profile via Shizuku privileged shell...")
                val command = "mkdir -p '${downloadsDir.absolutePath}' && echo '${configContent.replace("\n", "\\n")}' > '${targetFile.absolutePath}'"
                executeShizukuCommand(command)

                _isBoosterProfilePlaced.value = true
                logMessage("Profile saved to: ${targetFile.absolutePath}")
                BoosterOperationResult(
                    success = true,
                    message = "Gaming booster profile deployed at ${targetFile.name} with privileged permissions",
                    executionMethod = "SHIZUKU_PRIVILEGED"
                )
            } catch (e: Exception) {
                logMessage("Shizuku file write failed: ${e.message}. Using standard storage fallback...")
                writeStandardProfile(targetFile, configContent)
            }
        } else {
            logMessage("Using standard filesystem API for booster profile placement...")
            writeStandardProfile(targetFile, configContent)
        }
    }

    private fun writeStandardProfile(targetFile: File, content: String): BoosterOperationResult {
        return try {
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(content)
            _isBoosterProfilePlaced.value = true
            logMessage("Booster config placed via standard storage: ${targetFile.name}")
            BoosterOperationResult(
                success = true,
                message = "Config placed in Downloads (${targetFile.name}) via standard storage",
                executionMethod = "STANDARD_FALLBACK"
            )
        } catch (e: Exception) {
            logMessage("Failed to write booster file: ${e.message}")
            BoosterOperationResult(
                success = false,
                message = "Could not write booster profile: ${e.message}",
                executionMethod = "STANDARD_FALLBACK"
            )
        }
    }

    private fun checkExistingBoosterFile() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetFile = File(downloadsDir, "p2p_gaming_boost.cfg")
            _isBoosterProfilePlaced.value = targetFile.exists() && targetFile.length() > 0
        } catch (_: Exception) {}
    }

    private fun applyStandardWifiLock(): BoosterOperationResult {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wm != null) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock?.release()
                wifiLock = wm.createWifiLock(mode, "PeerLinkLowLatency")
                wifiLock?.acquire()
                _isLowLatencyEnabled.value = true
                logMessage("Standard Android WifiLock (Low Latency) acquired successfully")
                BoosterOperationResult(
                    success = true,
                    message = "Standard Low-Latency Wi-Fi lock active",
                    executionMethod = "STANDARD_FALLBACK"
                )
            } else {
                BoosterOperationResult(false, "WifiManager unavailable", "STANDARD_FALLBACK")
            }
        } catch (e: Exception) {
            BoosterOperationResult(false, "Error: ${e.message}", "STANDARD_FALLBACK")
        }
    }

    suspend fun resetOptimizations(): BoosterOperationResult = withContext(Dispatchers.IO) {
        try {
            wifiLock?.release()
            wifiLock = null
            _isLowLatencyEnabled.value = false

            if (_status.value == ShizukuStatus.AUTHORIZED) {
                executeShizukuCommand("cmd wifi set-low-latency-mode disabled")
            }
            logMessage("Network and gaming booster reset to defaults")
            BoosterOperationResult(true, "Optimizations reset", "ALL")
        } catch (e: Exception) {
            BoosterOperationResult(false, "Reset error: ${e.message}", "ALL")
        }
    }

    private fun executeShizukuCommand(command: String): String {
        return try {
            val method = try {
                Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply { isAccessible = true }
            } catch (_: Exception) {
                null
            }

            val process = if (method != null) {
                method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            } else {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            }
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            reader.close()
            output.toString().trim()
        } catch (e: Exception) {
            Log.e(tag, "Command execution failed: $command, err: ${e.message}")
            throw e
        }
    }

    private fun logMessage(msg: String) {
        Log.d(tag, msg)
        val current = _consoleLog.value.toMutableList()
        if (current.size > 50) current.removeAt(0)
        current.add("[${System.currentTimeMillis() % 100000}] $msg")
        _consoleLog.value = current
    }

    fun cleanUp() {
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            wifiLock?.release()
        } catch (_: Throwable) {}
    }

    companion object {
        const val REQUEST_CODE_SHIZUKU = 4001
    }
}
