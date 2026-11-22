package com.keynova.keyboard.settings

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

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

    var doubleSpacePeriod: Boolean
        get() = prefs.getBoolean(KEY_DOUBLE_SPACE_PERIOD, true)
        set(value) = prefs.edit().putBoolean(KEY_DOUBLE_SPACE_PERIOD, value).apply()

    var autoCapitalize: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CAP, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CAP, value).apply()

    // 0=off, 1=language switch, 2=cursor move
    var spaceSwipeMode: Int
        get() = prefs.getInt(KEY_SPACE_SWIPE_MODE, if (prefs.getBoolean(KEY_SWIPE_SPACE_LANG, true)) 1 else 0).coerceIn(0, 2)
        set(value) = prefs.edit().putInt(KEY_SPACE_SWIPE_MODE, value.coerceIn(0, 2)).apply()

    // 0=language name, 1=space/فاصله, 2=empty
    var spaceLabelMode: Int
        get() = prefs.getInt(KEY_SPACE_LABEL, 1).coerceIn(0, 2)
        set(value) = prefs.edit().putInt(KEY_SPACE_LABEL, value.coerceIn(0, 2)).apply()

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

    var soundVolumePercent: Int
        get() = prefs.getInt(KEY_SOUND_VOLUME, 60).coerceIn(0, 100)
        set(value) = prefs.edit().putInt(KEY_SOUND_VOLUME, value.coerceIn(0, 100)).apply()

    var bottomPaddingDp: Int
        get() = prefs.getInt(KEY_BOTTOM_PADDING, 38).coerceIn(0, 80)
        set(value) = prefs.edit().putInt(KEY_BOTTOM_PADDING, value.coerceIn(0, 80)).apply()

    var themeId: String
        get() = prefs.getString(KEY_THEME, "dark_gold") ?: "dark_gold"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var suggestionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUGGESTIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_SUGGESTIONS, value).apply()

    data class ClipItem(val text: String, val locked: Boolean = false)

    var clipboardItems: List<ClipItem>
        get() {
            val raw = prefs.getString(KEY_CLIPBOARD, "[]") ?: "[]"
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.opt(i)
                        val clip = when (item) {
                            is JSONObject -> ClipItem(item.optString("text").trim().take(500), item.optBoolean("locked", false))
                            is String -> ClipItem(item.trim().take(500), false)
                            else -> null
                        }
                        if (clip != null && clip.text.isNotBlank()) add(clip)
                    }
                }.distinctBy { it.text }.take(20)
            }.getOrDefault(emptyList())
        }
        set(value) {
            val arr = JSONArray()
            value.filter { it.text.isNotBlank() }.distinctBy { it.text }.take(20).forEach {
                arr.put(JSONObject().put("text", it.text.trim().take(500)).put("locked", it.locked))
            }
            prefs.edit().putString(KEY_CLIPBOARD, arr.toString()).apply()
        }

    var clipboardHistory: List<String>
        get() = clipboardItems.map { it.text }
        set(value) { clipboardItems = value.map { ClipItem(it) } }

    fun addClipboardItem(text: String) {
        val clean = text.trim().take(500)
        if (clean.isBlank()) return
        val current = clipboardItems.toMutableList()
        val existing = current.find { it.text == clean }
        current.removeAll { it.text == clean }
        current.add(0, existing?.copy(text = clean) ?: ClipItem(clean, false))
        clipboardItems = current
    }

    fun removeClipboardItem(text: String) {
        clipboardItems = clipboardItems.filterNot { it.text == text && !it.locked }
    }

    fun toggleClipboardLock(text: String) {
        clipboardItems = clipboardItems.map { if (it.text == text) it.copy(locked = !it.locked) else it }
    }

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
        const val KEY_AUTO_CAP = "auto_capitalize"
        const val KEY_SPACE_SWIPE_MODE = "space_swipe_mode"
        const val KEY_SPACE_LABEL = "space_label_mode"
        const val KEY_HEIGHT = "key_height_dp"
        const val KEY_GAP = "key_gap_dp"
        const val KEY_TEXT_SIZE = "key_text_size_sp"
        const val KEY_LONG_PRESS_DELAY = "long_press_delay_ms"
        const val KEY_SOUND_VOLUME = "sound_volume_percent"
        const val KEY_BOTTOM_PADDING = "bottom_padding_dp"
        const val KEY_THEME = "theme_id"
        const val KEY_SUGGESTIONS = "suggestions_enabled"
        const val KEY_CLIPBOARD = "clipboard_history"
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
