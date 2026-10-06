/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: RewardedAdManager.kt
 *
 * Responsibilities:
 * - Manages Google Mobile Ads Rewarded Ad loading, caching, and lifecycle.
 * - Ad Unit ID: ca-app-pub-3940256099942544/5224354917 (Official Test Rewarded Unit).
 * - Manages Turbo Boost rewards (30-minute high-speed pipeline boost & AI compute tokens).
 * - Non-intrusive, voluntary opt-in reward architecture.
 */

package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RewardedAdManager(private val context: Context) {

    companion object {
        private const val TAG = "RewardedAdManager"
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val PREFS_NAME = "peerlink_rewarded_prefs"
        private const val KEY_TURBO_EXPIRES_AT = "turbo_expires_at"
        private const val KEY_TURBO_TOKENS = "turbo_tokens"
        private const val KEY_TOTAL_REWARDS = "total_rewards_earned"
        const val BOOST_DURATION_MILLIS = 30 * 60 * 1000L // 30 minutes
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rewardedAd: RewardedAd? = null

    // State flows
    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _adLoadError = MutableStateFlow<String?>(null)
    val adLoadError: StateFlow<String?> = _adLoadError.asStateFlow()

    private val _isTurboActive = MutableStateFlow(false)
    val isTurboActive: StateFlow<Boolean> = _isTurboActive.asStateFlow()

    private val _remainingBoostSeconds = MutableStateFlow(0L)
    val remainingBoostSeconds: StateFlow<Long> = _remainingBoostSeconds.asStateFlow()

    private val _turboTokens = MutableStateFlow(prefs.getInt(KEY_TURBO_TOKENS, 0))
    val turboTokens: StateFlow<Int> = _turboTokens.asStateFlow()

    private val _totalRewardsEarned = MutableStateFlow(prefs.getInt(KEY_TOTAL_REWARDS, 0))
    val totalRewardsEarned: StateFlow<Int> = _totalRewardsEarned.asStateFlow()

    private val _rewardEvents = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val rewardEvents: SharedFlow<String> = _rewardEvents.asSharedFlow()

    init {
        // Safe SDK initialization
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "Google Mobile Ads initialized successfully: $status")
                loadAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds SDK", e)
        }

        // Start countdown ticker for Turbo Boost
        startBoostTicker()
    }

    private fun startBoostTicker() {
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                updateBoostStatus()
                delay(1000)
            }
        }
    }

    private fun updateBoostStatus() {
        val expiresAt = prefs.getLong(KEY_TURBO_EXPIRES_AT, 0L)
        val now = System.currentTimeMillis()
        val remaining = (expiresAt - now) / 1000L

        if (remaining > 0) {
            _isTurboActive.value = true
            _remainingBoostSeconds.value = remaining
        } else {
            _isTurboActive.value = false
            _remainingBoostSeconds.value = 0L
        }
    }

    fun loadAd() {
        if (_isLoading.value || _isAdLoaded.value) return

        mainHandler.post {
            _isLoading.value = true
            _adLoadError.value = null

            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context,
                TEST_REWARDED_AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "Rewarded ad loaded successfully.")
                        rewardedAd = ad
                        _isAdLoaded.value = true
                        _isLoading.value = false
                        _adLoadError.value = null
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.w(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                        rewardedAd = null
                        _isAdLoaded.value = false
                        _isLoading.value = false
                        _adLoadError.value = loadAdError.message
                    }
                }
            )
        }
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (RewardItem) -> Unit = {},
        onAdDismissed: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ) {
        val currentAd = rewardedAd
        if (currentAd == null) {
            Log.w(TAG, "Ad not loaded yet, trying to load...")
            loadAd()
            onAdFailed("Ad is loading. Please try again in a few moments.")
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad displayed on screen.")
                rewardedAd = null
                _isAdLoaded.value = false
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed by user.")
                rewardedAd = null
                _isAdLoaded.value = false
                onAdDismissed()
                // Automatically preload next ad for seamless future interaction
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
                _isAdLoaded.value = false
                onAdFailed(adError.message)
                loadAd()
            }
        }

        currentAd.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            grantTurboBoost(BOOST_DURATION_MILLIS)
            onRewardEarned(rewardItem)
        }
    }

    fun grantTurboBoost(durationMillis: Long = BOOST_DURATION_MILLIS) {
        val currentExpires = prefs.getLong(KEY_TURBO_EXPIRES_AT, 0L)
        val now = System.currentTimeMillis()
        val newExpires = if (currentExpires > now) {
            currentExpires + durationMillis
        } else {
            now + durationMillis
        }

        val newTokens = _turboTokens.value + 2
        val newTotal = _totalRewardsEarned.value + 1

        prefs.edit()
            .putLong(KEY_TURBO_EXPIRES_AT, newExpires)
            .putInt(KEY_TURBO_TOKENS, newTokens)
            .putInt(KEY_TOTAL_REWARDS, newTotal)
            .apply()

        _turboTokens.value = newTokens
        _totalRewardsEarned.value = newTotal
        updateBoostStatus()

        _rewardEvents.tryEmit("Turbo Boost activated for 30 minutes! +2 Turbo Tokens added.")
    }

    fun formatRemainingTime(seconds: Long): String {
        if (seconds <= 0) return "Inactive"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
            else -> String.format("%02d:%02d", minutes, secs)
        }
    }
}
