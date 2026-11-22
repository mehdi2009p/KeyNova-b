package com.keynova.keyboard.settings

import android.content.Context
import android.content.SharedPreferences

class KeyboardPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var keyPreviewEnabled: Boolean
        get() = prefs.getBoolean(KEY_PREVIEW, true)
        set(value) = prefs.edit().putBoolean(KEY_PREVIEW, value).apply()

    var numberRowEnabled: Boolean
        get() = prefs.getBoolean(KEY_NUMBER_ROW, false)
        set(value) = prefs.edit().putBoolean(KEY_NUMBER_ROW, value).apply()

    var showHintsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HINTS, true)
        set(value) = prefs.edit().putBoolean(KEY_HINTS, value).apply()

    var longPressSpaceDot: Boolean
        get() = prefs.getBoolean(KEY_SPACE_DOT, true)
        set(value) = prefs.edit().putBoolean(KEY_SPACE_DOT, value).apply()

    var swipeSpaceLanguage: Boolean
        get() = prefs.getBoolean(KEY_SWIPE_SPACE_LANG, true)
        set(value) = prefs.edit().putBoolean(KEY_SWIPE_SPACE_LANG, value).apply()

    var doubleSpacePeriod: Boolean
        get() = prefs.getBoolean(KEY_DOUBLE_SPACE_PERIOD, true)
        set(value) = prefs.edit().putBoolean(KEY_DOUBLE_SPACE_PERIOD, value).apply()

    var keyHeightDp: Int
        get() = prefs.getInt(KEY_HEIGHT, DEFAULT_HEIGHT_DP).coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
        set(value) = prefs.edit().putInt(KEY_HEIGHT, value.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)).apply()

    var keyGapDp: Int
        get() = prefs.getInt(KEY_GAP, DEFAULT_GAP_DP).coerceIn(MIN_GAP_DP, MAX_GAP_DP)
        set(value) = prefs.edit().putInt(KEY_GAP, value.coerceIn(MIN_GAP_DP, MAX_GAP_DP)).apply()

    var keyTextSizeSp: Int
        get() = prefs.getInt(KEY_TEXT_SIZE, DEFAULT_TEXT_SP).coerceIn(MIN_TEXT_SP, MAX_TEXT_SP)
        set(value) = prefs.edit().putInt(KEY_TEXT_SIZE, value.coerceIn(MIN_TEXT_SP, MAX_TEXT_SP)).apply()

    var longPressDelayMs: Int
        get() = prefs.getInt(KEY_LONG_PRESS_DELAY, DEFAULT_LONG_PRESS_MS).coerceIn(MIN_LONG_PRESS_MS, MAX_LONG_PRESS_MS)
        set(value) = prefs.edit().putInt(KEY_LONG_PRESS_DELAY, value.coerceIn(MIN_LONG_PRESS_MS, MAX_LONG_PRESS_MS)).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val PREFS_NAME = "keynova_prefs"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_PREVIEW = "key_preview_enabled"
        const val KEY_NUMBER_ROW = "number_row_enabled"
        const val KEY_HINTS = "show_hints_enabled"
        const val KEY_SPACE_DOT = "space_longpress_dot"
        const val KEY_SWIPE_SPACE_LANG = "swipe_space_language"
        const val KEY_DOUBLE_SPACE_PERIOD = "double_space_period"
        const val KEY_HEIGHT = "key_height_dp"
        const val KEY_GAP = "key_gap_dp"
        const val KEY_TEXT_SIZE = "key_text_size_sp"
        const val KEY_LONG_PRESS_DELAY = "long_press_delay_ms"
        const val DEFAULT_HEIGHT_DP = 58
        const val MIN_HEIGHT_DP = 48
        const val MAX_HEIGHT_DP = 68
        const val DEFAULT_GAP_DP = 2
        const val MIN_GAP_DP = 0
        const val MAX_GAP_DP = 6
        const val DEFAULT_TEXT_SP = 20
        const val MIN_TEXT_SP = 16
        const val MAX_TEXT_SP = 26
        const val DEFAULT_LONG_PRESS_MS = 350
        const val MIN_LONG_PRESS_MS = 200
        const val MAX_LONG_PRESS_MS = 700
    }
}
