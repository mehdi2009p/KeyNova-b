package com.keynova.keyboard.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.keynova.keyboard.R
import com.keynova.keyboard.settings.KeyboardPrefs
import com.keynova.keyboard.theme.ThemeCategory
import com.keynova.keyboard.theme.ThemeManager

class KeyboardSettingsActivity : AppCompatActivity() {

    private lateinit var prefs: KeyboardPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard_settings)
        prefs = KeyboardPrefs(this)

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        bindSwitch(R.id.switchSuggestions, prefs.suggestionsEnabled) { prefs.suggestionsEnabled = it }
        bindSwitch(R.id.switchVibration, prefs.vibrationEnabled) { prefs.vibrationEnabled = it }
        bindSwitch(R.id.switchSound, prefs.soundEnabled) { prefs.soundEnabled = it }
        bindSwitch(R.id.switchPreview, prefs.keyPreviewEnabled) { prefs.keyPreviewEnabled = it }
        bindSwitch(R.id.switchNumberRow, prefs.numberRowEnabled) { prefs.numberRowEnabled = it }
        bindSwitch(R.id.switchHints, prefs.showHintsEnabled) { prefs.showHintsEnabled = it }
        bindSwitch(R.id.switchSpaceDot, prefs.longPressSpaceDot) { prefs.longPressSpaceDot = it }
        bindSwitch(R.id.switchDoubleSpace, prefs.doubleSpacePeriod) { prefs.doubleSpacePeriod = it }
        bindSwitch(R.id.switchAutoCap, prefs.autoCapitalize) { prefs.autoCapitalize = it }

        bindSeek(R.id.seekHeight, R.id.valueHeight, KeyboardPrefs.MIN_HEIGHT_DP, KeyboardPrefs.MAX_HEIGHT_DP, prefs.keyHeightDp, "dp") { prefs.keyHeightDp = it }
        bindSeek(R.id.seekGap, R.id.valueGap, KeyboardPrefs.MIN_GAP_DP, KeyboardPrefs.MAX_GAP_DP, prefs.keyGapDp, "dp") { prefs.keyGapDp = it }
        bindSeek(R.id.seekText, R.id.valueText, KeyboardPrefs.MIN_TEXT_SP, KeyboardPrefs.MAX_TEXT_SP, prefs.keyTextSizeSp, "sp") { prefs.keyTextSizeSp = it }
        bindSeek(R.id.seekLongPress, R.id.valueLongPress, KeyboardPrefs.MIN_LONG_PRESS_MS, KeyboardPrefs.MAX_LONG_PRESS_MS, prefs.longPressDelayMs, "ms") { prefs.longPressDelayMs = it }
        bindSeek(R.id.seekSoundVol, R.id.valueSoundVol, 0, 100, prefs.soundVolumePercent, "%") { prefs.soundVolumePercent = it }
        bindSeek(R.id.seekBottomPad, R.id.valueBottomPad, 0, 80, prefs.bottomPaddingDp, "dp") { prefs.bottomPaddingDp = it }

        bindRadioGroup(R.id.groupSpaceSwipe, arrayOf(R.id.radioSwipeOff, R.id.radioSwipeLang, R.id.radioSwipeCursor), prefs.spaceSwipeMode) { prefs.spaceSwipeMode = it }
        bindRadioGroup(R.id.groupSpaceLabel, arrayOf(R.id.radioLabelLang, R.id.radioLabelSpace, R.id.radioLabelEmpty), prefs.spaceLabelMode) { prefs.spaceLabelMode = it }
        setupThemePickers()
    }

    private fun setupThemePickers() {
        val colorGrid = findViewById<GridLayout>(R.id.themeColorGrid)
        val wpGrid = findViewById<GridLayout>(R.id.themeWallpaperGrid)
        fun render() {
            colorGrid.removeAllViews(); wpGrid.removeAllViews()
            ThemeManager.all().forEach { t ->
                val tv = TextView(this).apply {
                    text = t.name
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(16, 24, 16, 24)
                    val strokeW = if (prefs.themeId == t.id) 5 else 1
                    val strokeC = if (prefs.themeId == t.id) t.accent else 0x44FFFFFF
                    if (t.wallpaperDrawable != null) {
                        val bg = android.graphics.drawable.LayerDrawable(arrayOf(
                            getDrawable(t.wallpaperDrawable)!!,
                            GradientDrawable().apply { setColor(0x66000000); cornerRadius = 18f },
                            GradientDrawable().apply { setColor(Color.TRANSPARENT); cornerRadius = 18f; setStroke(strokeW, strokeC) }
                        ))
                        background = bg
                    } else {
                        background = GradientDrawable().apply {
                            cornerRadius = 18f
                            setColor(t.keyBg)
                            setStroke(strokeW, strokeC)
                        }
                        setTextColor(t.text)
                    }
                    val lp = GridLayout.LayoutParams().apply {
                        width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(8,8,8,8)
                    }
                    layoutParams = lp
                    setOnClickListener { prefs.themeId = t.id; render() }
                }
                if (t.category == ThemeCategory.COLOR) colorGrid.addView(tv) else wpGrid.addView(tv)
            }
        }
        render()
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

    private fun bindRadioGroup(groupId: Int, ids: Array<Int>, selected: Int, onChange: (Int) -> Unit) {
        val g = findViewById<RadioGroup>(groupId)
        if (selected in ids.indices) g.check(ids[selected])
        g.setOnCheckedChangeListener { _, checkedId ->
            val idx = ids.indexOf(checkedId)
            if (idx >= 0) onChange(idx)
        }
    }
}
