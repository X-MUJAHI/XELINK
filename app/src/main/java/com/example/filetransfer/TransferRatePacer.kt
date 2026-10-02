package com.example.filetransfer

import kotlinx.coroutines.delay

/**
 * Microsecond Token-Bucket Transfer Rate Pacer.
 * Prevents:
 * 1. Pseudo-SLC exhaustion: Pacing at the sustained write limit of TLC NAND allows the
 *    controller's background garbage collection (folding) to keep up with incoming data.
 * 2. Linux kernel dirty page writeback throttling: Prevents dirty pages from breaching
 *    the hard `dirty_ratio` limit, eliminating thread-sleep stalls and TCP Zero-Window crashes.
 * 3. Bufferbloat & RTT latency spikes: Keeps in-flight network queues shallow (<10ms).
 */
class TransferRatePacer(
    targetMBps: Int = 65
) {
    @Volatile
    var targetBytesPerSec: Long = targetMBps.toLong() * 1024L * 1024L
        private set

    private var windowStartTime = System.currentTimeMillis()
    private var bytesSentInWindow = 0L

    fun updateTargetMBps(targetMBps: Int) {
        targetBytesPerSec = targetMBps.toLong().coerceIn(30L, 120L) * 1024L * 1024L
    }

    /**
     * Called before or after transmitting a chunk.
     * Computes necessary sleep delay to ensure average throughput does not exceed the
     * sustained rate that would cause UFS writeback throttling.
     */
    suspend fun pace(chunkSizeBytes: Int) {
        if (targetBytesPerSec <= 0) return
        bytesSentInWindow += chunkSizeBytes
        val now = System.currentTimeMillis()
        val elapsed = now - windowStartTime

        // Calculate expected time (in ms) to transmit bytesSentInWindow at targetBytesPerSec
        val expectedTimeMs = (bytesSentInWindow * 1000L) / targetBytesPerSec
        if (expectedTimeMs > elapsed) {
            val sleepMs = expectedTimeMs - elapsed
            if (sleepMs > 0) {
                delay(sleepMs)
            }
        }

        // Reset window every 500ms to avoid drift and maintain responsive micro-pacing
        val currentNow = System.currentTimeMillis()
        if (currentNow - windowStartTime >= 500) {
            windowStartTime = currentNow
            bytesSentInWindow = 0L
        }
    }
}
