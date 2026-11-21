package com.keynova.keyboard.model

sealed interface KeyAction {
    data class Character(val value: String) : KeyAction
    data object Shift : KeyAction
    data object Backspace : KeyAction
    data object Enter : KeyAction
    data object Space : KeyAction
    data object ModeSymbols : KeyAction
    data object ModeLetters : KeyAction
    data object SwitchLanguage : KeyAction
    data object Zwnj : KeyAction
    data object OpenSettings : KeyAction
}

data class Key(
    val label: String,
    val action: KeyAction,
    val weight: Float = 1f,
    val isSpecial: Boolean = false,
    val isAccent: Boolean = false,
    val longPressLabel: String? = null,
    val longPressAction: KeyAction? = null
)

enum class KeyboardMode {
    LETTERS,
    SYMBOLS
}

enum class KeyboardLanguage {
    FA,
    EN
}

enum class ShiftState {
    OFF,
    ON
}
