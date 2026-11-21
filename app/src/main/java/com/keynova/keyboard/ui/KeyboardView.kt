package com.keynova.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
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

    private val keyHeightDp = 48
    private val horizontalGapDp = 4
    private val verticalGapDp = 6
    private val rowPaddingDp = 3

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
        val rows = KeyboardLayouts.rowsFor(ctrl.mode, ctrl.shiftState)
        rows.forEachIndexed { index, rowKeys ->
            addView(createRow(rowKeys))
        }
    }

    private fun createRow(keys: List<Key>): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(rowPaddingDp)
                bottomMargin = dp(rowPaddingDp)
            }
            gravity = Gravity.CENTER
        }
        keys.forEach { key ->
            row.addView(createKeyView(key))
        }
        return row
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createKeyView(key: Key): TextView {
        val tv = TextView(context).apply {
            text = when (key.action) {
                KeyAction.Space -> ""
                else -> key.label
            }
            if (key.action == KeyAction.Space) {
                contentDescription = "Space"
            }
            setTextColor(ContextCompat.getColor(context, R.color.keynova_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (key.label.length > 2) 14f else 18f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isAllCaps = false
            val bgRes = when {
                key.isAccent -> R.drawable.key_bg_accent
                key.isSpecial -> R.drawable.key_bg_special
                else -> R.drawable.key_bg
            }
            setBackgroundResource(bgRes)
            val lp = LayoutParams(0, dp(keyHeightDp), key.weight)
            val hGap = dp(horizontalGapDp) / 2
            lp.setMargins(hGap, 0, hGap, 0)
            layoutParams = lp
            minHeight = dp(keyHeightDp)
        }

        when (key.action) {
            KeyAction.Backspace -> {
                tv.setOnTouchListener { v, event ->
                    val handled = controller?.onBackspaceTouch(event) ?: false
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        v.isPressed = true
                    } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                        v.isPressed = false
                        v.performClick()
                    }
                    handled
                }
            }
            else -> {
                tv.setOnClickListener {
                    controller?.handleAction(key.action)
                }
            }
        }
        return tv
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}
