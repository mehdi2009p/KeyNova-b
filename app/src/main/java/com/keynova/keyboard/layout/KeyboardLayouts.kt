package com.keynova.keyboard.layout

import com.keynova.keyboard.model.Key
import com.keynova.keyboard.model.KeyAction
import com.keynova.keyboard.model.KeyboardMode
import com.keynova.keyboard.model.ShiftState

object KeyboardLayouts {

    fun rowsFor(mode: KeyboardMode, shift: ShiftState): List<List<Key>> {
        return when (mode) {
            KeyboardMode.LETTERS -> letterRows(shift)
            KeyboardMode.SYMBOLS -> symbolRows()
        }
    }

    private fun letterRows(shift: ShiftState): List<List<Key>> {
        val upper = shift == ShiftState.ON
        fun charKey(c: Char): Key {
            val out = if (upper) c.uppercaseChar().toString() else c.lowercaseChar().toString()
            return Key(
                label = out,
                action = KeyAction.Character(out),
                weight = 1f
            )
        }

        val row1 = "qwertyuiop".map { charKey(it) }
        val row2 = "asdfghjkl".map { charKey(it) }
        val row3 = buildList {
            add(
                Key(
                    label = if (upper) "⇧" else "⇧",
                    action = KeyAction.Shift,
                    weight = 1.5f,
                    isSpecial = true,
                    isAccent = upper
                )
            )
            "zxcvbnm".forEach { add(charKey(it)) }
            add(
                Key(
                    label = "⌫",
                    action = KeyAction.Backspace,
                    weight = 1.5f,
                    isSpecial = true
                )
            )
        }
        val row4 = listOf(
            Key(label = "?123", action = KeyAction.ModeSymbols, weight = 1.5f, isSpecial = true),
            Key(label = ",", action = KeyAction.Character(","), weight = 1f),
            Key(label = "space", action = KeyAction.Space, weight = 4f),
            Key(label = ".", action = KeyAction.Character("."), weight = 1f),
            Key(label = "↵", action = KeyAction.Enter, weight = 1.5f, isSpecial = true, isAccent = true)
        )
        return listOf(row1, row2, row3, row4)
    }

    private fun symbolRows(): List<List<Key>> {
        val row1 = "1234567890".map {
            val s = it.toString()
            Key(label = s, action = KeyAction.Character(s))
        }
        val row2 = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")").map {
            Key(label = it, action = KeyAction.Character(it))
        }
        val row3 = buildList {
            add(Key(label = "=\\<", action = KeyAction.ModeLetters, weight = 1.5f, isSpecial = true))
            listOf("*", "\"", "'", ":", ";", "!", "?").forEach {
                add(Key(label = it, action = KeyAction.Character(it)))
            }
            add(Key(label = "⌫", action = KeyAction.Backspace, weight = 1.5f, isSpecial = true))
        }
        // row3 above is a bit crowded in label for mode switch; better make mode switch clear
        val fixedRow3 = listOf(
            Key(label = "/", action = KeyAction.Character("/")),
            Key(label = "*", action = KeyAction.Character("*")),
            Key(label = "\"", action = KeyAction.Character("\"")),
            Key(label = "'", action = KeyAction.Character("'")),
            Key(label = ":", action = KeyAction.Character(":")),
            Key(label = ";", action = KeyAction.Character(";")),
            Key(label = "!", action = KeyAction.Character("!")),
            Key(label = "?", action = KeyAction.Character("?")),
            Key(label = "⌫", action = KeyAction.Backspace, weight = 1.4f, isSpecial = true)
        )
        val row4 = listOf(
            Key(label = "ABC", action = KeyAction.ModeLetters, weight = 1.5f, isSpecial = true),
            Key(label = ",", action = KeyAction.Character(","), weight = 1f),
            Key(label = "space", action = KeyAction.Space, weight = 4f),
            Key(label = ".", action = KeyAction.Character("."), weight = 1f),
            Key(label = "↵", action = KeyAction.Enter, weight = 1.5f, isSpecial = true, isAccent = true)
        )
        return listOf(row1, row2, fixedRow3, row4)
    }
}
