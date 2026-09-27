package com.example.filetransfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.repository.MessageRepository
import com.example.security.CryptoManager
import com.example.security.DeviceIdentity
import com.example.transport.TransportManager
import com.example.transport.model.P2PPacket
import com.example.transport.model.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val FILE_CHUNK_SIZE = 512 * 1024 // 512 KB
private const val FILE_PIPELINE_DEPTH = 8

data class FileTransferProgress(
    val transferId: String,
    val fileName: String,
    val totalBytes: Long,
    val transferredBytes: Long,
    val isOutgoing: Boolean,
    val isComplete: Boolean = false,
    val error: String? = null,
    val localFilePath: String? = null
) {
    val progressPercent: Float
        get() = if (totalBytes > 0) {
            (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

private data class IncomingTransferState(
    val transferId: String,
    val fileName: String,
    val fileSize: Long,
    val totalChunks: Int,
    val senderId: String,
    val senderName: String,
    val outputFile: File,
    val randomAccessFile: RandomAccessFile,
    val receivedChunkIndices: MutableSet<Int> = ConcurrentHashMap.newKeySet(),
    var receivedBytes: Long = 0L
)

class FileTransferManager(
    private val context: Context,
    private val transportManager: TransportManager,
    private val messageRepository: MessageRepository,
    private val cryptoManager: CryptoManager,
    private val deviceIdentity: DeviceIdentity
) {
    private val tag = "FileTransferManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _transfers = MutableStateFlow<Map<String, FileTransferProgress>>(emptyMap())
    val transfers: StateFlow<Map<String, FileTransferProgress>> = _transfers.asStateFlow()

    private val _fileReceivedEvent = MutableSharedFlow<FileTransferProgress>(extraBufferCapacity = 16)
    val fileReceivedEvent: SharedFlow<FileTransferProgress> = _fileReceivedEvent.asSharedFlow()

    private val incomingTransfers = ConcurrentHashMap<String, IncomingTransferState>()

    init {
        scope.launch {
            transportManager.incomingPackets.collect { (packet, remoteIp) ->
                handlePacket(packet, remoteIp)
            }
        }
    }

    suspend fun sendFile(
        uri: Uri,
        targetPeerId: String,
        targetPeerName: String,
        targetPeerIp: String
    ): String = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val transferId = UUID.randomUUID().toString()
        val resolver = context.contentResolver

        var fileName = "file_${System.currentTimeMillis()}"
        var fileSize = -1L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        val mimeType = resolver.getType(uri) ?: getMimeType(fileName)
        val totalChunks = when {
            fileSize > 0L -> ((fileSize + FILE_CHUNK_SIZE - 1L) / FILE_CHUNK_SIZE).toInt()
            fileSize == 0L -> 1
            else -> -1 // Unknown size; receiver completes from the binary isLast flag.
        }

        updateTransfer(
            FileTransferProgress(
                transferId = transferId,
                fileName = fileName,
                totalBytes = fileSize,
                transferredBytes = 0L,
                isOutgoing = true,
                localFilePath = uri.toString()
            )
        )

        val messageId = transferId
        messageRepository.saveOutgoingMessage(
            id = messageId,
            peerId = targetPeerId,
            peerName = targetPeerName,
            myId = deviceIdentity.deviceId,
            myName = deviceIdentity.deviceName,
            content = fileName,
            peerIp = targetPeerIp,
            status = "SENDING"
        )

        val startPacket = P2PPacket(
            packetId = UUID.randomUUID().toString(),
            type = PacketType.FILE_START,
            senderId = deviceIdentity.deviceId,
            senderName = deviceIdentity.deviceName,
            targetId = targetPeerId,
            payload = transferId,
            extraData = mapOf(
                "fileName" to fileName,
                "fileSize" to fileSize.toString(),
                "totalChunks" to totalChunks.toString(),
                "mimeType" to mimeType
            )
        )

        try {
            if (!transportManager.sendPacketToIp(targetPeerIp, startPacket)) {
                throw IOException("Unable to connect to peer")
            }

            resolver.openInputStream(uri)?.use { inputStream ->
                coroutineScope {
                    val permits = Semaphore(FILE_PIPELINE_DEPTH)
                    val inFlight = java.util.ArrayDeque<Deferred<Int>>(FILE_PIPELINE_DEPTH)
                    var completedBytes = 0L
                    var chunkIndex = 0

                    // One-chunk look-ahead makes isLast reliable even when the
                    // content provider does not expose a file size or when the
                    // size is an exact multiple of FILE_CHUNK_SIZE.
                    var currentBuffer = ByteArray(FILE_CHUNK_SIZE)
                    var currentBytes = inputStream.read(currentBuffer)

                    if (currentBytes == -1) {
                        permits.acquire()
                        val lastJob = async {
                            try {
                                sendChunk(
                                    targetPeerId = targetPeerId,
                                    targetPeerIp = targetPeerIp,
                                    transferId = transferId,
                                    chunkIndex = 0,
                                    chunkBytes = ByteArray(0),
                                    isLast = true
                                )
                                0
                            } finally {
                                permits.release()
                            }
                        }
                        inFlight.addLast(lastJob)
                    }

                    while (currentBytes != -1) {
                        val nextBuffer = ByteArray(FILE_CHUNK_SIZE)
                        val nextBytes = inputStream.read(nextBuffer)
                        val isLast = nextBytes == -1
                        val chunkData = if (currentBytes == currentBuffer.size) {
                            currentBuffer
                        } else {
                            currentBuffer.copyOf(currentBytes)
                        }
                        val currentChunkIndex = chunkIndex

                        permits.acquire()
                        val job = async {
                            try {
                                sendChunk(
                                    targetPeerId = targetPeerId,
                                    targetPeerIp = targetPeerIp,
                                    transferId = transferId,
                                    chunkIndex = currentChunkIndex,
                                    chunkBytes = chunkData,
                                    isLast = isLast
                                )
                            } finally {
                                permits.release()
                            }
                        }
                        inFlight.addLast(job)
                        chunkIndex++

                        currentBuffer = nextBuffer
                        currentBytes = nextBytes

                        // Keep at most FILE_PIPELINE_DEPTH buffers/jobs alive.
                        if (inFlight.size >= FILE_PIPELINE_DEPTH) {
                            completedBytes += inFlight.removeFirst().await()
                            updateOutgoingProgress(
                                transferId = transferId,
                                fileName = fileName,
                                fileSize = fileSize,
                                transferredBytes = completedBytes,
                                isComplete = false,
                                uri = uri
                            )
                        }
                    }

                    while (inFlight.isNotEmpty()) {
                        completedBytes += inFlight.removeFirst().await()
                        updateOutgoingProgress(
                            transferId = transferId,
                            fileName = fileName,
                            fileSize = fileSize,
                            transferredBytes = completedBytes,
                            isComplete = inFlight.isEmpty(),
                            uri = uri
                        )
                    }
                }
            } ?: throw IOException("Unable to open selected file")

            messageRepository.updateMessageStatus(messageId, "SENT")
            Log.d(tag, "File $fileName successfully sent ($fileSize bytes)")
        } catch (e: Exception) {
            Log.e(tag, "Error transmitting file: ${e.message}", e)
            updateTransfer(
                FileTransferProgress(
                    transferId = transferId,
                    fileName = fileName,
                    totalBytes = fileSize,
                    transferredBytes = _transfers.value[transferId]?.transferredBytes ?: 0L,
                    isOutgoing = true,
                    error = e.message,
                    localFilePath = uri.toString()
                )
            )
            messageRepository.updateMessageStatus(messageId, "FAILED")
        }

        transferId
    }

    private suspend fun sendChunk(
        targetPeerId: String,
        targetPeerIp: String,
        transferId: String,
        chunkIndex: Int,
        chunkBytes: ByteArray,
        isLast: Boolean
    ): Int {
        val chunkPacket = P2PPacket(
            packetId = UUID.randomUUID().toString(),
            type = PacketType.FILE_CHUNK,
            senderId = deviceIdentity.deviceId,
            senderName = deviceIdentity.deviceName,
            targetId = targetPeerId,
            payload = transferId,
            binaryPayload = chunkBytes,
            extraData = mapOf(
                "transferId" to transferId,
                "chunkIndex" to chunkIndex.toString(),
                "isLast" to isLast.toString()
            )
        )

        if (!transportManager.sendPacketToIp(targetPeerIp, chunkPacket)) {
            throw IOException("Failed to send chunk $chunkIndex")
        }
        return chunkBytes.size
    }

    private fun updateOutgoingProgress(
        transferId: String,
        fileName: String,
        fileSize: Long,
        transferredBytes: Long,
        isComplete: Boolean,
        uri: Uri
    ) {
        updateTransfer(
            FileTransferProgress(
                transferId = transferId,
                fileName = fileName,
                totalBytes = fileSize,
                transferredBytes = transferredBytes,
                isOutgoing = true,
                isComplete = isComplete,
                localFilePath = uri.toString()
            )
        )
    }

    private suspend fun handlePacket(packet: P2PPacket, remoteIp: String) {
        when (packet.type) {
            PacketType.FILE_START -> {
                val transferId = packet.payload
                val fileName = packet.extraData["fileName"]
                    ?: "received_file_${System.currentTimeMillis()}"
                val fileSize = packet.extraData["fileSize"]?.toLongOrNull() ?: -1L
                val totalChunks = packet.extraData["totalChunks"]?.toIntOrNull() ?: -1

                val downloadsDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.filesDir,
                    "Received"
                )
                downloadsDir.mkdirs()

                var targetFile = File(downloadsDir, fileName)
                var counter = 1
                while (targetFile.exists()) {
                    val dot = fileName.lastIndexOf('.')
                    targetFile = if (dot != -1) {
                        File(
                            downloadsDir,
                            "${fileName.substring(0, dot)}_$counter.${fileName.substring(dot + 1)}"
                        )
                    } else {
                        File(downloadsDir, "${fileName}_$counter")
                    }
                    counter++
                }

                val raf = RandomAccessFile(targetFile, "rw")
                if (fileSize >= 0L) {
                    raf.setLength(fileSize)
                }

                val state = IncomingTransferState(
                    transferId = transferId,
                    fileName = targetFile.name,
                    fileSize = fileSize,
                    totalChunks = totalChunks,
                    senderId = packet.senderId,
                    senderName = packet.senderName,
                    outputFile = targetFile,
                    randomAccessFile = raf
                )
                incomingTransfers[transferId] = state

                updateTransfer(
                    FileTransferProgress(
                        transferId = transferId,
                        fileName = targetFile.name,
                        totalBytes = fileSize,
                        transferredBytes = 0L,
                        isOutgoing = false,
                        localFilePath = targetFile.absolutePath
                    )
                )
            }

            PacketType.FILE_CHUNK -> {
                val transferId = packet.extraData["transferId"] ?: packet.payload
                val chunkBytes = packet.binaryPayload ?: return
                val chunkIndex = packet.extraData["chunkIndex"]?.toIntOrNull() ?: return
                val isLast = packet.extraData["isLast"]?.toBooleanStrictOrNull() == true
                val state = incomingTransfers[transferId] ?: return

                try {
                    val isComplete: Boolean
                    val receivedBytes: Long

                    synchronized(state) {
                        if (chunkIndex < 0 || (state.totalChunks > 0 && chunkIndex >= state.totalChunks)) {
                            throw IOException("Invalid chunk index $chunkIndex")
                        }

                        if (state.fileSize >= 0L) {
                            val offset = chunkIndex.toLong() * FILE_CHUNK_SIZE
                            if (offset > state.fileSize || offset + chunkBytes.size > state.fileSize) {
                                throw IOException("Chunk exceeds declared file size")
                            }
                        }

                        // Duplicate chunks are ignored without double-counting.
                        if (state.receivedChunkIndices.add(chunkIndex)) {
                            val offset = chunkIndex.toLong() * FILE_CHUNK_SIZE
                            state.randomAccessFile.seek(offset)
                            state.randomAccessFile.write(chunkBytes)
                            state.receivedBytes += chunkBytes.size
                        }

                        receivedBytes = state.receivedBytes
                        isComplete = isLast ||
                            (state.totalChunks > 0 && state.receivedChunkIndices.size >= state.totalChunks) ||
                            (state.fileSize >= 0L && receivedBytes >= state.fileSize)
                    }

                    updateTransfer(
                        FileTransferProgress(
                            transferId = transferId,
                            fileName = state.fileName,
                            totalBytes = state.fileSize,
                            transferredBytes = receivedBytes,
                            isOutgoing = false,
                            isComplete = isComplete,
                            localFilePath = state.outputFile.absolutePath
                        )
                    )

                    if (isComplete) {
                        synchronized(state) {
                            state.randomAccessFile.fd.sync()
                            state.randomAccessFile.close()
                        }
                        incomingTransfers.remove(transferId, state)

                        messageRepository.saveIncomingMessage(
                            id = transferId,
                            peerId = state.senderId,
                            peerName = state.senderName,
                            content = "Received file: ${state.fileName}",
                            senderId = state.senderId,
                            senderName = state.senderName,
                            timestamp = System.currentTimeMillis(),
                            peerIp = remoteIp
                        )

                        val ack = P2PPacket(
                            packetId = UUID.randomUUID().toString(),
                            type = PacketType.FILE_ACK,
                            senderId = deviceIdentity.deviceId,
                            senderName = deviceIdentity.deviceName,
                            targetId = state.senderId,
                            payload = transferId
                        )
                        transportManager.sendPacketToIp(remoteIp, ack)

                        val completedProgress = FileTransferProgress(
                            transferId = transferId,
                            fileName = state.fileName,
                            totalBytes = state.fileSize,
                            transferredBytes = receivedBytes,
                            isOutgoing = false,
                            isComplete = true,
                            localFilePath = state.outputFile.absolutePath
                        )
                        _fileReceivedEvent.emit(completedProgress)
                        Log.d(
                            tag,
                            "Incoming file ${state.fileName} completed ($receivedBytes bytes)"
                        )
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error writing chunk: ${e.message}", e)
                    incomingTransfers.remove(transferId, state)
                    try {
                        synchronized(state) {
                            state.randomAccessFile.close()
                        }
                    } catch (_: Exception) {}
                    updateTransfer(
                        FileTransferProgress(
                            transferId = transferId,
                            fileName = state.fileName,
                            totalBytes = state.fileSize,
                            transferredBytes = state.receivedBytes,
                            isOutgoing = false,
                            error = e.message,
                            localFilePath = state.outputFile.absolutePath
                        )
                    )
                }
            }

            PacketType.FILE_ACK -> {
                val transferId = packet.payload
                _transfers.value[transferId]?.let { current ->
                    updateTransfer(current.copy(isComplete = true))
                    messageRepository.updateMessageStatus(transferId, "DELIVERED")
                }
            }

            else -> {}
        }
    }

    private fun updateTransfer(progress: FileTransferProgress) {
        val map = _transfers.value.toMutableMap()
        map[progress.transferId] = progress
        _transfers.value = map
    }

    fun openFile(localFilePath: String): Boolean {
        return try {
            val file = File(localFilePath)
            if (!file.exists()) return false

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = getMimeType(file.name)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error opening file: ${e.message}")
            false
        }
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }
}
