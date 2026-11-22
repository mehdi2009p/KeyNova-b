package com.keynova.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
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

    init {
        orientation = VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        setBackgroundColor(ContextCompat.getColor(context, R.color.keynova_background))
        val pad = dp(4)
        setPadding(pad, dp(10), pad, dp(38))
        isClickable = true
        isFocusable = false
        isHapticFeedbackEnabled = true
    }

    fun render() {
        dismissPopup()
        removeAllViews()
        val ctrl = controller ?: return
        val rows = KeyboardLayouts.rowsFor(ctrl.mode, ctrl.language, ctrl.shiftState)
        rows.forEach { rowKeys -> addView(createRow(rowKeys)) }
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
        val keyH = prefs.keyHeightDp
        val container = android.widget.FrameLayout(context).apply {
            val lp = LayoutParams(0, dp(keyH), key.weight)
            val hGap = dp(2)
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
                KeyAction.Space -> if ((controller?.language?.name ?: "FA") == "FA") "فاصله" else "space"
                else -> key.label
            }
            contentDescription = when (key.action) {
                KeyAction.Space -> "Space"
                KeyAction.Backspace -> "Backspace"
                KeyAction.Enter -> "Enter"
                else -> key.label
            }
            setTextColor(ContextCompat.getColor(context, R.color.keynova_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (key.label.length > 3) 12f else 20f)
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

        if (key.longPressLabel != null && key.popupActions.isEmpty()) {
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
        v.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    switched = false
                    provideFeedback(view)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - downX
                    if (!switched && abs(dx) > dp(36)) {
                        switched = true
                        controller?.handleAction(KeyAction.SwitchLanguage)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!switched) controller?.handleAction(KeyAction.Space)
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
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
                    if (key.popupActions.isNotEmpty()) {
                        handler.postDelayed({
                            longPressFired = true
                            showPopup(view, key.popupActions)
                        }, LONG_PRESS_MS)
                    } else if (key.longPressAction != null) {
                        handler.postDelayed({
                            longPressFired = true
                            controller?.handleAction(key.longPressAction)
                        }, LONG_PRESS_MS)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacksAndMessages(null)
                    if (!longPressFired) controller?.handleAction(key.action)
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacksAndMessages(null)
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

    private fun provideFeedback(view: View) {
        if (prefs.vibrationEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (prefs.soundEnabled) {
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.6f)
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private const val LONG_PRESS_MS = 350L
    }
}
