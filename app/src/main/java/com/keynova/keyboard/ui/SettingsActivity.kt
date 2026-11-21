package com.keynova.keyboard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.keynova.keyboard.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var statusEnabled: TextView
    private lateinit var statusSelected: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        statusEnabled = findViewById(R.id.statusEnabled)
        statusSelected = findViewById(R.id.statusSelected)

        findViewById<Button>(R.id.btnEnableKeyboard).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<Button>(R.id.btnSelectKeyboard).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = isKeyNovaEnabled()
        val selected = isKeyNovaSelected()

        statusEnabled.text = if (enabled) {
            getString(R.string.status_enabled)
        } else {
            getString(R.string.status_not_enabled)
        }
        statusEnabled.setTextColor(
            getColor(if (enabled) R.color.keynova_status_ok else R.color.keynova_status_warn)
        )

        val selectedText = when {
            !enabled -> getString(R.string.status_not_enabled)
            selected -> getString(R.string.status_selected)
            else -> getString(R.string.status_not_selected)
        }
        statusSelected.text = selectedText
        statusSelected.setTextColor(
            getColor(if (enabled && selected) R.color.keynova_status_ok else R.color.keynova_status_warn)
        )
    }

    private fun isKeyNovaEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun isKeyNovaSelected(): Boolean {
        val current = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return current?.startsWith(packageName) == true
    }
}
