package com.keynova.keyboard.ui

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.keynova.keyboard.R
import com.keynova.keyboard.settings.KeyboardPrefs

class KeyboardSettingsActivity : AppCompatActivity() {

    private lateinit var prefs: KeyboardPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard_settings)
        prefs = KeyboardPrefs(this)

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        bindSwitch(R.id.switchVibration, prefs.vibrationEnabled) { prefs.vibrationEnabled = it }
        bindSwitch(R.id.switchSound, prefs.soundEnabled) { prefs.soundEnabled = it }
        bindSwitch(R.id.switchPreview, prefs.keyPreviewEnabled) { prefs.keyPreviewEnabled = it }
        bindSwitch(R.id.switchNumberRow, prefs.numberRowEnabled) { prefs.numberRowEnabled = it }
        bindSwitch(R.id.switchHints, prefs.showHintsEnabled) { prefs.showHintsEnabled = it }
        bindSwitch(R.id.switchSpaceDot, prefs.longPressSpaceDot) { prefs.longPressSpaceDot = it }
        bindSwitch(R.id.switchSwipeLang, prefs.swipeSpaceLanguage) { prefs.swipeSpaceLanguage = it }
        bindSwitch(R.id.switchDoubleSpace, prefs.doubleSpacePeriod) { prefs.doubleSpacePeriod = it }

        bindSeek(R.id.seekHeight, R.id.valueHeight, KeyboardPrefs.MIN_HEIGHT_DP, KeyboardPrefs.MAX_HEIGHT_DP, prefs.keyHeightDp, "dp") { prefs.keyHeightDp = it }
        bindSeek(R.id.seekGap, R.id.valueGap, KeyboardPrefs.MIN_GAP_DP, KeyboardPrefs.MAX_GAP_DP, prefs.keyGapDp, "dp") { prefs.keyGapDp = it }
        bindSeek(R.id.seekText, R.id.valueText, KeyboardPrefs.MIN_TEXT_SP, KeyboardPrefs.MAX_TEXT_SP, prefs.keyTextSizeSp, "sp") { prefs.keyTextSizeSp = it }
        bindSeek(R.id.seekLongPress, R.id.valueLongPress, KeyboardPrefs.MIN_LONG_PRESS_MS, KeyboardPrefs.MAX_LONG_PRESS_MS, prefs.longPressDelayMs, "ms") { prefs.longPressDelayMs = it }
    }

    private fun bindSwitch(id: Int, initial: Boolean, onChange: (Boolean) -> Unit) {
        val sw = findViewById<SwitchCompat>(id)
        sw.isChecked = initial
        sw.setOnCheckedChangeListener { _, checked -> onChange(checked) }
    }

    private fun bindSeek(seekId: Int, valueId: Int, min: Int, max: Int, current: Int, unit: String, onChange: (Int) -> Unit) {
        val seek = findViewById<SeekBar>(seekId)
        val value = findViewById<TextView>(valueId)
        seek.max = max - min
        seek.progress = current - min
        value.text = "$current$unit"
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val v = min + progress
                value.text = "$v$unit"
                if (fromUser) onChange(v)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}
