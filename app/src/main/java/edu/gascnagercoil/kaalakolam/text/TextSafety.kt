package edu.gascnagercoil.kaalakolam.text

import java.text.BreakIterator
import java.text.Collator
import java.text.Normalizer
import java.util.Locale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object TextSafety {
    fun graphemePrefix(text: String, maxGraphemes: Int, locale: Locale): String {
        require(maxGraphemes >= 0)
        if (text.isEmpty() || maxGraphemes == 0) return ""
        val iterator = BreakIterator.getCharacterInstance(locale)
        iterator.setText(text)
        var boundary = iterator.first()
        var count = 0
        while (count < maxGraphemes) {
            val next = iterator.next()
            if (next == BreakIterator.DONE) return text
            boundary = next
            count++
        }
        return text.subSequence(0, boundary).toString()
    }

    fun graphemeCount(text: String, locale: Locale): Int {
        val iterator = BreakIterator.getCharacterInstance(locale)
        iterator.setText(text)
        var count = 0
        var boundary = iterator.first()
        while (boundary != BreakIterator.DONE) {
            val next = iterator.next()
            if (next == BreakIterator.DONE) break
            count++
            boundary = next
        }
        return count
    }

    fun limitTextFieldValue(value: TextFieldValue, maxGraphemes: Int, locale: Locale): TextFieldValue {
        if (graphemeCount(value.text, locale) <= maxGraphemes) return value
        val limited = graphemePrefix(value.text, maxGraphemes, locale)
        val end = limited.length
        val selection = TextRange(
            value.selection.start.coerceIn(0, end),
            value.selection.end.coerceIn(0, end),
        )
        val composition = value.composition?.let {
            TextRange(it.start.coerceIn(0, end), it.end.coerceIn(0, end))
        }?.takeIf { it.start < it.end }
        return value.copy(text = limited, selection = selection, composition = composition)
    }

    fun sortTamil(values: Iterable<String>): List<String> {
        val collator = Collator.getInstance(Locale("ta", "IN"))
        return values.sortedWith { left, right -> collator.compare(left, right) }
    }

    fun normalisedContains(haystack: String, needle: String): Boolean {
        val normalisedHaystack = Normalizer.normalize(haystack, Normalizer.Form.NFC)
        val normalisedNeedle = Normalizer.normalize(needle, Normalizer.Form.NFC)
        return normalisedHaystack.contains(normalisedNeedle, ignoreCase = true)
    }
}
