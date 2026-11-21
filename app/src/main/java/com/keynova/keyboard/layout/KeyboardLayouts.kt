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
            KeyboardMode.SYMBOLS -> symbolRows(language)
        }
    }

    private fun persianRows(): List<List<Key>> {
        // Preserve legacy KeyNova Persian layout (identity). Long-press alternates preserved/improved.
        val row1 = listOf(
            fa("چ"), fa("ج"), fa("ح"), fa("خ"), fa("ه"),
            fa("ع", "غ"), fa("ف"), fa("ق"), fa("ث"), fa("ص", "ض")
        )
        val row2 = listOf(
            fa("پ"), fa("م"), fa("ن"), fa("ت"), fa("ا"),
            fa("ل"), fa("ب"), fa("ی"), fa("س"), fa("ش")
        )
        val row3 = listOf(
            Key(label = "⇧", action = KeyAction.Shift, weight = 1.35f, isSpecial = true),
            fa("گ"), fa("ک"), fa("و"), fa("د", "ذ"), fa("ر"), fa("ز", "ژ"), fa("ط"), fa("ظ"),
            Key(label = "⌫", action = KeyAction.Backspace, weight = 1.35f, isSpecial = true)
        )
        val row4 = listOf(
            Key(label = "؟۱۲۳", action = KeyAction.ModeSymbols, weight = 1.4f, isSpecial = true),
            Key(label = "FA/EN", action = KeyAction.SwitchLanguage, weight = 1.2f, isSpecial = true),
            Key(label = "،", action = KeyAction.Character("،"), weight = 0.9f),
            Key(label = "فاصله", action = KeyAction.Space, weight = 3.4f),
            Key(label = "ZNJ", action = KeyAction.Zwnj, weight = 0.9f, isSpecial = true, longPressLabel = "␣", longPressAction = KeyAction.Space),
            Key(label = ".", action = KeyAction.Character("."), weight = 0.9f),
            Key(label = "↵", action = KeyAction.Enter, weight = 1.4f, isSpecial = true, isAccent = true)
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
            add(Key(label = if (upper) "⇧" else "⇧", action = KeyAction.Shift, weight = 1.5f, isSpecial = true, isAccent = upper))
            "zxcvbnm".forEach { add(c(it)) }
            add(Key(label = "⌫", action = KeyAction.Backspace, weight = 1.5f, isSpecial = true))
        }
        val row4 = listOf(
            Key(label = "?123", action = KeyAction.ModeSymbols, weight = 1.4f, isSpecial = true),
            Key(label = "FA/EN", action = KeyAction.SwitchLanguage, weight = 1.2f, isSpecial = true),
            Key(label = ",", action = KeyAction.Character(","), weight = 0.9f),
            Key(label = "space", action = KeyAction.Space, weight = 3.4f),
            Key(label = ".", action = KeyAction.Character("."), weight = 0.9f),
            Key(label = "↵", action = KeyAction.Enter, weight = 1.4f, isSpecial = true, isAccent = true)
        )
        return listOf(row1, row2, row3, row4)
    }

    private fun symbolRows(language: KeyboardLanguage): List<List<Key>> {
        val row1 = "1234567890".map { ch ->
            val s = ch.toString()
            Key(label = s, action = KeyAction.Character(s))
        }
        val row2 = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")").map {
            Key(label = it, action = KeyAction.Character(it))
        }
        val row3 = listOf(
            Key(label = "*", action = KeyAction.Character("*")),
            Key(label = "\"", action = KeyAction.Character("\"")),
            Key(label = "'", action = KeyAction.Character("'")),
            Key(label = ":", action = KeyAction.Character(":")),
            Key(label = "؛", action = KeyAction.Character("؛")),
            Key(label = "!", action = KeyAction.Character("!")),
            Key(label = "؟", action = KeyAction.Character("؟")),
            Key(label = "/", action = KeyAction.Character("/")),
            Key(label = "⌫", action = KeyAction.Backspace, weight = 1.3f, isSpecial = true)
        )
        val abcLabel = if (language == KeyboardLanguage.FA) "اب‌ج" else "ABC"
        val comma = if (language == KeyboardLanguage.FA) "،" else ","
        val row4 = listOf(
            Key(label = abcLabel, action = KeyAction.ModeLetters, weight = 1.4f, isSpecial = true),
            Key(label = "FA/EN", action = KeyAction.SwitchLanguage, weight = 1.2f, isSpecial = true),
            Key(label = comma, action = KeyAction.Character(comma), weight = 0.9f),
            Key(label = if (language == KeyboardLanguage.FA) "فاصله" else "space", action = KeyAction.Space, weight = 3.4f),
            Key(label = ".", action = KeyAction.Character("."), weight = 0.9f),
            Key(label = "↵", action = KeyAction.Enter, weight = 1.4f, isSpecial = true, isAccent = true)
        )
        return listOf(row1, row2, row3, row4)
    }

    private fun fa(label: String, longPress: String? = null): Key {
        return Key(
            label = label,
            action = KeyAction.Character(label),
            longPressLabel = longPress,
            longPressAction = longPress?.let { KeyAction.Character(it) }
        )
    }
}
