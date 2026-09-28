package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent storage and reactive state for UI Presentation Style (Default vs Glassmorphism).
 * Ensures "Default" is strictly preserved on fresh installs or until the user chooses otherwise.
 */
class UiThemeManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentStyle = MutableStateFlow(loadSavedStyle())
    val currentStyle: StateFlow<UiThemeStyle> = _currentStyle.asStateFlow()

    private fun loadSavedStyle(): UiThemeStyle {
        val saved = prefs.getString(KEY_UI_STYLE, UiThemeStyle.DEFAULT.id)
        return UiThemeStyle.fromId(saved)
    }

    fun setUiStyle(style: UiThemeStyle) {
        prefs.edit().putString(KEY_UI_STYLE, style.id).apply()
        _currentStyle.value = style
    }

    companion object {
        private const val PREFS_NAME = "peerlink_ui_theme_prefs"
        private const val KEY_UI_STYLE = "selected_ui_style"
    }
}
