package com.keynova.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keynova.keyboard.R
import com.keynova.keyboard.layout.KeyboardLayouts
import com.keynova.keyboard.logic.KeyboardController
import com.keynova.keyboard.model.Key
import com.keynova.keyboard.model.KeyAction
import com.keynova.keyboard.model.KeyboardLanguage
import com.keynova.keyboard.model.KeyboardMode
import com.keynova.keyboard.settings.KeyboardPrefs
import kotlin.math.abs

class KeyboardView(context: Context) : LinearLayout(context) {

    var controller: KeyboardController? = null
        set(value) {
            field = value
            render()
        }

    private val handler = Handler(Looper.getMainLooper())
    private var longPressFired = false
    private var popup: PopupWindow? = null
    private val prefs = KeyboardPrefs(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var keyPreview: PopupWindow? = null
    @Volatile private var vibrationEnabled = prefs.vibrationEnabled
    @Volatile private var soundEnabled = prefs.soundEnabled
    @Volatile private var keyPreviewEnabled = prefs.keyPreviewEnabled
    @Volatile private var numberRowEnabled = prefs.numberRowEnabled
    @Volatile private var showHintsEnabled = prefs.showHintsEnabled
    @Volatile private var longPressSpaceDot = prefs.longPressSpaceDot
    @Volatile private var doubleSpacePeriod = prefs.doubleSpacePeriod
    @Volatile private var spaceSwipeMode = prefs.spaceSwipeMode
    @Volatile private var spaceLabelMode = prefs.spaceLabelMode
    @Volatile private var keyHeightDp = prefs.keyHeightDp
    @Volatile private var keyGapDp = prefs.keyGapDp
    @Volatile private var keyTextSizeSp = prefs.keyTextSizeSp
    @Volatile private var longPressDelayMs = prefs.longPressDelayMs
    @Volatile private var soundVolumePercent = prefs.soundVolumePercent
    @Volatile private var bottomPaddingDp = prefs.bottomPaddingDp
    private var lastSpaceTapMs = 0L
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KeyboardPrefs.KEY_VIBRATION -> vibrationEnabled = prefs.vibrationEnabled
            KeyboardPrefs.KEY_SOUND -> soundEnabled = prefs.soundEnabled
            KeyboardPrefs.KEY_PREVIEW -> keyPreviewEnabled = prefs.keyPreviewEnabled
            KeyboardPrefs.KEY_HINTS -> { showHintsEnabled = prefs.showHintsEnabled; post { render() } }
            KeyboardPrefs.KEY_SPACE_DOT -> longPressSpaceDot = prefs.longPressSpaceDot
            KeyboardPrefs.KEY_DOUBLE_SPACE_PERIOD -> doubleSpacePeriod = prefs.doubleSpacePeriod
            KeyboardPrefs.KEY_SPACE_SWIPE_MODE, KeyboardPrefs.KEY_SWIPE_SPACE_LANG -> spaceSwipeMode = prefs.spaceSwipeMode
            KeyboardPrefs.KEY_SPACE_LABEL -> { spaceLabelMode = prefs.spaceLabelMode; post { render() } }
            KeyboardPrefs.KEY_LONG_PRESS_DELAY -> longPressDelayMs = prefs.longPressDelayMs
            KeyboardPrefs.KEY_SOUND_VOLUME -> soundVolumePercent = prefs.soundVolumePercent
            KeyboardPrefs.KEY_BOTTOM_PADDING, KeyboardPrefs.KEY_HEIGHT, KeyboardPrefs.KEY_GAP, KeyboardPrefs.KEY_TEXT_SIZE, KeyboardPrefs.KEY_NUMBER_ROW -> {
                keyHeightDp = prefs.keyHeightDp; keyGapDp = prefs.keyGapDp; keyTextSizeSp = prefs.keyTextSizeSp
                numberRowEnabled = prefs.numberRowEnabled; bottomPaddingDp = prefs.bottomPaddingDp
                post { updatePadding(); render() }
            }
        }
    }

    init {
        orientation = VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        setBackgroundColor(ContextCompat.getColor(context, R.color.keynova_background))
        updatePadding()
        isClickable = true
        isFocusable = false
        isHapticFeedbackEnabled = true
        prefs.registerListener(prefListener)
    }

    private fun updatePadding() {
        val pad = dp(4)
        setPadding(pad, dp(10), pad, dp(bottomPaddingDp))
    }

    override fun onDetachedFromWindow() {
        prefs.unregisterListener(prefListener)
        super.onDetachedFromWindow()
    }

    fun render() {
        dismissPopup()
        dismissKeyPreview()
        removeAllViews()
        val ctrl = controller ?: return
        val rows = KeyboardLayouts.rowsFor(ctrl.mode, ctrl.language, ctrl.shiftState).toMutableList()
        if (numberRowEnabled && ctrl.mode == KeyboardMode.LETTERS) {
            rows.add(0, numberRow(ctrl.language))
        }
        rows.forEach { rowKeys -> addView(createRow(rowKeys)) }
    }

    private fun numberRow(language: KeyboardLanguage): List<Key> {
        val nums = if (language == KeyboardLanguage.FA) listOf("۱","۲","۳","۴","۵","۶","۷","۸","۹","۰") else listOf("1","2","3","4","5","6","7","8","9","0")
        return nums.map { Key(it, KeyAction.Character(it)) }
    }

    private fun createRow(keys: List<Key>): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
            gravity = Gravity.CENTER
        }
        keys.forEach { row.addView(createKeyView(it)) }
        return row
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createKeyView(key: Key): View {
        val keyH = keyHeightDp
        val container = android.widget.FrameLayout(context).apply {
            val lp = LayoutParams(0, dp(keyH), key.weight)
            val hGap = dp(keyGapDp)
            lp.setMargins(hGap, 0, hGap, 0)
            layoutParams = lp
            minimumHeight = dp(keyH)
            setBackgroundResource(
                when {
                    key.isAccent -> R.drawable.key_bg_accent
                    key.isSpecial -> R.drawable.key_bg_special
                    else -> R.drawable.key_bg
                }
            )
        }

        val tv = TextView(context).apply {
            text = when (key.action) {
                KeyAction.Space -> when (spaceLabelMode) {
                    0 -> if ((controller?.language ?: KeyboardLanguage.FA) == KeyboardLanguage.FA) "فارسی" else "English"
                    2 -> ""
                    else -> if ((controller?.language ?: KeyboardLanguage.FA) == KeyboardLanguage.FA) "فاصله" else "space"
                }
                else -> key.label
            }
            contentDescription = when (key.action) {
                KeyAction.Space -> "Space"
                KeyAction.Backspace -> "Backspace"
                KeyAction.Enter -> "Enter"
                else -> key.label
            }
            setTextColor(ContextCompat.getColor(context, R.color.keynova_text))
            val base = if (key.label.length > 3) (keyTextSizeSp - 6).coerceAtLeast(10) else keyTextSizeSp
            setTextSize(TypedValue.COMPLEX_UNIT_SP, base.toFloat())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isAllCaps = false
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(3), dp(7), dp(3), dp(7))
        }
        container.addView(tv)

        if (showHintsEnabled && key.longPressLabel != null && key.popupActions.isEmpty()) {
            val hint = TextView(context).apply {
                text = key.longPressLabel
                setTextColor(ContextCompat.getColor(context, R.color.keynova_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.START
                ).apply { setMargins(dp(4), dp(2), 0, 0) }
            }
            container.addView(hint)
        }

        when (key.action) {
            KeyAction.Backspace -> bindBackspace(container)
            KeyAction.Space -> bindSpaceTouch(container)
            else -> bindKeyTouch(container, key)
        }
        return container
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindBackspace(v: View) {
        v.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) provideFeedback(view)
            val handled = controller?.onBackspaceTouch(event) ?: false
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                view.performClick()
            }
            handled
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindSpaceTouch(v: View) {
        var downX = 0f
        var switched = false
        var longDotFired = false
        val dotRunnable = Runnable {
            if (!switched) {
                longDotFired = true
                controller?.handleAction(KeyAction.Character("."))
            }
        }
        v.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    switched = false
                    longDotFired = false
                    provideFeedback(view)
                    if (longPressSpaceDot) handler.postDelayed(dotRunnable, longPressDelayMs.toLong())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - downX
                    if (!switched && abs(dx) > dp(44)) {
                        when (spaceSwipeMode) {
                            1 -> { switched = true; handler.removeCallbacks(dotRunnable); controller?.handleAction(KeyAction.SwitchLanguage) }
                            2 -> {
                                val steps = (dx / dp(28)).toInt()
                                if (steps != 0) {
                                    switched = true // prevent space on up; cursor moved
                                    handler.removeCallbacks(dotRunnable)
                                    controller?.moveCursor(steps)
                                    downX = event.x
                                    switched = false // allow continuous move, but mark moved
                                    longDotFired = true // reuse flag to suppress space
                                }
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(dotRunnable)
                    if (!switched && !longDotFired) {
                        val now = System.currentTimeMillis()
                        if (doubleSpacePeriod && now - lastSpaceTapMs < 320L) {
                            controller?.handleAction(KeyAction.Backspace)
                            controller?.handleAction(KeyAction.Character("."))
                            controller?.handleAction(KeyAction.Space)
                            lastSpaceTapMs = 0L
                        } else {
                            controller?.handleAction(KeyAction.Space)
                            lastSpaceTapMs = now
                        }
                    }
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(dotRunnable)
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindKeyTouch(v: View, key: Key) {
        v.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    longPressFired = false
                    provideFeedback(view)
                    showKeyPreview(view, key)
                    if (key.popupActions.isNotEmpty()) {
                        handler.postDelayed({
                            longPressFired = true
                            dismissKeyPreview()
                            showPopup(view, key.popupActions)
                        }, longPressDelayMs.toLong())
                    } else if (key.longPressAction != null) {
                        handler.postDelayed({
                            longPressFired = true
                            dismissKeyPreview()
                            controller?.handleAction(key.longPressAction)
                        }, longPressDelayMs.toLong())
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacksAndMessages(null)
                    dismissKeyPreview()
                    if (!longPressFired) controller?.handleAction(key.action)
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacksAndMessages(null)
                    dismissKeyPreview()
                    dismissPopup()
                    true
                }
                else -> false
            }
        }
    }

    private fun showPopup(anchor: View, actions: List<Pair<String, KeyAction>>) {
        dismissPopup()
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.keynova_key_bg))
            val p = dp(4)
            setPadding(p, p, p, p)
        }
        val pw = PopupWindow(row, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            elevation = dp(8).toFloat()
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, R.color.keynova_key_bg)))
        }
        actions.forEach { (label, action) ->
            val item = TextView(context).apply {
                text = label
                setTextColor(ContextCompat.getColor(context, R.color.keynova_text))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                gravity = Gravity.CENTER
                minWidth = dp(40)
                minHeight = dp(40)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener {
                    controller?.handleAction(action)
                    pw.dismiss()
                }
            }
            row.addView(item)
        }
        popup = pw
        pw.showAsDropDown(anchor, 0, -anchor.height - dp(48))
    }

    private fun dismissPopup() {
        popup?.dismiss()
        popup = null
    }

    private fun showKeyPreview(anchor: View, key: Key) {
        if (!keyPreviewEnabled) return
        if (key.action is KeyAction.Space || key.action is KeyAction.Enter || key.action is KeyAction.Backspace || key.action is KeyAction.Shift) return
        dismissKeyPreview()
        val tv = TextView(context).apply {
            text = key.label
            setTextColor(ContextCompat.getColor(context, R.color.keynova_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            minWidth = dp(52)
            minHeight = dp(52)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundResource(R.drawable.key_bg)
        }
        val pw = PopupWindow(tv, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false).apply {
            elevation = dp(6).toFloat()
            isClippingEnabled = false
        }
        keyPreview = pw
        pw.showAsDropDown(anchor, (anchor.width - dp(52)) / 2, -anchor.height - dp(56))
    }

    private fun dismissKeyPreview() {
        keyPreview?.dismiss()
        keyPreview = null
    }

    private fun provideFeedback(view: View) {
        if (vibrationEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (soundEnabled) audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, soundVolumePercent / 100f)
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private const val LONG_PRESS_MS = 350L // fallback; actual delay from prefs
    }
}
