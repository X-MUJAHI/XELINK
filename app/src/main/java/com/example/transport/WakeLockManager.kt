package com.example.transport

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages CPU WakeLock to keep networking, socket transfers, voice/video calls,
 * screen mirroring, and privileged low-latency active without CPU sleeping or packet throttling.
 */
class WakeLockManager(private val context: Context) {
    private val tag = "WakeLockManager"
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    // Track active lock holders by unique tag
    private val activeHolders = ConcurrentHashMap.newKeySet<String>()

    private val _isWakeLockActive = MutableStateFlow(false)
    val isWakeLockActive: StateFlow<Boolean> = _isWakeLockActive.asStateFlow()

    private val _manualOverride = MutableStateFlow(false)
    val manualOverride: StateFlow<Boolean> = _manualOverride.asStateFlow()

    @SuppressLint("WakelockTimeout")
    @Synchronized
    fun acquire(reason: String, timeoutMs: Long = -1L) {
        try {
            activeHolders.add(reason)
            ensureWakeLockAcquired(timeoutMs)
            _isWakeLockActive.value = true
            Log.d(tag, "WakeLock acquired by: $reason (Total holders: ${activeHolders.size})")
        } catch (e: Exception) {
            Log.e(tag, "Failed to acquire WakeLock for $reason: ${e.message}")
        }
    }

    @Synchronized
    fun release(reason: String) {
        try {
            activeHolders.remove(reason)
            if (activeHolders.isEmpty() && !_manualOverride.value) {
                releaseInternalLock()
                _isWakeLockActive.value = false
            }
            Log.d(tag, "WakeLock released by: $reason (Remaining holders: ${activeHolders.size})")
        } catch (e: Exception) {
            Log.e(tag, "Failed to release WakeLock for $reason: ${e.message}")
        }
    }

    fun setManualWakeLock(enabled: Boolean) {
        _manualOverride.value = enabled
        if (enabled) {
            acquire("UserManual")
        } else {
            release("UserManual")
        }
    }

    fun toggleManualWakeLock() {
        setManualWakeLock(!_manualOverride.value)
    }

    @SuppressLint("WakelockTimeout")
    private fun ensureWakeLockAcquired(timeoutMs: Long) {
        if (wakeLock == null) {
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PeerLink:NetworkingWakeLock"
            )?.apply {
                setReferenceCounted(false)
            }
        }

        wakeLock?.let { lock ->
            if (!lock.isHeld) {
                if (timeoutMs > 0) {
                    lock.acquire(timeoutMs)
                } else {
                    lock.acquire()
                }
            }
        }
    }

    private fun releaseInternalLock() {
        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error releasing internal lock: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    fun getActiveHoldersList(): List<String> = activeHolders.toList()
}
