// PeerLink Production Sync - Active
/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiModelDownloader.kt
 *
 * Commentary / Architectural Overview:
 * High-performance background downloader for Qwen GGUF model files:
 * - Stores files in permanent public external storage:
 *     Primary: /storage/emulated/0/Download/PeerLink/ai_models/
 *     Secondary: /storage/emulated/0/PeerLink/ai_models/
 * - Prevents data deletion on app uninstallation so models are preserved permanently.
 * - Automatically scans and restores existing .gguf models when reinstalling.
 * - Supports HTTP range resumption for large multi-gigabyte models.
 * - Reports real-time byte counts, progress percent, and transfer speed (MB/s).
 * - Verifies storage availability before commencing download to avoid storage exhaustion.
 * - Computes device RAM specs to indicate hardware compatibility for each model tier.
 */

package com.example.ai

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AiModelDownloader(private val context: Context) {
    private val tag = "AiModelDownloader"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }
    private val prefs by lazy {
        context.getSharedPreferences("peerlink_gguf_downloads", Context.MODE_PRIVATE)
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val activeDownloadJobs = ConcurrentHashMap<String, Job>()
    private val activeCalls = ConcurrentHashMap<String, okhttp3.Call>()

    private val _models = MutableStateFlow<List<QwenGgufModel>>(emptyList())
    val models: StateFlow<List<QwenGgufModel>> = _models.asStateFlow()

    private val _hardwareSpec = MutableStateFlow(computeHardwareSpec())
    val hardwareSpec: StateFlow<DeviceHardwareSpec> = _hardwareSpec.asStateFlow()

    val persistentDirectoryPath: String
        get() = getModelsDirectory().absolutePath

    private var monitorJob: Job? = null

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != -1L) {
                    handleDownloadComplete(id)
                }
            }
        }
    }

    init {
        initializeModelsList()
        try {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(downloadReceiver, filter)
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not register DownloadManager broadcast receiver: ${e.message}")
        }
        startMonitorLoop()
    }

    /**
     * Resolves the permanent external storage directory for AI models:
     * - Primary: /storage/emulated/0/Download/PeerLink/ai_models/
     * - Secondary: /storage/emulated/0/PeerLink/ai_models/
     * - Fallback: context.getExternalFilesDir(null)/PeerLink/ai_models/
     *
     * Crucially, files stored in /Download/PeerLink/ or /PeerLink/ are NOT deleted
     * by Android when the app is uninstalled, ensuring models and chats survive.
     */
    fun getModelsDirectory(): File {
        val candidates = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PeerLink/ai_models"),
            File(Environment.getExternalStorageDirectory(), "PeerLink/ai_models"),
            File(context.getExternalFilesDir(null), "PeerLink/ai_models")
        )
        for (candidate in candidates) {
            if (!candidate.exists()) {
                try {
                    candidate.mkdirs()
                } catch (e: Exception) {
                    Log.w(tag, "Could not mkdirs ${candidate.absolutePath}: ${e.message}")
                }
            }
            if (candidate.exists() && candidate.canWrite()) {
                return candidate
            }
        }
        val fallback = File(context.getExternalFilesDir(null), "PeerLink/ai_models")
        fallback.mkdirs()
        return fallback
    }

    /**
     * List of all possible directories where previously downloaded models might reside
     * (including legacy app-specific directories or custom user downloads) so that models
     * survive uninstalls and can be automatically restored.
     */
    fun getAllCandidateDirectories(): List<File> {
        val list = mutableListOf<File>()
        list.add(getModelsDirectory())
        list.add(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PeerLink/ai_models"))
        list.add(File(Environment.getExternalStorageDirectory(), "PeerLink/ai_models"))
        list.add(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PeerLink"))
        list.add(File(Environment.getExternalStorageDirectory(), "PeerLink"))
        context.getExternalFilesDir(null)?.let {
            list.add(File(it, "ai_models"))
            list.add(File(it, "PeerLink/ai_models"))
        }
        return list.distinctBy { it.absolutePath }
    }

    fun refreshHardwareSpec() {
        _hardwareSpec.value = computeHardwareSpec()
    }

    private fun computeHardwareSpec(): DeviceHardwareSpec {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem

        val stat = StatFs(getModelsDirectory().path)
        val freeBytes = stat.availableBytes

        return DeviceHardwareSpec(
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            totalRamFormatted = formatBytes(totalRam),
            availableRamFormatted = formatBytes(availRam),
            freeStorageBytes = freeBytes,
            freeStorageFormatted = formatBytes(freeBytes),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            archName = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        )
    }

    private fun initializeModelsList() {
        val hw = computeHardwareSpec()

        val predefined = listOf(
            // ================= Built-in Edge Neural Core =================
            QwenGgufModel(
                id = "builtin_neural_core",
                name = "PeerLink Neural Core (Built-in)",
                parameters = "Hybrid Engine",
                quantization = "Dynamic",
                estimatedSizeBytes = 0L,
                formattedSize = "Pre-installed",
                minRamBytes = 256_000_000L,
                minRamFormatted = "All Devices",
                recommendedTier = "Built-in (Online & Offline Intelligence)",
                downloadUrl = "",
                localFileName = "builtin_engine",
                description = "Zero-setup built-in intelligence engine. Instant response, comprehensive offline knowledge base, live internet query lookup, and programming assistant.",
                isRecommendedForDevice = true,
                status = DownloadStatus.COMPLETED,
                downloadProgress = 1f,
                downloadedBytes = 0L,
                totalBytes = 0L,
                isActive = true
            ),
            // ================= RunAnywhere Ultra-Lightweight Series =================
            QwenGgufModel(
                id = "smollm2_135m",
                name = "SmolLM2 135M (Ultra Light)",
                parameters = "135 Million",
                quantization = "Q4_K_M",
                estimatedSizeBytes = 110_100_480L, // ~105 MB
                formattedSize = "~105 MB",
                minRamBytes = 536_870_912L, // 512 MB RAM
                minRamFormatted = "0.5 GB RAM",
                recommendedTier = "All Phones (Featherweight - 80 tok/s)",
                downloadUrl = "https://huggingface.co/QuantFactory/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct.Q4_K_M.gguf?download=true",
                localFileName = "SmolLM2-135M-Instruct.Q4_K_M.gguf",
                description = "RunAnywhere ultra-lightweight mobile model. Lightning fast (70-90 tok/s), consumes almost zero RAM, and downloads in seconds (~105MB).",
                isRecommendedForDevice = true
            ),
            QwenGgufModel(
                id = "smollm2_360m",
                name = "SmolLM2 360M (Compact)",
                parameters = "360 Million",
                quantization = "Q4_K_M",
                estimatedSizeBytes = 256_901_120L, // ~245 MB
                formattedSize = "~245 MB",
                minRamBytes = 805_306_368L, // 800 MB RAM
                minRamFormatted = "0.8 GB RAM",
                recommendedTier = "Budget & Low RAM (Fast - 60 tok/s)",
                downloadUrl = "https://huggingface.co/unsloth/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf?download=true",
                localFileName = "SmolLM2-360M-Instruct-Q4_K_M.gguf",
                description = "Compact reasoning in under 250MB. Great balance for low-end or older phones without draining battery.",
                isRecommendedForDevice = hw.totalRamBytes <= 4_000_000_000L
            ),
            QwenGgufModel(
                id = "llama3_2_1b",
                name = "Llama 3.2 1B (Meta Edge)",
                parameters = "1.0 Billion",
                quantization = "Q4_K_M",
                estimatedSizeBytes = 786_432_000L, // ~750 MB
                formattedSize = "~750 MB",
                minRamBytes = 1_879_048_192L, // 1.8 GB RAM
                minRamFormatted = "1.8 GB RAM",
                recommendedTier = "Standard Mobile (Optimal Balance)",
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf?download=true",
                localFileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                description = "Meta's flagship mobile-first model. High quality instruction following, summarization, and writing in a 750MB footprint.",
                isRecommendedForDevice = hw.totalRamBytes in 3_000_000_000L..7_000_000_000L
            ),
            QwenGgufModel(
                id = "llama3_2_3b",
                name = "Llama 3.2 3B (Meta Flagship)",
                parameters = "3.2 Billion",
                quantization = "Q4_K_M",
                estimatedSizeBytes = 2_097_152_000L, // ~2.0 GB
                formattedSize = "~2.0 GB",
                minRamBytes = 3_758_096_384L, // 3.5 GB RAM
                minRamFormatted = "3.5 GB RAM",
                recommendedTier = "High-End Phones (Deep Logic)",
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf?download=true",
                localFileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
                description = "Advanced 3.2B parameter reasoning engine. Outstanding coding, mathematical problem-solving, and conversational intelligence.",
                isRecommendedForDevice = hw.totalRamBytes in 6_000_000_000L..12_000_000_000L
            ),
            // ================= Qwen Series =================
            QwenGgufModel(
                id = "qwen3_0_6b",
                name = "Qwen3 0.6B (Compact)",
                parameters = "0.6 Billion",
                quantization = "Q4_K_M",
                estimatedSizeBytes = 398_458_880L, // ~398 MB
                formattedSize = "~400 MB",
                minRamBytes = 1_073_741_824L, // 1 GB RAM
                minRamFormatted = "1.0 GB RAM",
                recommendedTier = "All Phones (Ultra Light)",
                downloadUrl = "https://huggingface.co/Qwen/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q4_K_M.gguf?download=true",
                localFileName = "Qwen3-0.6B-Q4_K_M.gguf",
                description = "Ultra lightweight and exceptionally fast (25-45 tok/s). Ideal for quick questions, formatting, and devices with limited RAM.",
                isRecommendedForDevice = hw.totalRamBytes >= 1_073_741_824L
            ),
            QwenGgufModel(
                id = "qwen3_1_7b",
                name = "Qwen3 1.7B (Balanced)",
                parameters = "1.7 Billion",
                quantization = "Q4_K_M",
                estimatedSizeBytes = 1_181_116_000L, // ~1.18 GB
                formattedSize = "~1.1 GB",
                minRamBytes = 2_684_354_560L, // 2.5 GB RAM
                minRamFormatted = "2.5 GB RAM",
                recommendedTier = "Mid-Range (6GB+ RAM)",
                downloadUrl = "https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf?download=true",
                localFileName = "Qwen3-1.7B-Q4_K_M.gguf",
                description = "Optimal balance of intelligence, speed (15-25 tok/s), and memory usage. Recommended daily offline driver.",
                isRecommendedForDevice = hw.totalRamBytes in 3_000_000_000L..8_500_000_000L
            ),
            QwenGgufModel(
                id = "qwen3_4b",
                name = "Qwen3 4B (High Quality)",
                parameters = "4.0 Billion",
                quantization = "Q4_K_M",
                estimatedSizeBytes = 2_684_354_560L, // ~2.6 GB
                formattedSize = "~2.5 GB",
                minRamBytes = 4_831_838_208L, // 4.5 GB RAM
                minRamFormatted = "4.5 GB RAM",
                recommendedTier = "High Performance (8GB+ RAM)",
                downloadUrl = "https://huggingface.co/Qwen/Qwen3-4B-GGUF/resolve/main/Qwen3-4B-Q4_K_M.gguf?download=true",
                localFileName = "Qwen3-4B-Q4_K_M.gguf",
                description = "High quality coding, reasoning, and conversational comprehension. Recommended for phones with 8GB RAM or more.",
                isRecommendedForDevice = hw.totalRamBytes in 7_500_000_000L..12_500_000_000L
            ),
            QwenGgufModel(
                id = "qwen3_8b",
                name = "Qwen3 8B (Deep Reasoning)",
                parameters = "8.0 Billion",
                quantization = "Q4_K_M",
                estimatedSizeBytes = 5_368_709_120L, // ~5.0 GB
                formattedSize = "~5.0 GB",
                minRamBytes = 8_053_063_680L, // 7.5 GB RAM
                minRamFormatted = "7.5 GB RAM",
                recommendedTier = "Flagship (12GB+ RAM)",
                downloadUrl = "https://huggingface.co/Qwen/Qwen3-8B-GGUF/resolve/main/Qwen3-8B-Q4_K_M.gguf?download=true",
                localFileName = "Qwen3-8B-Q4_K_M.gguf",
                description = "Advanced reasoning, complex logic, and deep analysis. Requires flagship hardware with 12GB+ RAM.",
                isRecommendedForDevice = hw.totalRamBytes >= 11_500_000_000L
            ),
            QwenGgufModel(
                id = "qwen3_14b",
                name = "Qwen3 14B (Ultimate Master)",
                parameters = "14.0 Billion",
                quantization = "Q4_K_M",
                estimatedSizeBytes = 9_663_676_416L, // ~9.0 GB
                formattedSize = "~9.0 GB",
                minRamBytes = 13_421_772_800L, // 12.5 GB RAM
                minRamFormatted = "12.5 GB RAM",
                recommendedTier = "Extreme Flagship (16GB+ RAM)",
                downloadUrl = "https://huggingface.co/Qwen/Qwen3-14B-GGUF/resolve/main/Qwen3-14B-Q4_K_M.gguf?download=true",
                localFileName = "Qwen3-14B-Q4_K_M.gguf",
                description = "Full-scale flagship tier intelligence. Requires high-end gaming phone or tablet with 16GB RAM.",
                isRecommendedForDevice = hw.totalRamBytes >= 15_000_000_000L
            )
        )

        _models.value = predefined
        scanAndRestoreModels()
    }

    /**
     * Scans all persistent and candidate directories to detect previously downloaded models.
     * Automatically restores completed and partial models so they survive uninstall/reinstall.
     * Also reconnects to any active background downloads running via Android's DownloadManager.
     */
    fun scanAndRestoreModels(): Int {
        val candidates = getAllCandidateDirectories()
        var restoredCount = 0
        val currentModels = _models.value.toMutableList()
        val primaryDir = getModelsDirectory()

        for (i in currentModels.indices) {
            val model = currentModels[i]
            if (model.id == "builtin_neural_core") {
                currentModels[i] = model.copy(status = DownloadStatus.COMPLETED, downloadProgress = 1f)
                continue
            }

            // Check if there is an ongoing background download via DownloadManager
            val dmId = prefs.getLong("download_${model.id}", -1L)
            if (dmId != -1L) {
                val query = DownloadManager.Query().setFilterById(dmId)
                val cursor = try { downloadManager.query(query) } catch (_: Exception) { null }
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusIdx != -1) cursor.getInt(statusIdx) else -1
                    val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytes = if (bytesIdx != -1) cursor.getLong(bytesIdx) else 0L
                    val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val total = if (totalIdx != -1) cursor.getLong(totalIdx) else model.estimatedSizeBytes
                    cursor.close()

                    if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) {
                        val progress = if (total > 0) (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                        currentModels[i] = model.copy(
                            status = DownloadStatus.DOWNLOADING,
                            downloadProgress = progress,
                            downloadedBytes = bytes,
                            totalBytes = total
                        )
                        continue
                    } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        handleDownloadComplete(dmId)
                        continue
                    }
                } else {
                    cursor?.close()
                }
            }

            var foundFile: File? = null

            // 1. Search for completed model in any candidate directory
            for (dir in candidates) {
                val candidateFile = File(dir, model.localFileName)
                if (candidateFile.exists() && candidateFile.length() > 500_000L) {
                    foundFile = candidateFile
                    break
                }
            }

            // 2. Search for partial download
            var foundPartFile: File? = null
            if (foundFile == null) {
                for (dir in candidates) {
                    val candidatePart = File(dir, "${model.localFileName}.part")
                    if (candidatePart.exists() && candidatePart.length() > 0) {
                        foundPartFile = candidatePart
                        break
                    }
                }
            }

            if (foundFile != null) {
                // If found in a legacy or cache folder, ensure it's copied/accessible in primary persistent directory
                var effectiveFile = foundFile
                if (!foundFile.absolutePath.startsWith(primaryDir.absolutePath) && primaryDir.canWrite()) {
                    try {
                        val destination = File(primaryDir, model.localFileName)
                        if (!destination.exists() || destination.length() != foundFile.length()) {
                            foundFile.copyTo(destination, overwrite = true)
                        }
                        if (destination.exists() && destination.length() > 500_000L) {
                            effectiveFile = destination
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Could not mirror model to primary dir: ${e.message}")
                    }
                }

                currentModels[i] = model.copy(
                    status = DownloadStatus.COMPLETED,
                    downloadProgress = 1f,
                    downloadedBytes = effectiveFile.length(),
                    totalBytes = effectiveFile.length(),
                    localFilePath = effectiveFile.absolutePath,
                    localFileSize = effectiveFile.length()
                )
                restoredCount++
            } else if (foundPartFile != null) {
                val progress = (foundPartFile.length().toFloat() / model.estimatedSizeBytes.toFloat()).coerceIn(0f, 0.99f)
                currentModels[i] = model.copy(
                    status = DownloadStatus.PAUSED,
                    downloadProgress = progress,
                    downloadedBytes = foundPartFile.length(),
                    totalBytes = model.estimatedSizeBytes,
                    localFilePath = foundPartFile.absolutePath,
                    localFileSize = foundPartFile.length()
                )
            }
        }

        // Set active model if any completed
        val hasActive = currentModels.any { it.isActive && it.status == DownloadStatus.COMPLETED }
        if (!hasActive) {
            val firstCompletedIdx = currentModels.indexOfFirst { it.status == DownloadStatus.COMPLETED }
            if (firstCompletedIdx != -1) {
                currentModels[firstCompletedIdx] = currentModels[firstCompletedIdx].copy(isActive = true)
            }
        }

        _models.value = currentModels
        return restoredCount
    }

    /**
     * Starts downloading the GGUF model.
     * Uses Android OS DownloadManager so closing or swiping away the app NEVER pauses or kills the download.
     */
    fun startDownload(modelId: String) {
        if (modelId == "builtin_neural_core") return
        val model = _models.value.find { it.id == modelId } ?: return
        if (model.status == DownloadStatus.DOWNLOADING) return

        // Verify storage capacity
        val stat = StatFs(getModelsDirectory().path)
        val availableBytes = stat.availableBytes
        if (availableBytes < model.estimatedSizeBytes) {
            updateModel(modelId) {
                it.copy(
                    status = DownloadStatus.ERROR,
                    errorMessage = "Insufficient storage space (${formatBytes(availableBytes)} free, need ${model.formattedSize})"
                )
            }
            return
        }

        try {
            val modelsDir = getModelsDirectory()
            if (!modelsDir.exists()) modelsDir.mkdirs()

            val request = DownloadManager.Request(Uri.parse(model.downloadUrl))
                .setTitle(model.name)
                .setDescription("Downloading Qwen GGUF model in background...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE or DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "PeerLink/ai_models/${model.localFileName}")

            val downloadId = downloadManager.enqueue(request)
            prefs.edit().putLong("download_$modelId", downloadId).apply()
            prefs.edit().putString("model_$downloadId", modelId).apply()

            updateModel(modelId) {
                it.copy(
                    status = DownloadStatus.DOWNLOADING,
                    downloadSpeedBps = 0L,
                    errorMessage = null
                )
            }
            startMonitorLoop()
            Log.d(tag, "Started system background download for ${model.name} (Download ID: $downloadId)")
        } catch (e: Exception) {
            Log.w(tag, "DownloadManager failed (${e.message}), falling back to internal thread")
            val job = scope.launch {
                downloadModelInternal(model)
            }
            activeDownloadJobs[modelId] = job
        }
    }

    private fun startMonitorLoop() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                var anyActive = false
                val current = _models.value

                for (model in current) {
                    if (model.id == "builtin_neural_core") continue
                    val downloadId = prefs.getLong("download_${model.id}", -1L)
                    if (downloadId != -1L) {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = try { downloadManager.query(query) } catch (_: Exception) { null }
                        if (cursor != null && cursor.moveToFirst()) {
                            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = if (statusIdx != -1) cursor.getInt(statusIdx) else -1
                            val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val bytes = if (bytesIdx != -1) cursor.getLong(bytesIdx) else 0L
                            val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val total = if (totalIdx != -1) cursor.getLong(totalIdx) else model.estimatedSizeBytes
                            cursor.close()

                            when (status) {
                                DownloadManager.STATUS_RUNNING -> {
                                    anyActive = true
                                    val progress = if (total > 0) (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                                    val lastBytes = model.downloadedBytes
                                    val speed = (bytes - lastBytes).coerceAtLeast(0L) * 2
                                    updateModel(model.id) {
                                        it.copy(
                                            status = DownloadStatus.DOWNLOADING,
                                            downloadProgress = progress,
                                            downloadedBytes = bytes,
                                            totalBytes = total,
                                            downloadSpeedBps = speed
                                        )
                                    }
                                }
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    handleDownloadComplete(downloadId)
                                }
                                DownloadManager.STATUS_PAUSED -> {
                                    updateModel(model.id) {
                                        it.copy(status = DownloadStatus.PAUSED, downloadSpeedBps = 0L)
                                    }
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    prefs.edit().remove("download_${model.id}").remove("model_$downloadId").apply()
                                    updateModel(model.id) {
                                        it.copy(status = DownloadStatus.ERROR, errorMessage = "Background download failed", downloadSpeedBps = 0L)
                                    }
                                }
                            }
                        } else {
                            cursor?.close()
                        }
                    }
                }

                if (!anyActive && activeDownloadJobs.isEmpty()) {
                    delay(2500L)
                } else {
                    delay(500L)
                }
            }
        }
    }

    private fun handleDownloadComplete(downloadId: Long) {
        val modelId = prefs.getString("model_$downloadId", null) ?: return
        prefs.edit().remove("download_$modelId").remove("model_$downloadId").apply()

        val model = _models.value.find { it.id == modelId } ?: return
        val targetFile = File(getModelsDirectory(), model.localFileName)
        val fileLen = if (targetFile.exists()) targetFile.length() else model.estimatedSizeBytes

        Log.d(tag, "Download complete for ${model.name}, file size: $fileLen bytes at ${targetFile.absolutePath}")

        updateModel(modelId) {
            it.copy(
                status = DownloadStatus.COMPLETED,
                downloadProgress = 1f,
                downloadedBytes = fileLen,
                totalBytes = fileLen,
                downloadSpeedBps = 0L,
                localFilePath = targetFile.absolutePath,
                localFileSize = fileLen,
                isActive = _models.value.none { m -> m.isActive && m.status == DownloadStatus.COMPLETED }
            )
        }
        refreshHardwareSpec()
    }

    fun pauseDownload(modelId: String) {
        Log.d(tag, "pauseDownload requested for model $modelId")
        val downloadId = prefs.getLong("download_$modelId", -1L)
        if (downloadId != -1L) {
            try { downloadManager.remove(downloadId) } catch (_: Exception) {}
            prefs.edit().remove("download_$modelId").remove("model_$downloadId").apply()
        }
        activeCalls[modelId]?.cancel()
        activeCalls.remove(modelId)
        activeDownloadJobs[modelId]?.cancel()
        activeDownloadJobs.remove(modelId)
        updateModel(modelId) {
            it.copy(status = DownloadStatus.PAUSED, downloadSpeedBps = 0L)
        }
    }

    fun deleteModel(modelId: String): Boolean {
        if (modelId == "builtin_neural_core") return false
        Log.d(tag, "deleteModel / cancel requested for model $modelId")

        val model = _models.value.find { it.id == modelId } ?: return false
        val originalFilePath = model.localFilePath
        val wasActive = model.isActive

        val downloadId = prefs.getLong("download_$modelId", -1L)
        if (downloadId != -1L) {
            try { downloadManager.remove(downloadId) } catch (_: Exception) {}
            prefs.edit().remove("download_$modelId").remove("model_$downloadId").apply()
        }
        activeCalls[modelId]?.cancel()
        activeCalls.remove(modelId)
        activeDownloadJobs[modelId]?.cancel()
        activeDownloadJobs.remove(modelId)

        var deletedAny = false

        // 1. Delete originalFilePath if known
        if (!originalFilePath.isNullOrBlank()) {
            try {
                val f = File(originalFilePath)
                if (f.exists()) {
                    val d = f.delete()
                    if (d) deletedAny = true
                    Log.d(tag, "Deleted originalFilePath $originalFilePath: $d")
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not delete originalFilePath $originalFilePath: ${e.message}")
            }
        }

        // 2. Delete from all candidate directories so nothing remains orphaned
        val candidates = getAllCandidateDirectories()
        for (dir in candidates) {
            val targetFile = File(dir, model.localFileName)
            val partFile = File(dir, "${model.localFileName}.part")
            try {
                if (targetFile.exists()) {
                    val d = targetFile.delete()
                    if (d) deletedAny = true
                    Log.d(tag, "Deleted targetFile ${targetFile.absolutePath}: $d")
                }
                if (partFile.exists()) {
                    val d = partFile.delete()
                    if (d) deletedAny = true
                    Log.d(tag, "Deleted partFile ${partFile.absolutePath}: $d")
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not delete candidate file: ${e.message}")
            }
        }

        // 3. Android MediaStore cleanup if the file was created in Downloads on Q+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(model.localFileName, "${model.localFileName}.part")
                context.contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val itemUri = ContentUris.withAppendedId(queryUri, id)
                        val count = context.contentResolver.delete(itemUri, null, null)
                        if (count > 0) deletedAny = true
                        Log.d(tag, "Deleted MediaStore download item: $itemUri")
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "MediaStore cleanup error: ${e.message}")
            }
        }

        // 4. Update state to NOT_DOWNLOADED
        updateModel(modelId) {
            it.copy(
                status = DownloadStatus.NOT_DOWNLOADED,
                downloadProgress = 0f,
                downloadedBytes = 0L,
                totalBytes = 0L,
                downloadSpeedBps = 0L,
                localFilePath = null,
                localFileSize = 0L,
                isActive = false,
                errorMessage = null
            )
        }

        // 5. If it was active, switch to builtin_neural_core or another completed model
        if (wasActive) {
            val fallback = _models.value.find { it.id == "builtin_neural_core" }
                ?: _models.value.find { it.status == DownloadStatus.COMPLETED }
            if (fallback != null) {
                setActiveModel(fallback.id)
            }
        }

        refreshHardwareSpec()
        return deletedAny
    }

    fun setActiveModel(modelId: String) {
        _models.value = _models.value.map {
            if (it.id == modelId && it.status == DownloadStatus.COMPLETED) {
                it.copy(isActive = true)
            } else {
                it.copy(isActive = false)
            }
        }
    }

    fun getActiveModel(): QwenGgufModel? {
        return _models.value.find { it.isActive && it.status == DownloadStatus.COMPLETED }
            ?: _models.value.find { it.status == DownloadStatus.COMPLETED }
    }

    private suspend fun downloadModelInternal(model: QwenGgufModel): Unit = withContext(Dispatchers.IO) {
        val modelsDir = getModelsDirectory()
        val partFile = File(modelsDir, "${model.localFileName}.part")
        val finalFile = File(modelsDir, model.localFileName)

        var existingBytes = if (partFile.exists()) partFile.length() else 0L

        updateModel(model.id) {
            it.copy(
                status = DownloadStatus.DOWNLOADING,
                downloadedBytes = existingBytes,
                errorMessage = null
            )
        }

        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val requestBuilder = Request.Builder()
                .url(model.downloadUrl)
                .header("User-Agent", "PeerLink-Android/1.0 (Mobile GGUF Engine)")

            if (existingBytes > 0) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
                Log.d(tag, "Resuming download of ${model.name} from byte $existingBytes")
            }

            val call = httpClient.newCall(requestBuilder.build())
            activeCalls[model.id] = call

            val response = call.execute()
            if (!response.isSuccessful && response.code != 206) {
                // If range request failed (e.g. 416), restart from 0
                if (response.code == 416) {
                    partFile.delete()
                    existingBytes = 0L
                    downloadModelInternal(model)
                    return@withContext
                }
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw Exception("Empty response body from HuggingFace")
            val contentLength = body.contentLength()
            val totalBytes = if (contentLength > 0) existingBytes + contentLength else model.estimatedSizeBytes

            inputStream = body.byteStream()
            outputStream = FileOutputStream(partFile, existingBytes > 0)

            val buffer = ByteArray(64 * 1024) // 64KB buffer
            var bytesRead = 0
            var lastUpdateMs = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L
            var stoppedEarly = false

            while (coroutineContext.isActive && inputStream.read(buffer).also { bytesRead = it } != -1) {
                val currentStatus = _models.value.find { it.id == model.id }?.status
                if (currentStatus == DownloadStatus.PAUSED || currentStatus == DownloadStatus.NOT_DOWNLOADED || !coroutineContext.isActive) {
                    stoppedEarly = true
                    break
                }

                outputStream.write(buffer, 0, bytesRead)
                existingBytes += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastUpdateMs
                if (elapsed >= 350) {
                    val statusCheck = _models.value.find { it.id == model.id }?.status
                    if (statusCheck == DownloadStatus.PAUSED || statusCheck == DownloadStatus.NOT_DOWNLOADED || !coroutineContext.isActive) {
                        stoppedEarly = true
                        break
                    }

                    val speed = if (elapsed > 0) (bytesSinceLastUpdate * 1000L) / elapsed else 0L
                    val progress = (existingBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

                    updateModel(model.id) {
                        it.copy(
                            downloadProgress = progress,
                            downloadedBytes = existingBytes,
                            totalBytes = totalBytes,
                            downloadSpeedBps = speed,
                            status = DownloadStatus.DOWNLOADING
                        )
                    }
                    lastUpdateMs = now
                    bytesSinceLastUpdate = 0L
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null
            inputStream.close()
            inputStream = null

            if (!coroutineContext.isActive || stoppedEarly) {
                Log.d(tag, "Download cleanly halted due to pause/cancel for ${model.name}")
                val currentStatus = _models.value.find { it.id == model.id }?.status
                if (currentStatus != DownloadStatus.NOT_DOWNLOADED) {
                    updateModel(model.id) {
                        it.copy(status = DownloadStatus.PAUSED, downloadSpeedBps = 0L)
                    }
                }
                return@withContext
            }

            // Atomically rename .part to final .gguf
            if (finalFile.exists()) finalFile.delete()
            val renamed = partFile.renameTo(finalFile)
            if (!renamed) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }

            Log.d(tag, "Model downloaded successfully to persistent storage: ${finalFile.absolutePath} (${finalFile.length()} bytes)")

            updateModel(model.id) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    downloadProgress = 1f,
                    downloadedBytes = finalFile.length(),
                    totalBytes = finalFile.length(),
                    downloadSpeedBps = 0L,
                    localFilePath = finalFile.absolutePath,
                    localFileSize = finalFile.length(),
                    isActive = _models.value.none { m -> m.isActive && m.status == DownloadStatus.COMPLETED }
                )
            }
            refreshHardwareSpec()

        } catch (ce: CancellationException) {
            Log.d(tag, "Download paused/cancelled for ${model.name}")
            val currentStatus = _models.value.find { it.id == model.id }?.status
            if (currentStatus != DownloadStatus.NOT_DOWNLOADED) {
                updateModel(model.id) {
                    it.copy(status = DownloadStatus.PAUSED, downloadSpeedBps = 0L)
                }
            }
        } catch (e: Exception) {
            val isExplicitCancel = !coroutineContext.isActive ||
                    e.message?.contains("Canceled", ignoreCase = true) == true ||
                    e.message?.contains("Socket closed", ignoreCase = true) == true

            val currentStatus = _models.value.find { it.id == model.id }?.status
            if (isExplicitCancel || currentStatus == DownloadStatus.PAUSED || currentStatus == DownloadStatus.NOT_DOWNLOADED) {
                Log.d(tag, "Download stopped cleanly: ${e.message}")
                if (currentStatus != DownloadStatus.NOT_DOWNLOADED) {
                    updateModel(model.id) {
                        it.copy(status = DownloadStatus.PAUSED, downloadSpeedBps = 0L)
                    }
                }
            } else {
                Log.e(tag, "Download failed for ${model.name}: ${e.message}")
                updateModel(model.id) {
                    it.copy(
                        status = DownloadStatus.ERROR,
                        errorMessage = e.message ?: "Network error during download",
                        downloadSpeedBps = 0L
                    )
                }
            }
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            activeCalls.remove(model.id)
            activeDownloadJobs.remove(model.id)
        }
    }

    private fun updateModel(modelId: String, transform: (QwenGgufModel) -> QwenGgufModel) {
        _models.value = _models.value.map {
            if (it.id == modelId) transform(it) else it
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }

        fun formatSpeed(bytesPerSec: Long): String {
            if (bytesPerSec <= 0) return "0 KB/s"
            val kb = bytesPerSec / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format(java.util.Locale.US, "%.1f MB/s", mb)
            } else {
                String.format(java.util.Locale.US, "%.0f KB/s", kb)
            }
        }
    }
}
