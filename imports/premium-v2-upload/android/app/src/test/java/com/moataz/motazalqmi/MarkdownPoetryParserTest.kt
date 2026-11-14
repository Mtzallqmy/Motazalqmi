package com.moataz.motazalqmi

import com.moataz.motazalqmi.domain.text.ContentBlock
import com.moataz.motazalqmi.domain.text.MarkdownPoetryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownPoetryParserTest {
    @Test
    fun parsesPoetryHemistichsPreservingTashkeel() {
        val md = """
            # قصيدة مختارة
            عَلَى قَدْرِ أَهْلِ العَزْمِ تَأْتِي العَزَائِمُ *** وَتَأْتِي عَلَى قَدْرِ الكِرَامِ المَكَارِمُ
        """.trimIndent()

        val blocks = MarkdownPoetryParser.parse(md)
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Heading)
        val stanza = blocks[1] as ContentBlock.PoetryStanza
        assertEquals("عَلَى قَدْرِ أَهْلِ العَزْمِ تَأْتِي العَزَائِمُ", stanza.verses.first().sadr)
        assertEquals("وَتَأْتِي عَلَى قَدْرِ الكِرَامِ المَكَارِمُ", stanza.verses.first().ajuz)
    }
}
