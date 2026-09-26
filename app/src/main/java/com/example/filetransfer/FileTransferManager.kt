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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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
        get() = if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

private data class IncomingTransferState(
    val transferId: String,
    val fileName: String,
    val fileSize: Long,
    val totalChunks: Int,
    val senderId: String,
    val senderName: String,
    val outputFile: File,
    val outputStream: FileOutputStream,
    var receivedChunks: Int = 0,
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

    private val chunkSize = 64 * 1024 // 64 KB chunks for smooth socket transmission

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
    ): String = withContext(Dispatchers.IO) {
        val transferId = UUID.randomUUID().toString()
        val resolver = context.contentResolver

        var fileName = "file_${System.currentTimeMillis()}"
        var fileSize = 0L

        // Query file metadata
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }

        val mimeType = resolver.getType(uri) ?: getMimeType(fileName)
        val totalChunks = if (fileSize > 0) ((fileSize + chunkSize - 1) / chunkSize).toInt() else 1

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

        // Save message in local database as SENDING
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

        // Send FILE_START packet
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
        transportManager.sendPacketToIp(targetPeerIp, startPacket)

        // Stream Chunks
        try {
            resolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(chunkSize)
                var bytesRead: Int
                var chunkIndex = 0
                var totalTransferred = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val chunkData = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)

                    val chunkPacket = P2PPacket(
                        packetId = UUID.randomUUID().toString(),
                        type = PacketType.FILE_CHUNK,
                        senderId = deviceIdentity.deviceId,
                        senderName = deviceIdentity.deviceName,
                        targetId = targetPeerId,
                        payload = transferId,
                        binaryPayload = chunkData,
                        extraData = mapOf(
                            "transferId" to transferId,
                            "chunkIndex" to chunkIndex.toString()
                        )
                    )

                    transportManager.sendPacketToIp(targetPeerIp, chunkPacket)

                    chunkIndex++
                    totalTransferred += bytesRead
                    updateTransfer(
                        FileTransferProgress(
                            transferId = transferId,
                            fileName = fileName,
                            totalBytes = fileSize,
                            transferredBytes = totalTransferred,
                            isOutgoing = true,
                            isComplete = totalTransferred >= fileSize,
                            localFilePath = uri.toString()
                        )
                    )
                }
            }
            messageRepository.updateMessageStatus(messageId, "SENT")
            Log.d(tag, "File $fileName successfully sent ($fileSize bytes)")
        } catch (e: Exception) {
            Log.e(tag, "Error transmitting file: ${e.message}")
            updateTransfer(
                FileTransferProgress(
                    transferId = transferId,
                    fileName = fileName,
                    totalBytes = fileSize,
                    transferredBytes = 0L,
                    isOutgoing = true,
                    error = e.message
                )
            )
            messageRepository.updateMessageStatus(messageId, "FAILED")
        }

        return@withContext transferId
    }

    private suspend fun handlePacket(packet: P2PPacket, remoteIp: String) {
        when (packet.type) {
            PacketType.FILE_START -> {
                val transferId = packet.payload
                val fileName = packet.extraData["fileName"] ?: "received_file_${System.currentTimeMillis()}"
                val fileSize = packet.extraData["fileSize"]?.toLongOrNull() ?: 0L
                val totalChunks = packet.extraData["totalChunks"]?.toIntOrNull() ?: 1

                val downloadsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "Received")
                downloadsDir.mkdirs()

                // Avoid collision
                var targetFile = File(downloadsDir, fileName)
                var counter = 1
                while (targetFile.exists()) {
                    val dot = fileName.lastIndexOf('.')
                    targetFile = if (dot != -1) {
                        File(downloadsDir, "${fileName.substring(0, dot)}_$counter.${fileName.substring(dot + 1)}")
                    } else {
                        File(downloadsDir, "${fileName}_$counter")
                    }
                    counter++
                }

                val fos = FileOutputStream(targetFile)
                val state = IncomingTransferState(
                    transferId = transferId,
                    fileName = targetFile.name,
                    fileSize = fileSize,
                    totalChunks = totalChunks,
                    senderId = packet.senderId,
                    senderName = packet.senderName,
                    outputFile = targetFile,
                    outputStream = fos
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
                val state = incomingTransfers[transferId] ?: return

                try {
                    state.outputStream.write(chunkBytes)
                    state.receivedBytes += chunkBytes.size
                    state.receivedChunks++

                    val isComplete = state.receivedBytes >= state.fileSize || state.receivedChunks >= state.totalChunks

                    updateTransfer(
                        FileTransferProgress(
                            transferId = transferId,
                            fileName = state.fileName,
                            totalBytes = state.fileSize,
                            transferredBytes = state.receivedBytes,
                            isOutgoing = false,
                            isComplete = isComplete,
                            localFilePath = state.outputFile.absolutePath
                        )
                    )

                    if (isComplete) {
                        state.outputStream.flush()
                        state.outputStream.close()
                        incomingTransfers.remove(transferId)

                        // Save message in local Room database
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

                        // Send ACK
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
                            transferredBytes = state.receivedBytes,
                            isOutgoing = false,
                            isComplete = true,
                            localFilePath = state.outputFile.absolutePath
                        )
                        _fileReceivedEvent.emit(completedProgress)
                        Log.d(tag, "Incoming file ${state.fileName} completed (${state.receivedBytes} bytes)")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error writing chunk: ${e.message}")
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
