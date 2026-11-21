package com.keynova.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.keynova.keyboard.R
import com.keynova.keyboard.layout.KeyboardLayouts
import com.keynova.keyboard.logic.KeyboardController
import com.keynova.keyboard.model.Key
import com.keynova.keyboard.model.KeyAction

class KeyboardView(context: Context) : LinearLayout(context) {

    var controller: KeyboardController? = null
        set(value) {
            field = value
            render()
        }

    private val handler = Handler(Looper.getMainLooper())
    private var longPressFired = false

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.keynova_background))
        val pad = dp(4)
        setPadding(pad, dp(6), pad, dp(8))
        isClickable = true
        isFocusable = false
    }

    fun render() {
        removeAllViews()
        val ctrl = controller ?: return
        val rows = KeyboardLayouts.rowsFor(ctrl.mode, ctrl.language, ctrl.shiftState)
        rows.forEach { rowKeys ->
            addView(createRow(rowKeys))
        }
    }

    private fun createRow(keys: List<Key>): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(3)
                bottomMargin = dp(3)
            }
            gravity = Gravity.CENTER
        }
        keys.forEach { row.addView(createKeyView(it)) }
        return row
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createKeyView(key: Key): TextView {
        val tv = TextView(context).apply {
            text = when (key.action) {
                KeyAction.Space -> ""
                else -> key.label
            }
            contentDescription = when (key.action) {
                KeyAction.Space -> "Space"
                KeyAction.Backspace -> "Backspace"
                KeyAction.Enter -> "Enter"
                else -> key.label
            }
            setTextColor(ContextCompat.getColor(context, R.color.keynova_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (key.label.length > 3) 12f else 18f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isAllCaps = false
            setBackgroundResource(
                when {
                    key.isAccent -> R.drawable.key_bg_accent
                    key.isSpecial -> R.drawable.key_bg_special
                    else -> R.drawable.key_bg
                }
            )
            val lp = LayoutParams(0, dp(48), key.weight)
            val hGap = dp(2)
            lp.setMargins(hGap, 0, hGap, 0)
            layoutParams = lp
            minHeight = dp(48)
            setPadding(dp(2), dp(6), dp(2), dp(6))
        }

        when (key.action) {
            KeyAction.Backspace -> {
                tv.setOnTouchListener { v, event ->
                    val handled = controller?.onBackspaceTouch(event) ?: false
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> v.isPressed = true
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.isPressed = false
                            v.performClick()
                        }
                    }
                    handled
                }
            }
            else -> {
                tv.setOnTouchListener { v, event ->
                    val hasLong = key.longPressAction != null
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            v.isPressed = true
                            longPressFired = false
                            if (hasLong) {
                                handler.postDelayed({
                                    longPressFired = true
                                    controller?.handleAction(key.longPressAction!!)
                                }, LONG_PRESS_MS)
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            handler.removeCallbacksAndMessages(null)
                            v.isPressed = false
                            if (!longPressFired) {
                                controller?.handleAction(key.action)
                            }
                            v.performClick()
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            handler.removeCallbacksAndMessages(null)
                            v.isPressed = false
                            true
                        }
                        else -> false
                    }
                }
            }
        }
        return tv
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private const val LONG_PRESS_MS = 350L
    }
}
