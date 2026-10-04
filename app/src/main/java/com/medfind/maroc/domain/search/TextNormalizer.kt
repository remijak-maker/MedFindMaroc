package com.medfind.maroc.domain.search

import java.text.Normalizer
import java.util.Locale

object TextNormalizer {
    private val diacritics = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val nonAlphaNum = "[^a-z0-9]+".toRegex()

    /** Minuscules, sans accents, ponctuation remplacée par des espaces. */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed
            .replace(diacritics, "")
            .replace("œ", "oe")
            .replace("æ", "ae")
            .replace(nonAlphaNum, " ")
            .trim()
    }
}
