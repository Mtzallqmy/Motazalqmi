package com.moataz.motazalqmi.domain.text

object ArabicTextNormalizer {
    private val tashkeelRegex = Regex("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]")

    fun normalizeForSearch(input: String): String {
        if (input.isBlank()) return ""
        return input
            .replace(tashkeelRegex, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .trim()
            .lowercase()
    }
}
