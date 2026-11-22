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
        // Rows ordered for LTR rendering to produce standard Persian visual layout
        val row1 = listOf(
            fa("ص", "ض"), fa("ث"), fa("ق"), fa("ف"), fa("ع", "غ"),
            fa("ه"), fa("خ"), fa("ح"), fa("ج"), fa("چ")
        )
        val row2 = listOf(
            fa("ش"), fa("س"), fa("ی"), fa("ب"), fa("ل"),
            fa("ا"), fa("ت"), fa("ن"), fa("م"), fa("پ")
        )
        val row3 = listOf(
            fa("ظ"), fa("ط"), fa("ز", "ژ"), fa("ر"), fa("د", "ذ"),
            fa("و"), fa("ک"), fa("گ"),
            Key(label = "⌫", action = KeyAction.Backspace, weight = 1.35f, isSpecial = true)
        )
        val row4 = listOf(
            Key(label = "#+=", action = KeyAction.ModeSymbols2, weight = 1.0f, isSpecial = true),
            Key(label = "⌨", action = KeyAction.ModeNumpad, weight = 1.0f, isSpecial = true),
            Key(label = "،", action = KeyAction.Character("،"), weight = 1.0f, longPressLabel = "ZNJ", longPressAction = KeyAction.Zwnj),
            Key(label = "فاصله", action = KeyAction.Space, weight = 3.6f),
            Key(label = ".", action = KeyAction.Character("."), weight = 1.0f, popupActions = persianDotPopup()),
            Key(label = "↵", action = KeyAction.Enter, weight = 2.0f, isSpecial = true, isAccent = true)
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
            add(Key(label = "⇧", action = KeyAction.Shift, weight = 1.5f, isSpecial = true, isAccent = upper))
            "zxcvbnm".forEach { add(c(it)) }
            add(Key(label = "⌫", action = KeyAction.Backspace, weight = 1.5f, isSpecial = true))
        }
        val row4 = listOf(
            Key(label = "#+=", action = KeyAction.ModeSymbols2, weight = 1.0f, isSpecial = true),
            Key(label = "⌨", action = KeyAction.ModeNumpad, weight = 1.0f, isSpecial = true),
            Key(label = ",", action = KeyAction.Character(","), weight = 1.0f),
            Key(label = "space", action = KeyAction.Space, weight = 3.6f),
            Key(label = ".", action = KeyAction.Character("."), weight = 1.0f, popupActions = englishDotPopup()),
            Key(label = "↵", action = KeyAction.Enter, weight = 2.0f, isSpecial = true, isAccent = true)
        )
        return listOf(row1, row2, row3, row4)
    }

    private fun symbolRows(language: KeyboardLanguage, secondary: Boolean): List<List<Key>> {
        val fa = language == KeyboardLanguage.FA
        val abcLabel = if (fa) "اب‌ج" else "ABC"
        val comma = if (fa) "،" else ","
        val space = if (fa) "فاصله" else "space"

        val row1 = listOf("1","2","3","4","5","6","7","8","9","0").map { Key(it, KeyAction.Character(it)) }
        val row2 = if (!secondary) {
            listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/").map { Key(it, KeyAction.Character(it)) }
        } else {
            listOf("[", "]", "{", "}", "%", "^", "*", "+", "=", "\\").map { Key(it, KeyAction.Character(it)) }
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
                if (fa) Key("؟", KeyAction.Character("؟")) else Key("?", KeyAction.Character("?"))
            )
        } else {
            listOf(
                Key("⌫", KeyAction.Backspace, 1.35f, isSpecial = true),
                Key("_", KeyAction.Character("_")),
                Key("|", KeyAction.Character("|")),
                Key("~", KeyAction.Character("~")),
                Key("<", KeyAction.Character("<")),
                Key(">", KeyAction.Character(">")),
                Key("€", KeyAction.Character("€")),
                Key("•", KeyAction.Character("•"))
            )
        }
        val row4 = listOf(
            Key("#+=", KeyAction.ModeSymbols2, 1.0f, isSpecial = true),
            Key("⌨", KeyAction.ModeNumpad, 1.0f, isSpecial = true),
            Key(comma, KeyAction.Character(comma), 1.0f, longPressLabel = if (fa) "ZNJ" else null, longPressAction = if (fa) KeyAction.Zwnj else null),
            Key(space, KeyAction.Space, 3.6f),
            Key(".", KeyAction.Character("."), 1.0f, popupActions = if (fa) persianDotPopup() else englishDotPopup()),
            Key("↵", KeyAction.Enter, 2.0f, isSpecial = true, isAccent = true)
        )
        // Keep abc access: put on symbols2 row3 end? Better add to row4? requested layout fixed; use long-press on #+=? no.
        // Add ABC as first in row4 when in symbols? but user specified exact order. I'll replace #+= toggle behavior via ModeSymbols button? 
        // Simpler: make #+= key label show ABC when secondary? No, user asked for #+= button.
        // Use row3 last as ABC in both modes by adjusting count.
        return listOf(row1, row2, row3WithAbc(row3, abcLabel), row4)
    }

    private fun row3WithAbc(row3: List<Key>, abcLabel: String): List<Key> {
        return row3 + Key(abcLabel, KeyAction.ModeLetters, 1.1f, isSpecial = true)
    }

    private fun numpadRows(language: KeyboardLanguage): List<List<Key>> {
        val fa = language == KeyboardLanguage.FA
        val abc = if (fa) "اب‌ج" else "ABC"
        val d = listOf("1","2","3","4","5","6","7","8","9","0")
        val row1 = listOf(k(d[0]), k(d[1]), k(d[2]), Key("⌫", KeyAction.Backspace, isSpecial = true))
        val row2 = listOf(k(d[3]), k(d[4]), k(d[5]), Key(if (fa) "،" else ",", KeyAction.Character(if (fa) "،" else ",")))
        val row3 = listOf(k(d[6]), k(d[7]), k(d[8]), Key(if (fa) "فاصله" else "space", KeyAction.Space))
        val row4 = listOf(Key(abc, KeyAction.ModeLetters, isSpecial = true), k(d[9]), Key(".", KeyAction.Character(".")), Key("↵", KeyAction.Enter, isSpecial = true, isAccent = true))
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
