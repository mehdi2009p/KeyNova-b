package com.keynova.keyboard.settings

import android.content.Context

class KeyboardPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var keyHeightDp: Int
        get() = prefs.getInt(KEY_HEIGHT, DEFAULT_HEIGHT_DP).coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
        set(value) = prefs.edit().putInt(KEY_HEIGHT, value.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)).apply()

    companion object {
        private const val PREFS_NAME = "keynova_prefs"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_HEIGHT = "key_height_dp"
        const val DEFAULT_HEIGHT_DP = 58
        const val MIN_HEIGHT_DP = 48
        const val MAX_HEIGHT_DP = 68
    }
}
