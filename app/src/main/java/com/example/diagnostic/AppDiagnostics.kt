package com.example.diagnostic

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

object AppDiagnostics {
    private const val TAG = "PeerLinkDiagnostics"
    private const val MAX_LOGS = 150
    private val logBuffer = ConcurrentLinkedDeque<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var isHandlerInstalled = false

    private val _logsFlow = MutableStateFlow<List<String>>(emptyList())
    val logsFlow: StateFlow<List<String>> = _logsFlow.asStateFlow()

    private val _lastCrashMessage = MutableStateFlow<String?>(null)
    val lastCrashMessage: StateFlow<String?> = _lastCrashMessage.asStateFlow()

    fun init(context: Context) {
        if (!isHandlerInstalled) {
            isHandlerInstalled = true
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                recordCrash(context, thread, throwable)
                defaultHandler?.uncaughtException(thread, throwable)
            }
            log("System", "AppDiagnostics initialized. Model: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            
            // Check for previous crash on startup
            val crash = getSavedCrashLog(context)
            if (crash != null) {
                _lastCrashMessage.value = crash
                log("Startup", "Detected previous crash report (${crash.lines().firstOrNull()})")
            }
        }
    }

    fun getSavedCrashLog(context: Context): String? {
        return try {
            val file = File(context.filesDir, "peerlink_crash.log")
            if (file.exists() && file.length() > 0) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    fun clearSavedCrashLog(context: Context) {
        try {
            val file = File(context.filesDir, "peerlink_crash.log")
            if (file.exists()) file.delete()
            _lastCrashMessage.value = null
        } catch (_: Exception) {}
    }

    fun log(tag: String, message: String, throwable: Throwable? = null) {
        val time = dateFormat.format(Date())
        val entry = if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            "[$time][$tag] $message\n${sw.toString().take(600)}"
        } else {
            "[$time][$tag] $message"
        }

        logBuffer.addLast(entry)
        while (logBuffer.size > MAX_LOGS) {
            logBuffer.pollFirst()
        }
        _logsFlow.value = logBuffer.toList()

        if (throwable != null) {
            Log.e(TAG, "[$tag] $message", throwable)
        } else {
            Log.d(TAG, "[$tag] $message")
        }
    }

    private fun recordCrash(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        val crashText = "CRASH on thread [${thread.name}]: ${throwable.message}\n$stackTrace"
        _lastCrashMessage.value = crashText

        log("CRASH", crashText)

        try {
            val file = File(context.filesDir, "peerlink_crash.log")
            file.writeText(
                "--- CRASH AT ${Date()} ---\n" +
                "Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})\n" +
                crashText + "\n" +
                "--- RECENT LOGS ---\n" +
                logBuffer.joinToString("\n")
            )
        } catch (_: Exception) {}
    }

    fun generateReport(context: Context, extraInfo: Map<String, String> = emptyMap()): String {
        val sb = StringBuilder()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        sb.appendLine("══════════════════════════════════════════")
        sb.appendLine("     PEERLINK DIAGNOSTIC & DEBUG REPORT   ")
        sb.appendLine("══════════════════════════════════════════")
        sb.appendLine("Timestamp: $time")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
        sb.appendLine("Android OS: ${Build.VERSION.RELEASE} (API Level ${Build.VERSION.SDK_INT})")
        sb.appendLine("Build Fingerprint: ${Build.FINGERPRINT}")

        // Memory info
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            val runtime = Runtime.getRuntime()
            val maxMemMb = runtime.maxMemory() / (1024 * 1024)
            val totalMemMb = runtime.totalMemory() / (1024 * 1024)
            val freeMemMb = runtime.freeMemory() / (1024 * 1024)
            sb.appendLine("\n─── MEMORY METRICS ───")
            sb.appendLine("System Avail RAM: ${memInfo.availMem / (1024 * 1024)} MB (LowMem: ${memInfo.lowMemory})")
            sb.appendLine("JVM Max Heap: ${maxMemMb}MB | Total: ${totalMemMb}MB | Free: ${freeMemMb}MB | Used: ${totalMemMb - freeMemMb}MB")
        } catch (_: Exception) {}

        // Extra status from managers
        if (extraInfo.isNotEmpty()) {
            sb.appendLine("\n─── SUBSYSTEM STATES ───")
            extraInfo.forEach { (key, value) ->
                sb.appendLine("• $key: $value")
            }
        }

        // Check persistent crash log
        try {
            val crashFile = File(context.filesDir, "peerlink_crash.log")
            if (crashFile.exists()) {
                val crashContent = crashFile.readText().take(1500)
                sb.appendLine("\n─── RECORDED PREVIOUS CRASH ───")
                sb.appendLine(crashContent)
            }
        } catch (_: Exception) {}

        // Recent runtime logs
        sb.appendLine("\n─── RECENT RUNTIME LOGS (${logBuffer.size}) ───")
        if (logBuffer.isEmpty()) {
            sb.appendLine("(No logs recorded yet)")
        } else {
            logBuffer.forEach { entry ->
                sb.appendLine(entry)
            }
        }
        sb.appendLine("══════════════════════════════════════════")

        return sb.toString()
    }

    fun copyReportToClipboard(context: Context, extraInfo: Map<String, String> = emptyMap()) {
        try {
            val report = generateReport(context, extraInfo)
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("PeerLink Diagnostic Log", report)
            cm.setPrimaryClip(clip)
            Toast.makeText(context, "📋 Diagnostic logs copied to clipboard!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to copy logs: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
