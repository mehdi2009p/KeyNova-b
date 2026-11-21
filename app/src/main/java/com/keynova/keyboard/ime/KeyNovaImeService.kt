package com.keynova.keyboard.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.keynova.keyboard.logic.KeyboardController
import com.keynova.keyboard.ui.KeyboardView
import com.keynova.keyboard.ui.SettingsActivity

class KeyNovaImeService : InputMethodService() {

    private var keyboardView: KeyboardView? = null
    private lateinit var controller: KeyboardController

    override fun onCreate() {
        super.onCreate()
        controller = KeyboardController(
            inputConnectionProvider = { currentInputConnection },
            editorInfoProvider = { currentInputEditorInfo },
            onStateChanged = { keyboardView?.render() },
            onOpenSettings = {
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        )
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
        if (::controller.isInitialized) {
            controller.stopBackspaceRepeat()
        }
        super.onDestroy()
    }
}
