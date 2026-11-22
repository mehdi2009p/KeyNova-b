package com.keynova.keyboard.layout

import com.keynova.keyboard.model.Key
import com.keynova.keyboard.model.KeyAction
import com.keynova.keyboard.model.KeyboardLanguage
import com.keynova.keyboard.model.KeyboardMode
import com.keynova.keyboard.model.ShiftState

object KeyboardLayouts {

    fun rowsFor(mode: KeyboardMode, language: KeyboardLanguage, shift: ShiftState): List<List<Key>> {
        return when (mode) {
            KeyboardMode.LETTERS -> if (language == KeyboardLanguage.FA) persianRows() else englishRows(shift)
            KeyboardMode.SYMBOLS -> symbolRows(language, secondary = false)
            KeyboardMode.SYMBOLS2 -> symbolRows(language, secondary = true)
            KeyboardMode.NUMPAD -> numpadRows(language)
        }
    }

    private fun persianRows(): List<List<Key>> {
        val row1 = listOf(
            fa("چ"), fa("ج"), fa("ح"), fa("خ"), fa("ه"),
            fa("ع", "غ"), fa("ف"), fa("ق"), fa("ث"), fa("ص", "ض")
        )
        val row2 = listOf(
            fa("پ"), fa("م"), fa("ن"), fa("ت"), fa("ا"),
            fa("ل"), fa("ب"), fa("ی"), fa("س"), fa("ش")
        )
        val row3 = listOf(
            Key(label = "⌫", action = KeyAction.Backspace, weight = 1.35f, isSpecial = true),
            fa("گ"), fa("ک"), fa("و"), fa("د", "ذ"), fa("ر"), fa("ز", "ژ"), fa("ط"), fa("ظ")
        )
        val row4 = listOf(
            Key(label = "↵", action = KeyAction.Enter, weight = 1.2f, isSpecial = true, isAccent = true),
            Key(label = "؟۱۲۳", action = KeyAction.ModeSymbols, weight = 1.1f, isSpecial = true),
            Key(label = "#+=", action = KeyAction.ModeSymbols2, weight = 1.1f, isSpecial = true),
            Key(label = ".", action = KeyAction.Character("."), weight = 0.9f, popupActions = persianDotPopup()),
            Key(label = "فاصله", action = KeyAction.Space, weight = 3.0f),
            Key(label = "،", action = KeyAction.Character("،"), weight = 0.9f, longPressLabel = "ZNJ", longPressAction = KeyAction.Zwnj)
        )
        return listOf(row1, row2, row3, row4)
    }

    private fun englishRows(shift: ShiftState): List<List<Key>> {
        val upper = shift == ShiftState.ON
        fun c(ch: Char): Key {
            val out = if (upper) ch.uppercaseChar().toString() else ch.lowercaseChar().toString()
            return Key(label = out, action = KeyAction.Character(out))
        }

        val row1 = "qwertyuiop".map { c(it) }
        val row2 = "asdfghjkl".map { c(it) }
        val row3 = buildList {
            add(Key(label = "⌫", action = KeyAction.Backspace, weight = 1.5f, isSpecial = true))
            "zxcvbnm".forEach { add(c(it)) }
            add(Key(label = if (upper) "⇪" else "⇧", action = KeyAction.Shift, weight = 1.5f, isSpecial = true, isAccent = upper))
        }
        val row4 = listOf(
            Key(label = "↵", action = KeyAction.Enter, weight = 1.2f, isSpecial = true, isAccent = true),
            Key(label = "?123", action = KeyAction.ModeSymbols, weight = 1.1f, isSpecial = true),
            Key(label = "#+=", action = KeyAction.ModeSymbols2, weight = 1.1f, isSpecial = true),
            Key(label = ".", action = KeyAction.Character("."), weight = 0.9f, popupActions = englishDotPopup()),
            Key(label = "space", action = KeyAction.Space, weight = 3.0f),
            Key(label = ",", action = KeyAction.Character(","), weight = 0.9f)
        )
        return listOf(row1, row2, row3, row4)
    }

