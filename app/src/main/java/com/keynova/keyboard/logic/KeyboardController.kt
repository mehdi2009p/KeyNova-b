package com.keynova.keyboard.logic

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.keynova.keyboard.model.KeyAction
import com.keynova.keyboard.model.KeyboardLanguage
import com.keynova.keyboard.model.KeyboardMode
import com.keynova.keyboard.model.ShiftState

class KeyboardController(
    private val inputConnectionProvider: () -> InputConnection?,
    private val editorInfoProvider: () -> EditorInfo?,
    private val onStateChanged: () -> Unit,
    private val onOpenSettings: () -> Unit = {},
    private val autoCapitalizeProvider: () -> Boolean = { true }
) {
    var mode: KeyboardMode = KeyboardMode.LETTERS
        private set

    var language: KeyboardLanguage = KeyboardLanguage.FA
        private set

    var shiftState: ShiftState = ShiftState.OFF
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var backspaceRepeating = false
    private val backspaceRunnable = object : Runnable {
        override fun run() {
            if (!backspaceRepeating) return
            deleteOnce()
            handler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    fun onStartInput() {
        mode = KeyboardMode.LETTERS
        // preserve selected language across input sessions for now
        if (language != KeyboardLanguage.FA && language != KeyboardLanguage.EN) language = KeyboardLanguage.FA
        shiftState = ShiftState.OFF
        stopBackspaceRepeat()
        applyAutoCapitalize()
        onStateChanged()
    }

    fun currentInputConnection(): InputConnection? = inputConnectionProvider()

    fun moveCursor(offset: Int) {
        val ic = inputConnectionProvider() ?: return
        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
        val selStart = extracted?.selectionStart ?: return
        val selEnd = extracted?.selectionEnd ?: selStart
        val newPos = (if (offset < 0) selStart else selEnd) + offset
        val clamped = newPos.coerceIn(0, extracted.text?.length ?: 0)
        ic.setSelection(clamped, clamped)
    }

    fun handleAction(action: KeyAction) {
        val ic = inputConnectionProvider()
        when (action) {
            is KeyAction.Character -> {
                ic?.commitText(action.value, 1)
                if (mode == KeyboardMode.LETTERS && language == KeyboardLanguage.EN && shiftState == ShiftState.ON) {
                    shiftState = ShiftState.OFF
                    onStateChanged()
                }
                maybeAutoCapitalize(action.value)
            }
            KeyAction.Space -> { ic?.commitText(" ", 1); maybeAutoCapitalize(" ") }
            KeyAction.Zwnj -> ic?.commitText("\u200C", 1)
            KeyAction.Enter -> if (ic != null) handleEnter(ic)
            KeyAction.Backspace -> deleteOnce()
            KeyAction.Shift -> {
                if (mode == KeyboardMode.LETTERS && language == KeyboardLanguage.EN) {
                    shiftState = if (shiftState == ShiftState.OFF) ShiftState.ON else ShiftState.OFF
                    onStateChanged()
                }
            }
            KeyAction.ModeSymbols -> {
                mode = KeyboardMode.SYMBOLS
                onStateChanged()
            }
            KeyAction.ModeSymbols2 -> {
                mode = KeyboardMode.SYMBOLS2
                onStateChanged()
            }
            KeyAction.ModeNumpad -> {
                mode = KeyboardMode.NUMPAD
                onStateChanged()
            }
            KeyAction.ModeLetters -> {
                mode = KeyboardMode.LETTERS
                onStateChanged()
            }
            KeyAction.SwitchLanguage -> {
                language = if (language == KeyboardLanguage.FA) KeyboardLanguage.EN else KeyboardLanguage.FA
                shiftState = ShiftState.OFF
                onStateChanged()
            }
            KeyAction.OpenSettings -> onOpenSettings()
        }
    }

    fun onBackspaceTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleAction(KeyAction.Backspace)
                backspaceRepeating = true
                handler.postDelayed(backspaceRunnable, REPEAT_DELAY_MS)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopBackspaceRepeat()
                return true
            }
        }
        return false
    }

    fun stopBackspaceRepeat() {
        backspaceRepeating = false
        handler.removeCallbacks(backspaceRunnable)
    }

    private fun deleteOnce() {
        val ic = inputConnectionProvider() ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun applyAutoCapitalize() {
        if (!autoCapitalizeProvider()) return
        if (mode != KeyboardMode.LETTERS || language != KeyboardLanguage.EN) return
        if (shiftState != ShiftState.OFF) return
        val ic = inputConnectionProvider()
        val before = ic?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        val shouldCap = before.isEmpty() || Regex("(?:[.!?]\\s+|\\n\\s*)$").containsMatchIn(before)
        if (shouldCap) shiftState = ShiftState.ON
    }

    private fun maybeAutoCapitalize(@Suppress("UNUSED_PARAMETER") last: String) {
        val before = shiftState
        applyAutoCapitalize()
        if (shiftState != before) onStateChanged()
    }

    private fun handleEnter(ic: InputConnection) {
        val ei = editorInfoProvider()
        val action = ei?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = (ei?.imeOptions?.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION) ?: 0) != 0
        val inputType = ei?.inputType ?: 0
        val multiLine = (inputType and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0

        if (multiLine && (action == EditorInfo.IME_ACTION_NONE || noEnterAction)) {
            ic.commitText("\n", 1)
            maybeAutoCapitalize("\n")
            return
        }

        when (action) {
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_PREVIOUS -> ic.performEditorAction(action)
            else -> if (multiLine) { ic.commitText("\n", 1); maybeAutoCapitalize("\n") } else ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
        }
    }

    companion object {
        private const val REPEAT_DELAY_MS = 350L
        private const val REPEAT_INTERVAL_MS = 45L
    }
}
