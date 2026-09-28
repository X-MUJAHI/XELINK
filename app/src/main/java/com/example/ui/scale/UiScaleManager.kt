package com.example.ui.scale

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class UiScaleMode(val displayName: String, val scaleFactor: Float, val description: String) {
    AUTO("Auto Adaptive", 1.0f, "Auto-detects DPI & screen width to eliminate truncation"),
    COMPACT("Compact (85%)", 0.85f, "Optimized for small screens & high information density"),
    BALANCED("Standard (100%)", 1.00f, "Default 1:1 Android system scale"),
    COMFORTABLE("Comfortable (115%)", 1.15f, "Larger text and touch targets for comfort"),
    CUSTOM("Custom", 1.0f, "Fine-tune scaling with a continuous slider")
}

data class UiScaleConfig(
    val mode: UiScaleMode = UiScaleMode.AUTO,
    val customScale: Float = 1.0f,
    val autoBalanceLargeFonts: Boolean = true
)

data class DeviceDisplayMetrics(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val densityFactor: Float,
    val scaledDensity: Float,
    val widthDp: Int,
    val heightDp: Int,
    val aspectRatio: Float,
    val refreshRate: Float,
    val formFactor: String
)

class UiScaleManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<UiScaleConfig> = _config.asStateFlow()

    private val _displayMetrics = MutableStateFlow(detectDisplayMetrics())
    val displayMetrics: StateFlow<DeviceDisplayMetrics> = _displayMetrics.asStateFlow()

    fun refreshDisplayMetrics() {
        _displayMetrics.value = detectDisplayMetrics()
    }

    fun setMode(mode: UiScaleMode) {
        val newConfig = _config.value.copy(mode = mode)
        saveConfig(newConfig)
        _config.value = newConfig
    }

    fun setCustomScale(scale: Float) {
        val clamped = (scale.coerceIn(0.70f, 1.35f) * 100).toInt() / 100f
        val newConfig = _config.value.copy(customScale = clamped, mode = UiScaleMode.CUSTOM)
        saveConfig(newConfig)
        _config.value = newConfig
    }

    fun setAutoBalanceLargeFonts(enabled: Boolean) {
        val newConfig = _config.value.copy(autoBalanceLargeFonts = enabled)
        saveConfig(newConfig)
        _config.value = newConfig
    }

    fun computeEffectiveScale(
        screenWidthDp: Int,
        screenHeightDp: Int,
        systemDensity: Float,
        systemFontScale: Float
    ): Float {
        val current = _config.value
        return when (current.mode) {
            UiScaleMode.AUTO -> {
                when {
                    screenWidthDp < 340 -> 0.85f
                    screenWidthDp < 365 -> 0.92f
                    screenWidthDp <= 440 -> 1.00f
                    screenWidthDp < 600 -> 1.02f
                    else -> 1.05f
                }
            }
            UiScaleMode.COMPACT -> 0.85f
            UiScaleMode.BALANCED -> 1.00f
            UiScaleMode.COMFORTABLE -> 1.15f
            UiScaleMode.CUSTOM -> current.customScale.coerceIn(0.70f, 1.35f)
        }
    }

    fun computeEffectiveFontScale(systemFontScale: Float): Float {
        val current = _config.value
        return if (current.autoBalanceLargeFonts && systemFontScale > 1.25f) {
            1.20f
        } else {
            systemFontScale
        }
    }

    private fun detectDisplayMetrics(): DeviceDisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val dm = context.resources.displayMetrics
        val configuration = context.resources.configuration

        val widthPx: Int
        val heightPx: Int
        val refreshRate: Float

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
            val windowMetrics = wm.currentWindowMetrics
            widthPx = windowMetrics.bounds.width()
            heightPx = windowMetrics.bounds.height()
            refreshRate = try {
                context.display?.refreshRate ?: 60f
            } catch (_: Throwable) {
                60f
            }
        } else {
            widthPx = dm.widthPixels
            heightPx = dm.heightPixels
            @Suppress("DEPRECATION")
            refreshRate = wm?.defaultDisplay?.refreshRate ?: 60f
        }

        val widthDp = configuration.screenWidthDp
        val heightDp = configuration.screenHeightDp
        val densityDpi = dm.densityDpi
        val densityFactor = dm.density
        val aspectRatio = if (heightPx > 0) widthPx.toFloat() / heightPx.toFloat() else 0.56f

        val formFactor = when {
            widthDp < 360 -> "Compact Phone (<360dp)"
            widthDp <= 440 -> "Standard Phone (360–440dp)"
            widthDp < 600 -> "Large / Phablet (440–600dp)"
            else -> "Tablet / Foldable (≥600dp)"
        }

        return DeviceDisplayMetrics(
            widthPx = widthPx,
            heightPx = heightPx,
            densityDpi = densityDpi,
            densityFactor = densityFactor,
            scaledDensity = dm.scaledDensity,
            widthDp = widthDp,
            heightDp = heightDp,
            aspectRatio = aspectRatio,
            refreshRate = refreshRate,
            formFactor = formFactor
        )
    }

    private fun loadConfig(): UiScaleConfig {
        val modeStr = prefs.getString(KEY_MODE, UiScaleMode.AUTO.name) ?: UiScaleMode.AUTO.name
        val mode = try {
            UiScaleMode.valueOf(modeStr)
        } catch (_: Exception) {
            UiScaleMode.AUTO
        }
        val customScale = prefs.getFloat(KEY_CUSTOM_SCALE, 1.0f)
        val autoBalanceLargeFonts = prefs.getBoolean(KEY_AUTO_BALANCE_FONTS, true)
        return UiScaleConfig(
            mode = mode,
            customScale = customScale,
            autoBalanceLargeFonts = autoBalanceLargeFonts
        )
    }

    private fun saveConfig(config: UiScaleConfig) {
        prefs.edit()
            .putString(KEY_MODE, config.mode.name)
            .putFloat(KEY_CUSTOM_SCALE, config.customScale)
            .putBoolean(KEY_AUTO_BALANCE_FONTS, config.autoBalanceLargeFonts)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "ui_scale_preferences"
        private const val KEY_MODE = "ui_scale_mode"
        private const val KEY_CUSTOM_SCALE = "ui_custom_scale"
        private const val KEY_AUTO_BALANCE_FONTS = "ui_auto_balance_fonts"
    }
}
