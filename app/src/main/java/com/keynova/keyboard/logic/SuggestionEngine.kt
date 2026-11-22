package com.keynova.keyboard.logic

import android.content.Context
import com.keynova.keyboard.model.KeyboardLanguage
import java.util.Locale

class SuggestionEngine(context: Context) {
    private val faWords: List<String> by lazy { loadWords(context, "fa_words.txt") }
    private val enWords: List<String> by lazy { loadWords(context, "en_words.txt") }
    private val recent = ArrayDeque<String>()
    private val bigrams = mutableMapOf<String, MutableList<String>>() // prev -> next candidates

    private val faNext = mapOf(
        "سلام" to listOf("خوبی","عزیزم","دوست"), "خوبی" to listOf("؟","ممنون","خوبم"),
        "من" to listOf("هم","می‌خوام","دارم"), "به" to listOf("تو","من","نظر"),
        "در" to listOf("حال","این","واقع"), "با" to listOf("تو","هم","این"),
        "ممنون" to listOf("از","ممنونم","عزیزم"), "خداحافظ" to listOf("دوست","عزیزم","فعلا"),
        "چه" to listOf("خبری","کاری","زمانی"), "این" to listOf("رو","کار","یه"),
    )
    private val enNext = mapOf(
        "i" to listOf("am","think","want"), "you" to listOf("are","can","know"),
        "to" to listOf("be","do","go"), "the" to listOf("best","first","same"),
        "thank" to listOf("you","so","god"), "how" to listOf("are","is","do"),
        "good" to listOf("morning","night","job"), "see" to listOf("you","it","that"),
    )

    fun learn(text: String) {
        val tokens = tokenize(text)
        tokens.forEach { w ->
            if (w.length < 2) return@forEach
            recent.remove(w)
            recent.addFirst(w)
        }
        while (recent.size > 80) recent.removeLast()
        for (i in 0 until tokens.size - 1) {
            val prev = normalize(tokens[i]); val next = tokens[i + 1]
            if (prev.length < 2 || next.length < 2) continue
            val list = bigrams.getOrPut(prev) { mutableListOf() }
            list.remove(next); list.add(0, next)
            if (list.size > 8) list.removeAt(list.lastIndex)
        }
    }

    fun suggest(currentWord: String, language: KeyboardLanguage, afterSpace: Boolean): List<String> {
        val q = currentWord.trim()
        val dict = if (language == KeyboardLanguage.FA) faWords else enWords
        val locale = Locale.forLanguageTag(if (language == KeyboardLanguage.FA) "fa" else "en")

        if (q.isEmpty()) {
            if (!afterSpace) return emptyList()
            return nextWordSuggestions(language).take(3)
        }

        val lowerQ = q.lowercase(locale)
        val scored = linkedMapOf<String, Int>() // preserve order by score insertion

        fun add(word: String, score: Int) {
            if (word.equals(q, true)) return
            if (!matchesLang(word, language)) return
            scored[word] = maxOf(scored[word] ?: Int.MIN_VALUE, score)
        }

        // recent/personalized
        recent.forEachIndexed { idx, w ->
            val lw = w.lowercase(locale)
            if (lw.startsWith(lowerQ)) add(w, 120 - idx)
            else if (lowerQ.length >= 2 && lw.contains(lowerQ)) add(w, 70 - idx)
        }
        // dictionary
        dict.forEach { w ->
            val lw = w.lowercase(locale)
            if (lw.startsWith(lowerQ)) add(w, 100 - (w.length - q.length).coerceAtLeast(0))
            else if (lowerQ.length >= 3 && lw.contains(lowerQ)) add(w, 50)
            else if (lowerQ.length >= 3 && editDistance(lw, lowerQ) <= 1) add(w, 60)
        }
        // next-word context when user typed exact short word? no - currentWord is non-empty

        return scored.entries.sortedByDescending { it.value }.map { it.key }.distinct().take(3)
    }

    fun suggestNext(language: KeyboardLanguage, prevWord: String): List<String> {
        return nextWordSuggestions(language, prevWord).take(3)
    }

    private fun nextWordSuggestions(language: KeyboardLanguage, prevWord: String? = null): List<String> {
        val out = linkedSetOf<String>()
        val prev = prevWord?.let { normalize(it) }
        if (prev != null) {
            bigrams[prev]?.forEach { if (matchesLang(it, language)) out.add(it) }
            val common = (if (language == KeyboardLanguage.FA) faNext else enNext)[prev].orEmpty()
            common.forEach { out.add(it) }
        }
        recent.forEach { if (matchesLang(it, language)) out.add(it) }
        val base = if (language == KeyboardLanguage.FA) listOf("من","این","که","با","به","از","سلام","ممنون") else listOf("I","the","to","you","and","is","thanks","ok")
        base.forEach { out.add(it) }
        return out.toList()
    }

    private fun tokenize(text: String): List<String> = Regex("[\\p{L}\\u200C']+").findAll(text).map { it.value }.toList()
    private fun normalize(w: String): String = w.lowercase(Locale.ROOT)

    private fun matchesLang(word: String, lang: KeyboardLanguage): Boolean {
        val fa = word.any { it in '\u0600'..'\u06FF' }
        return if (lang == KeyboardLanguage.FA) fa else !fa
    }

    private fun loadWords(context: Context, name: String): List<String> = try {
        context.assets.open(name).bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
    } catch (_: Exception) { emptyList() }

    private fun editDistance(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 1) return 2
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
        }
        return dp[a.length][b.length]
    }
}