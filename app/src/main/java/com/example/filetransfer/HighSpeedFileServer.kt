package com.example.filetransfer

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.example.data.repository.MessageRepository
import com.example.diagnostic.AppDiagnostics
import com.example.security.DeviceIdentity
import com.example.transport.WakeLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.Channels

/**
 * Pure raw-socket binary TCP server for P2P file transfers (Port 8989).
 * Saves received files directly to /storage/emulated/0/Download/PeerLink/{file}
 * Max-speed line-rate optimizations:
 * 1. 16MB SO_RCVBUF TCP window scaling to eliminate ACK stalls.
 * 2. 8MB direct off-heap native ByteBuffer bypassing JVM heap and garbage collection.
 * 3. Continuous Active Mode (CAM) Wi-Fi lock and CPU WakeLock during transfers.
 * 4. Automatic MediaScanner registration.
 */
class HighSpeedFileServer(
    private val context: Context,
    private val deviceIdentity: DeviceIdentity,
    private val messageRepository: MessageRepository,
    private val wakeLockManager: WakeLockManager,
    private val onProgressUpdate: (FileTransferProgress) -> Unit,
    private val onFileCompleted: (FileTransferProgress) -> Unit,
    private val port: Int = 8989
) {
    private val tag = "HighSpeedFileServer"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    val isRunning: Boolean
        get() = serverSocket != null && !serverSocket!!.isClosed

    fun start() {
        if (isRunning) return
        serverJob = scope.launch {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.receiveBufferSize = 16 * 1024 * 1024 // 16MB receive buffer window
                server.bind(InetSocketAddress("0.0.0.0", port), 50)
                serverSocket = server
                AppDiagnostics.log(tag, "Raw High-Speed Server listening on 0.0.0.0:$port (16MB TCP buffer)")

                while (isActive && !server.isClosed) {
                    try {
                        val client = server.accept()
                        launch {
                            handleRawStream(client)
                        }
                    } catch (e: Exception) {
                        if (!server.isClosed) {
                            Log.w(tag, "Accept error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                AppDiagnostics.log(tag, "HighSpeedFileServer startup error: ${e.message}", e)
            }
        }
    }

    private suspend fun handleRawStream(socket: Socket) {
        val remoteIp = socket.inetAddress.hostAddress ?: ""
        wakeLockManager.acquire("HighSpeedReceive")
        try {
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.receiveBufferSize = 16 * 1024 * 1024
            socket.sendBufferSize = 16 * 1024 * 1024
            socket.trafficClass = 0x10 // IPTOS_THROUGHPUT
            socket.setPerformancePreferences(0, 1, 2) // Priority: Bandwidth > Latency > Connection time

            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val dis = DataInputStream(input)

            // Read Magic: 'PLFT' (0x504C4654)
            val magic = dis.readInt()
            if (magic != 0x504C4654) {
                Log.w(tag, "Invalid magic $magic from $remoteIp")
                socket.close()
                return
            }

            // Read transferId
            val transferIdLen = dis.readInt()
            val transferIdBytes = ByteArray(transferIdLen)
            dis.readFully(transferIdBytes)
            val transferId = String(transferIdBytes, Charsets.UTF_8)

            // Read fileName
            val fileNameLen = dis.readInt()
            val fileNameBytes = ByteArray(fileNameLen)
            dis.readFully(fileNameBytes)
            val rawFileName = String(fileNameBytes, Charsets.UTF_8)

            // Read fileSize (8 bytes)
            val fileSize = dis.readLong()

            AppDiagnostics.log(tag, "Incoming RAW file: $rawFileName ($fileSize bytes) from $remoteIp")

            // Destination: /storage/emulated/0/Download/PeerLink/
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val peerlinkFolder = File(publicDownloads, "PeerLink")
            if (!peerlinkFolder.exists()) {
                peerlinkFolder.mkdirs()
            }
            val targetDir = if (peerlinkFolder.exists() && (peerlinkFolder.canWrite() || peerlinkFolder.isDirectory)) {
                peerlinkFolder
            } else {
                val fallbackFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "PeerLink")
                fallbackFolder.mkdirs()
                fallbackFolder
            }

            // Storage space pre-check
            try {
                val stat = android.os.StatFs(targetDir.absolutePath)
                val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
                if (fileSize > 0 && availableBytes < fileSize + (30L * 1024 * 1024)) {
                    AppDiagnostics.log(tag, "Insufficient space: Requires $fileSize, Available $availableBytes")
                    val dos = DataOutputStream(output)
                    dos.writeInt(0x4E4F5350) // 'NOSP'
                    dos.flush()
                    socket.close()
                    return
                }
            } catch (_: Exception) {}

            // Send Ready acknowledgement: 'OKAY' (0x4F4B4159)
            val dosInit = DataOutputStream(output)
            dosInit.writeInt(0x4F4B4159)
            dosInit.flush()

            var targetFile = File(targetDir, rawFileName)
            var counter = 1
            while (targetFile.exists()) {
                val dot = rawFileName.lastIndexOf('.')
                targetFile = if (dot != -1) {
                    File(targetDir, "${rawFileName.substring(0, dot)}_$counter.${rawFileName.substring(dot + 1)}")
                } else {
                    File(targetDir, "${rawFileName}_$counter")
                }
                counter++
            }

            var progress = FileTransferProgress(
                transferId = transferId,
                fileName = targetFile.name,
                totalBytes = fileSize,
                transferredBytes = 0L,
                isOutgoing = false,
                localFilePath = targetFile.absolutePath
            )
            onProgressUpdate(progress)

            // High-speed native disk streaming using direct off-heap native ByteBuffer (8MB)
            val socketChannel = Channels.newChannel(input)
            val fileChannel = FileOutputStream(targetFile).channel
            val directBuffer = ByteBuffer.allocateDirect(8 * 1024 * 1024)
            var totalRead = 0L
            var lastUiUpdateTime = System.currentTimeMillis()
            var bytesSinceLastSpeedCalc = 0L
            var currentSpeedBps = 0L

            fileChannel.use { fc ->
                while (totalRead < fileSize) {
                    directBuffer.clear()
                    val toReadNow = minOf(directBuffer.capacity().toLong(), fileSize - totalRead).toInt()
                    directBuffer.limit(toReadNow)
                    val bytesRead = socketChannel.read(directBuffer)
                    if (bytesRead == -1) break
                    directBuffer.flip()
                    while (directBuffer.hasRemaining()) {
                        fc.write(directBuffer)
                    }
                    totalRead += bytesRead
                    bytesSinceLastSpeedCalc += bytesRead
                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastUiUpdateTime
                    if (timeDiff >= 100 || totalRead >= fileSize) { // Responsive 10Hz UI update
                        if (timeDiff > 0) {
                            currentSpeedBps = (bytesSinceLastSpeedCalc * 1000L) / timeDiff
                            bytesSinceLastSpeedCalc = 0L
                        }
                        lastUiUpdateTime = now
                        progress = progress.copy(
                            transferredBytes = totalRead,
                            isComplete = totalRead >= fileSize,
                            speedBytesPerSec = currentSpeedBps
                        )
                        onProgressUpdate(progress)
                    }
                }
                fc.force(false)
            }

            if (totalRead >= fileSize) {
                // Send completion ACK magic: 'PLAK' (0x504C414B)
                val dos = DataOutputStream(output)
                dos.writeInt(0x504C414B)
                dos.flush()

                progress = progress.copy(
                    transferredBytes = totalRead,
                    isComplete = true,
                    speedBytesPerSec = 0L
                )
                onProgressUpdate(progress)
                onFileCompleted(progress)

                // Register file in Android MediaStore so file explorers and gallery immediately see it
                try {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        null
                    ) { path, _ ->
                        Log.d(tag, "MediaScanner registered: $path")
                    }
                } catch (_: Exception) {}

                messageRepository.saveIncomingMessage(
                    id = transferId,
                    peerId = remoteIp,
                    peerName = "Peer-$remoteIp",
                    content = "Received file: ${targetFile.name}",
                    senderId = remoteIp,
                    senderName = "Peer",
                    timestamp = System.currentTimeMillis(),
                    peerIp = remoteIp
                )
                AppDiagnostics.log(tag, "File saved to ${targetFile.absolutePath} ($totalRead bytes)")
            } else {
                throw Exception("Stream terminated early: $totalRead of $fileSize bytes")
            }
        } catch (e: Exception) {
            AppDiagnostics.log(tag, "Raw stream receive failed: ${e.message}")
        } finally {
            wakeLockManager.release("HighSpeedReceive")
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            AppDiagnostics.log(tag, "HighSpeedFileServer stopped")
        } catch (e: Exception) {
            Log.w(tag, "Error stopping file server: ${e.message}")
        }
    }
}