    private fun symbolRows(language: KeyboardLanguage, secondary: Boolean): List<List<Key>> {
        val fa = language == KeyboardLanguage.FA
        val abcLabel = if (fa) "اب‌ج" else "ABC"
        val comma = if (fa) "،" else ","
        val space = if (fa) "فاصله" else "space"

        val row1 = if (!secondary) {
            "1234567890".map { Key(it.toString(), KeyAction.Character(it.toString())) }
        } else {
            listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=").map { Key(it, KeyAction.Character(it)) }
        }
        val row2 = if (!secondary) {
            listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/").map { Key(it, KeyAction.Character(it)) }
        } else {
            listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•").map { Key(it, KeyAction.Character(it)) }
        }
        val row3 = if (!secondary) {
            listOf(
                Key("⌫", KeyAction.Backspace, 1.35f, isSpecial = true),
                Key("*", KeyAction.Character("*")),
                Key("\"", KeyAction.Character("\"")),
                Key("'", KeyAction.Character("'")),
                Key(":", KeyAction.Character(":")),
                if (fa) Key("؛", KeyAction.Character("؛")) else Key(";", KeyAction.Character(";")),
                Key("!", KeyAction.Character("!")),
                if (fa) Key("؟", KeyAction.Character("؟")) else Key("?", KeyAction.Character("?")),
                Key("⌨", KeyAction.ModeNumpad, 1.2f, isSpecial = true)
            )
        } else {
            listOf(
                Key("⌫", KeyAction.Backspace, 1.35f, isSpecial = true),
                Key(".", KeyAction.Character(".")),
                Key(",", KeyAction.Character(",")),
                if (fa) Key("؟", KeyAction.Character("؟")) else Key("?", KeyAction.Character("?")),
                Key("!", KeyAction.Character("!")),
                Key("'", KeyAction.Character("'")),
                Key("\"", KeyAction.Character("\"")),
                if (fa) Key("؛", KeyAction.Character("؛")) else Key(";", KeyAction.Character(";")),
                Key("⌨", KeyAction.ModeNumpad, 1.2f, isSpecial = true)
            )
        }
        val row4 = listOf(
            Key("↵", KeyAction.Enter, 1.2f, isSpecial = true, isAccent = true),
            Key(abcLabel, KeyAction.ModeLetters, 1.1f, isSpecial = true),
            Key(if (secondary) "?123" else "#+=", if (secondary) KeyAction.ModeSymbols else KeyAction.ModeSymbols2, 1.1f, isSpecial = true),
            Key(".", KeyAction.Character("."), 0.9f, popupActions = if (fa) persianDotPopup() else englishDotPopup()),
            Key(space, KeyAction.Space, 3.0f),
            Key(comma, KeyAction.Character(comma), 0.9f, longPressLabel = if (fa) "ZNJ" else null, longPressAction = if (fa) KeyAction.Zwnj else null)
        )
        return listOf(row1, row2, row3, row4)
    }

    private fun numpadRows(language: KeyboardLanguage): List<List<Key>> {
        val fa = language == KeyboardLanguage.FA
        val abc = if (fa) "اب‌ج" else "ABC"
        val row1 = listOf(k("1"), k("2"), k("3"), Key("⌫", KeyAction.Backspace, isSpecial = true))
        val row2 = listOf(k("4"), k("5"), k("6"), Key(if (fa) "،" else ",", KeyAction.Character(if (fa) "،" else ",")))
        val row3 = listOf(k("7"), k("8"), k("9"), Key("فاصله".takeIf { fa } ?: "space", KeyAction.Space))
        val row4 = listOf(Key(abc, KeyAction.ModeLetters, isSpecial = true), k("0"), Key(".", KeyAction.Character(".")), Key("↵", KeyAction.Enter, isSpecial = true, isAccent = true))
        return listOf(row1, row2, row3, row4)
    }

    private fun persianDotPopup(): List<Pair<String, KeyAction>> = listOf(
        "َ" to KeyAction.Character("َ"), "ُ" to KeyAction.Character("ُ"), "ِ" to KeyAction.Character("ِ"),
        "ّ" to KeyAction.Character("ّ"), "ً" to KeyAction.Character("ً"), "؟" to KeyAction.Character("؟"),
        "!" to KeyAction.Character("!"), "؛" to KeyAction.Character("؛"), ":" to KeyAction.Character(":"),
        "-" to KeyAction.Character("-")
    )

    private fun englishDotPopup(): List<Pair<String, KeyAction>> = listOf(
        "!" to KeyAction.Character("!"), "?" to KeyAction.Character("?"), "#" to KeyAction.Character("#"),
        "@" to KeyAction.Character("@"), "-" to KeyAction.Character("-"), "_" to KeyAction.Character("_"),
        ":" to KeyAction.Character(":")
    )

    private fun fa(label: String, longPress: String? = null): Key {
        return Key(
            label = label,
            action = KeyAction.Character(label),
            longPressLabel = longPress,
            longPressAction = longPress?.let { KeyAction.Character(it) }
        )
    }

    private fun k(label: String) = Key(label, KeyAction.Character(label))
}
