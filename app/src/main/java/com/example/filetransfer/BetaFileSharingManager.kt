package com.example.filetransfer

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Configuration and state manager for the Beta Sustained High-Speed File Transfer Pipeline.
 * Root Cause Mitigation:
 * - Unconstrained burst transfers (111 MB/s) rapidly exhaust the device's Pseudo-SLC (pSLC)
 *   cache and breach the Linux kernel's `dirty_ratio` threshold.
 * - This causes Linux writeback throttling, halts the receiver's read loop, and triggers
 *   TCP Zero Window advertisements, collapsing throughput to 100 KB/s - 600 KB/s.
 * - The Beta File Sharing pipeline applies microsecond-level rate pacing (e.g., 65 MB/s),
 *   calibrated in-flight buffer windows, and pre-allocated sequential storage.
 * - This keeps the UFS storage controller in sustained equilibrium, preventing write stalls
 *   and maintaining a continuous high-speed stream.
 */
class BetaFileSharingManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Default to true so users immediately experience the sustained, drop-free transfer pipeline
    private val _isBetaEnabled = MutableStateFlow(prefs.getBoolean(KEY_BETA_ENABLED, true))
    val isBetaEnabled: StateFlow<Boolean> = _isBetaEnabled.asStateFlow()

    private val _targetSpeedMBps = MutableStateFlow(prefs.getInt(KEY_TARGET_SPEED_MBPS, 65))
    val targetSpeedMBps: StateFlow<Int> = _targetSpeedMBps.asStateFlow()

    private val _antiBufferbloatEnabled = MutableStateFlow(prefs.getBoolean(KEY_ANTI_BUFFERBLOAT, true))
    val antiBufferbloatEnabled: StateFlow<Boolean> = _antiBufferbloatEnabled.asStateFlow()

    fun setBetaEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BETA_ENABLED, enabled).apply()
        _isBetaEnabled.value = enabled
    }

    fun setTargetSpeedMBps(speedMBps: Int) {
        val clamped = speedMBps.coerceIn(45, 120)
        prefs.edit().putInt(KEY_TARGET_SPEED_MBPS, clamped).apply()
        _targetSpeedMBps.value = clamped
    }

    fun setAntiBufferbloat(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANTI_BUFFERBLOAT, enabled).apply()
        _antiBufferbloatEnabled.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "peerlink_beta_file_sharing"
        private const val KEY_BETA_ENABLED = "beta_file_sharing_enabled"
        private const val KEY_TARGET_SPEED_MBPS = "target_speed_mbps"
        private const val KEY_ANTI_BUFFERBLOAT = "anti_bufferbloat_enabled"
    }
}
