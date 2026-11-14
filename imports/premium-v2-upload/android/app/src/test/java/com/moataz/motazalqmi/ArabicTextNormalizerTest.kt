package com.moataz.motazalqmi

import com.moataz.motazalqmi.domain.text.ArabicTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicTextNormalizerTest {
    @Test
    fun normalizesTashkeelAndAlefVariants() {
        val input = "أَدَبُنَا الْعَرَبِيُّ — إِبْدَاعٌ وَآفَاقٌ"
        val expected = "ادبنا العربي — ابداع وافاق"
        assertEquals(expected, ArabicTextNormalizer.normalizeForSearch(input))
    }
}
