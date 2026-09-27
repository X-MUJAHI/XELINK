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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
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
    // RandomAccessFile instead of a plain append-only stream: chunks now arrive over a
    // sliding window (several in flight at once), so they can land slightly out of order.
    // Writing each chunk at its own byte offset makes ordering irrelevant and lets multiple
    // chunks be written without waiting on each other.
    val raf: RandomAccessFile,
    var receivedChunks: Int = 0,
    var receivedBytes: Long = 0L,
    val receivedChunkIndices: MutableSet<Int> = ConcurrentHashMap.newKeySet(),
    // Set once a frame with isLast=true is seen. Because sends are now pipelined,
    // several chunks race concurrently for the same socket's write lock, so the frame
    // carrying isLast=true is NOT guaranteed to be the one that *arrives* last - a
    // later-index chunk can win that race and reach the receiver before an
    // earlier-index one. Its chunkIndex + 1 reliably tells us the true total chunk
    // count the moment it shows up, but completion still has to wait until every
    // chunk up to that count has actually been received (see isComplete below) -
    // otherwise a fast-arriving final chunk would end the transfer while earlier
    // chunks are still in flight, silently leaving zero-filled gaps in the file.
    var expectedTotalChunks: Int? = null
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

    // Larger chunks now that chunks travel as raw binary frames (no JSON/Base64 overhead
    // per chunk), so fewer, bigger writes are used instead of many small ones.
    private val chunkSize = 512 * 1024 // 512 KB

    // How many chunks can be "in flight" (written to the socket but not yet necessarily
    // flushed through the OS) at once. Since sends are now cheap raw writes rather than
    // JSON round trips, overlapping several lets the sender keep the socket buffer full
    // instead of doing strict read-one/send-one/wait.
    private val pipelineDepth = 8

    init {
        scope.launch {
            transportManager.incomingPackets.collect { (packet, remoteIp) ->
                handlePacket(packet, remoteIp)
            }
        }
        scope.launch {
            transportManager.incomingFileChunks.collect { (frame, remoteIp) ->
                handleFileChunkFrame(frame, remoteIp)
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

        // Stream chunks as raw binary frames, pipelined so multiple chunks can be in
        // flight over the socket at once instead of waiting for each send to complete
        // before reading and sending the next one.
        try {
            resolver.openInputStream(uri)?.use { inputStream ->
                val semaphore = Semaphore(pipelineDepth)
                val inFlight = ArrayList<kotlinx.coroutines.Deferred<Unit>>()
                val transferredCounter = java.util.concurrent.atomic.AtomicLong(0L)
                var chunkIndex = 0

                // One-chunk read-ahead: whether the "current" chunk is the *last* one can only
                // be known once the *next* read comes back empty (true end-of-stream). fileSize
                // from the content resolver is not reliable enough to derive isLast from instead —
                // some providers (pipe-backed content:// streams, some cloud/share URIs) report 0
                // or an absent size, and readFullChunk() below also guards against a single
                // read() call returning fewer bytes than a full chunk even when more data is still
                // coming (common on pipe-backed streams), which the offset-based write on the
                // receiver side depends on to avoid leaving a gap in the reassembled file.
                var current: ByteArray? = readFullChunk(inputStream, chunkSize)

                while (current != null) {
                    val currentBytes = current
                    val next = readFullChunk(inputStream, chunkSize)
                    val isLast = next == null
                    val frame = P2PPacket.encodeChunkFrame(transferId, chunkIndex, isLast, currentBytes, 0, currentBytes.size)
                    val sentBytes = currentBytes.size

                    inFlight += scope.async {
                        semaphore.withPermit {
                            transportManager.sendFileChunkFrame(targetPeerIp, frame)
                            val nowTransferred = transferredCounter.addAndGet(sentBytes.toLong())
                            updateTransfer(
                                FileTransferProgress(
                                    transferId = transferId,
                                    fileName = fileName,
                                    totalBytes = fileSize,
                                    transferredBytes = nowTransferred,
                                    isOutgoing = true,
                                    isComplete = isLast,
                                    localFilePath = uri.toString()
                                )
                            )
                        }
                    }
                    chunkIndex++
                    current = next

                    // Keep the in-flight list from growing unboundedly on very large files;
                    // periodically drain completed sends while still overlapping new reads.
                    if (inFlight.size >= pipelineDepth * 4) {
                        inFlight.removeAll { it.isCompleted }
                    }
                }

                inFlight.awaitAll()

                // A genuinely empty (0-byte) file never enters the loop above, so the receiver
                // would otherwise wait forever for a chunk that never comes. Send one empty
                // "last" chunk so it still gets a completion signal.
                if (chunkIndex == 0) {
                    val emptyFrame = P2PPacket.encodeChunkFrame(transferId, 0, true, ByteArray(0), 0, 0)
                    transportManager.sendFileChunkFrame(targetPeerIp, emptyFrame)
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

                val raf = RandomAccessFile(targetFile, "rw")
                if (fileSize > 0) {
                    // Pre-allocate so out-of-order chunk writes (from the sender's pipelined
                    // sends) can seek to their own offset safely.
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
                    raf = raf
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

    /**
     * Handles an incoming raw binary file-chunk frame (fast path — see
     * P2PPacket.encodeChunkFrame). Writes at the chunk's own byte offset so chunks can be
     * received slightly out of order (the sender now pipelines multiple chunks at once).
     */
    private suspend fun handleFileChunkFrame(frame: com.example.transport.model.FileChunkFrame, remoteIp: String) {
        val state = incomingTransfers[frame.transferId] ?: return

        try {
            val offset = frame.chunkIndex.toLong() * chunkSize
            synchronized(state.raf) {
                state.raf.seek(offset)
                state.raf.write(frame.data)
            }

            // Guard against double-counting if a chunk is ever retransmitted.
            if (state.receivedChunkIndices.add(frame.chunkIndex)) {
                state.receivedChunks++
                state.receivedBytes += frame.data.size
            }

            if (frame.isLast) {
                state.expectedTotalChunks = frame.chunkIndex + 1
            }

            // Only complete once every chunk up to the known total has actually been
            // received - not merely because *a* frame tagged isLast has shown up, since
            // that frame can win the pipelined write-lock race and arrive before
            // earlier-index chunks do.
            val expectedTotal = state.expectedTotalChunks
            val isComplete = expectedTotal != null && state.receivedChunks >= expectedTotal

            updateTransfer(
                FileTransferProgress(
                    transferId = frame.transferId,
                    fileName = state.fileName,
                    totalBytes = state.fileSize,
                    transferredBytes = state.receivedBytes,
                    isOutgoing = false,
                    isComplete = isComplete,
                    localFilePath = state.outputFile.absolutePath
                )
            )

            if (isComplete) {
                synchronized(state.raf) {
                    state.raf.fd.sync()
                    state.raf.close()
                }
                incomingTransfers.remove(frame.transferId)

                messageRepository.saveIncomingMessage(
                    id = frame.transferId,
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
                    payload = frame.transferId
                )
                transportManager.sendPacketToIp(remoteIp, ack)

                val completedProgress = FileTransferProgress(
                    transferId = frame.transferId,
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
            Log.e(tag, "Error writing chunk frame: ${e.message}")
        }
    }

    /**
     * Reads exactly [size] bytes from [input], unless the stream ends first.
     *
     * A single `InputStream.read()` call is *not* guaranteed to fill the buffer just
     * because more data is still coming — this is common for content:// streams backed
     * by a pipe rather than a plain file (cloud storage, share-sheet URIs from other
     * apps, camera capture, etc). If that short read were treated as a full chunk, the
     * offset-based write on the receiving side (`chunkIndex * chunkSize`) would leave a
     * gap in the reassembled file. This loops until either the buffer is completely
     * filled or a read returns -1 (true end-of-stream).
     *
     * Returns null only at true EOF (nothing left to read at all); returns a
     * shorter-than-[size] array only for the final chunk of the file.
     */
    private fun readFullChunk(input: InputStream, size: Int): ByteArray? {
        val buffer = ByteArray(size)
        var filled = 0
        while (filled < size) {
            val n = input.read(buffer, filled, size - filled)
            if (n == -1) break
            filled += n
        }
        if (filled == 0) return null
        return if (filled == size) buffer else buffer.copyOf(filled)
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
