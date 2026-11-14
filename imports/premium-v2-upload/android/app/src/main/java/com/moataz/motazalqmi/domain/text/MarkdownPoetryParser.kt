package com.moataz.motazalqmi.domain.text

sealed interface ContentBlock {
    data class Heading(val level: Int, val text: String) : ContentBlock
    data class Paragraph(val text: String) : ContentBlock
    data class Blockquote(val text: String) : ContentBlock
    data class BulletList(val items: List<String>) : ContentBlock
    data class PoetryStanza(val verses: List<PoetryVerse>) : ContentBlock
    data object Divider : ContentBlock
}

data class PoetryVerse(
    val sadr: String,
    val ajuz: String
)

object MarkdownPoetryParser {
    private val poetrySplitters = listOf(" *** ", " :: ", " ... ", " | ", " — ")

    fun parse(markdown: String): List<ContentBlock> {
        val lines = markdown.replace("\r\n", "\n").lines()
        val blocks = mutableListOf<ContentBlock>()
        var i = 0

        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            if (trimmed.isEmpty()) {
                i++
                continue
            }

            if (trimmed == "---" || trimmed == "***") {
                blocks += ContentBlock.Divider
                i++
                continue
            }

            // Explicit poetry fenced block ```poetry or ```شعر
            if (trimmed.startsWith("```poetry") || trimmed.startsWith("```شعر")) {
                i++
                val verses = mutableListOf<PoetryVerse>()
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    val vLine = lines[i].trim()
                    if (vLine.isNotEmpty()) {
                        verses += parseVerseLine(vLine)
                    }
                    i++
                }
                if (verses.isNotEmpty()) {
                    blocks += ContentBlock.PoetryStanza(verses)
                }
                i++
                continue
            }

            // Headings
            if (trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                val text = trimmed.drop(level).trim()
                blocks += ContentBlock.Heading(level, text)
                i++
                continue
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines += lines[i].trim().removePrefix(">").trim()
                    i++
                }
                blocks += ContentBlock.Blockquote(quoteLines.joinToString("\n"))
                continue
            }

            // Bullet list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val cur = lines[i].trim()
                    if (cur.startsWith("- ") || cur.startsWith("* ") || cur.startsWith("• ")) {
                        items += cur.drop(2).trim()
                        i++
                    } else break
                }
                blocks += ContentBlock.BulletList(items)
                continue
            }

            // Inline poetry line detection (preserving tashkeel)
            if (poetrySplitters.any { trimmed.contains(it) } && trimmed.length <= 160) {
                val verses = mutableListOf<PoetryVerse>()
                while (i < lines.size) {
                    val cur = lines[i].trim()
                    if (cur.isNotEmpty() && poetrySplitters.any { cur.contains(it) }) {
                        verses += parseVerseLine(cur)
                        i++
                    } else break
                }
                blocks += ContentBlock.PoetryStanza(verses)
                continue
            }

            // Standard paragraph
            val paraLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().isNotEmpty() &&
                !lines[i].trim().startsWith("#") &&
                !lines[i].trim().startsWith(">") &&
                !lines[i].trim().startsWith("- ") &&
                !lines[i].trim().startsWith("```")
            ) {
                paraLines += lines[i].trim()
                i++
            }
            blocks += ContentBlock.Paragraph(paraLines.joinToString(" "))
        }
        return blocks
    }

    private fun parseVerseLine(line: String): PoetryVerse {
        for (splitter in poetrySplitters) {
            if (line.contains(splitter)) {
                val parts = line.split(splitter, limit = 2)
                return PoetryVerse(parts[0].trim(), parts.getOrElse(1) { "" }.trim())
            }
        }
        return PoetryVerse(line.trim(), "")
    }
}
