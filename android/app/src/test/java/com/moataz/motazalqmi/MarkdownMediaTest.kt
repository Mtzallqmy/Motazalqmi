package com.moataz.motazalqmi
import com.moataz.motazalqmi.ui.splitMarkdownMedia
import org.junit.Assert.*
import org.junit.Test
class MarkdownMediaTest {
 @Test fun embeddedImageIsPreservedAtItsOriginalPosition() {
  val parts=splitMarkdownMedia("المقدمة\n![صورة](https://example.org/image.png)\nالخاتمة")
  assertEquals(3,parts.size)
  assertTrue(parts[0].text!!.contains("المقدمة"))
  assertEquals("https://example.org/image.png",parts[1].imageUrl)
  assertTrue(parts[2].text!!.contains("الخاتمة"))
 }
 @Test fun emptyArticleAndNonHttpsAreNotConvertedToRemoteImages() {
  assertEquals("",splitMarkdownMedia("").single().text)
  val parts=splitMarkdownMedia("![غير آمن](http://example.org/a.png)")
  assertEquals(1,parts.size)
  assertNull(parts[0].imageUrl)
 }
}
