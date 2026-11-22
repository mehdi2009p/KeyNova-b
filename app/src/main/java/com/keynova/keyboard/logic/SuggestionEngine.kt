package com.keynova.keyboard.logic

import android.content.Context
import com.keynova.keyboard.model.KeyboardLanguage
import java.util.Locale

class SuggestionEngine(context: Context) {
    private val faWords: List<String> by lazy { loadWords(context, "fa_words.txt") }
    private val enWords: List<String> by lazy { loadWords(context, "en_words.txt") }
    private val recent = ArrayDeque<String>()

    fun learn(text: String) {
        val w = text.trim()
        if (w.length < 2) return
        recent.remove(w)
        recent.addFirst(w)
        while (recent.size > 50) recent.removeLast()
    }

    fun suggest(currentWord: String, language: KeyboardLanguage, afterSpace: Boolean): List<String> {
        val q = currentWord.trim()
        val dict = if (language == KeyboardLanguage.FA) faWords else enWords
        if (q.isEmpty()) {
            if (!afterSpace) return emptyList()
            val base = if (language == KeyboardLanguage.FA) listOf("من","این","که","با","به","از") else listOf("I","the","to","you","and","is")
            return (recent.filter { matchesLang(it, language) } + base).distinct().take(3)
        }
        val lowerQ = q.lowercase(Locale.forLanguageTag(if (language == KeyboardLanguage.FA) "fa" else "en"))
        val recentHits = recent.filter { matchesLang(it, language) && it.lowercase(Locale.forLanguageTag(if (language == KeyboardLanguage.FA) "fa" else "en")).startsWith(lowerQ) && !it.equals(q, true) }
        val dictHits = dict.asSequence()
            .filter { it.startsWith(lowerQ) && !it.equals(q, true) }
            .take(8)
            .toList()
        return (recentHits + dictHits).distinct().take(3)
    }

    private fun matchesLang(word: String, lang: KeyboardLanguage): Boolean {
        val fa = word.any { it in '\u0600'..'\u06FF' }
        return if (lang == KeyboardLanguage.FA) fa else !fa
    }

    private fun loadWords(context: Context, name: String): List<String> = try {
        context.assets.open(name).bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
    } catch (_: Exception) { emptyList() }
}