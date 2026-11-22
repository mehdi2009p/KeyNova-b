package com.keynova.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
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

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.keynova_background))
        val pad = dp(4)
        setPadding(pad, dp(6), pad, dp(8))
        isClickable = true
        isFocusable = false
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
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (key.label.length > 3) 11f else 18f)
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
            KeyAction.Space -> bindSpaceTouch(tv)
            else -> bindKeyTouch(tv, key)
        }
        return tv
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindSpaceTouch(tv: TextView) {
        var downX = 0f
        var switched = false
        tv.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    switched = false
                    v.isPressed = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - downX
                    if (!switched && abs(dx) > dp(36)) {
                        switched = true
                        controller?.handleAction(KeyAction.SwitchLanguage)
                        v.isPressed = false
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    if (!switched) controller?.handleAction(KeyAction.Space)
                    v.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindKeyTouch(tv: TextView, key: Key) {
        tv.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    longPressFired = false
                    if (key.popupActions.isNotEmpty()) {
                        handler.postDelayed({
                            longPressFired = true
                            showPopup(v, key.popupActions)
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
                    v.isPressed = false
                    if (!longPressFired) controller?.handleAction(key.action)
                    v.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacksAndMessages(null)
                    v.isPressed = false
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

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private const val LONG_PRESS_MS = 350L
    }
}
