package com.moataz.motazalqmi

import com.moataz.motazalqmi.ui.readerParts
import com.moataz.motazalqmi.ui.partHeading
import org.junit.Assert.*
import org.junit.Test

class ReaderNavigationTest {
 @Test fun headingsInsideCodeAreExcludedAndMediaOrderIsPreserved() {
  val markdown = "# بداية\nنص\n```kotlin\n## code\n```\n![صوت](https://example.org/voice.mp3)\n## نهاية\nفقرة".replace("\\n","\n")
  val parts = readerParts(markdown)
  assertEquals(listOf("بداية","نهاية"),parts.mapNotNull(::partHeading))
  val audio = parts.indexOfFirst { it.audioUrl != null }
  assertTrue(audio > 0)
  assertTrue(parts.indexOfFirst { partHeading(it)=="نهاية" } > audio)
 }
}
