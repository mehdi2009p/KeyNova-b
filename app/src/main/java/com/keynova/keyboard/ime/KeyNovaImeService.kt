package com.keynova.keyboard.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.keynova.keyboard.logic.KeyboardController
import com.keynova.keyboard.settings.KeyboardPrefs
import com.keynova.keyboard.ui.KeyboardView
import com.keynova.keyboard.ui.SettingsActivity

class KeyNovaImeService : InputMethodService() {

    private var keyboardView: KeyboardView? = null
    private lateinit var controller: KeyboardController
    private lateinit var prefs: KeyboardPrefs
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        prefs = KeyboardPrefs(this)
        controller = KeyboardController(
            inputConnectionProvider = { currentInputConnection },
            editorInfoProvider = { currentInputEditorInfo },
            onStateChanged = { keyboardView?.render() },
            onOpenSettings = {
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            },
            autoCapitalizeProvider = { prefs.autoCapitalize },
            onTextChanged = { keyboardView?.post { keyboardView?.updateSuggestions() } }
        )
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) {
                prefs.addClipboardItem(text)
                keyboardView?.post { keyboardView?.refreshClipboard() }
            }
        }.also { cm.addPrimaryClipChangedListener(it) }
    }

    override fun onCreateInputView(): View {
        val view = KeyboardView(this)
        view.controller = controller
        keyboardView = view
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        controller.onStartInput()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        controller.onStartInput()
        keyboardView?.render()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        controller.stopBackspaceRepeat()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardListener?.let { cm?.removePrimaryClipChangedListener(it) }
        } catch (_: Exception) {}
        try {
            if (::controller.isInitialized) {
                controller.stopBackspaceRepeat()
            }
        } catch (_: Exception) {}
        try {
            super.onDestroy()
        } catch (_: Exception) {}
    }
}
