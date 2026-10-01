/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: AiModelDownloader.kt
 *
 * Commentary / Architectural Overview:
 * High-performance background downloader for Qwen GGUF model files:
 * - Stores files in app-isolated external storage: context.getExternalFilesDir("ai_models")
 * - Supports HTTP range resumption for large multi-gigabyte models.
 * - Reports real-time byte counts, progress percent, and transfer speed (MB/s).
 * - Verifies storage availability before commencing download to avoid storage exhaustion.
 * - Computes device RAM specs to indicate hardware compatibility for each model tier.
 */

package com.example.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AiModelDownloader(private val context: Context) {
    private val tag = "AiModelDownloader"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val activeDownloadJobs = ConcurrentHashMap<String, Job>()

    private val _models = MutableStateFlow<List<QwenGgufModel>>(emptyList())
    val models: StateFlow<List<QwenGgufModel>> = _models.asStateFlow()

    private val _hardwareSpec = MutableStateFlow(computeHardwareSpec())
    val hardwareSpec: StateFlow<DeviceHardwareSpec> = _hardwareSpec.asStateFlow()

    init {
        initializeModelsList()
    }

    private fun getModelsDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), "ai_models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
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
        val modelsDir = getModelsDirectory()

        val predefined = listOf(
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

        // Inspect filesystem to populate existing downloads
        val resolved = predefined.map { model ->
            val targetFile = File(modelsDir, model.localFileName)
            if (targetFile.exists() && targetFile.length() > 100_000L) {
                model.copy(
                    status = DownloadStatus.COMPLETED,
                    downloadProgress = 1f,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    localFilePath = targetFile.absolutePath,
                    localFileSize = targetFile.length()
                )
            } else {
                val partFile = File(modelsDir, "${model.localFileName}.part")
                if (partFile.exists() && partFile.length() > 0) {
                    val progress = (partFile.length().toFloat() / model.estimatedSizeBytes.toFloat()).coerceIn(0f, 0.99f)
                    model.copy(
                        status = DownloadStatus.PAUSED,
                        downloadProgress = progress,
                        downloadedBytes = partFile.length(),
                        totalBytes = model.estimatedSizeBytes,
                        localFilePath = partFile.absolutePath,
                        localFileSize = partFile.length()
                    )
                } else {
                    model
                }
            }
        }

        // Set first completed model as active by default if none is active
        var activeFound = false
        val withActive = resolved.map {
            if (!activeFound && it.status == DownloadStatus.COMPLETED) {
                activeFound = true
                it.copy(isActive = true)
            } else {
                it
            }
        }

        _models.value = withActive
    }

    fun startDownload(modelId: String) {
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

        val job = scope.launch {
            downloadModelInternal(model)
        }
        activeDownloadJobs[modelId] = job
    }

    fun pauseDownload(modelId: String) {
        activeDownloadJobs[modelId]?.cancel()
        activeDownloadJobs.remove(modelId)
        updateModel(modelId) {
            it.copy(status = DownloadStatus.PAUSED, downloadSpeedBps = 0L)
        }
    }

    fun deleteModel(modelId: String) {
        pauseDownload(modelId)
        val model = _models.value.find { it.id == modelId } ?: return
        val modelsDir = getModelsDirectory()
        val targetFile = File(modelsDir, model.localFileName)
        val partFile = File(modelsDir, "${model.localFileName}.part")

        if (targetFile.exists()) targetFile.delete()
        if (partFile.exists()) partFile.delete()

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
        refreshHardwareSpec()
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

        try {
            val requestBuilder = Request.Builder()
                .url(model.downloadUrl)
                .header("User-Agent", "PeerLink-Android/1.0 (Mobile GGUF Engine)")

            if (existingBytes > 0) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
                Log.d(tag, "Resuming download of ${model.name} from byte $existingBytes")
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
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

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(partFile, existingBytes > 0)

            val buffer = ByteArray(64 * 1024) // 64KB buffer
            var bytesRead: Int
            var lastUpdateMs = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                existingBytes += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastUpdateMs
                if (elapsed >= 350) {
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
            inputStream.close()

            // Atomically rename .part to final .gguf
            if (finalFile.exists()) finalFile.delete()
            val renamed = partFile.renameTo(finalFile)
            if (!renamed) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }

            Log.d(tag, "Model downloaded successfully: ${finalFile.absolutePath} (${finalFile.length()} bytes)")

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
            updateModel(model.id) {
                it.copy(status = DownloadStatus.PAUSED, downloadSpeedBps = 0L)
            }
        } catch (e: Exception) {
            Log.e(tag, "Download failed for ${model.name}: ${e.message}")
            updateModel(model.id) {
                it.copy(
                    status = DownloadStatus.ERROR,
                    errorMessage = e.message ?: "Network error during download",
                    downloadSpeedBps = 0L
                )
            }
        } finally {
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
