package com.moataz.motazalqmi
import com.moataz.motazalqmi.domain.ArabicSearch
import org.junit.Assert.*
import org.junit.Test
class ArabicSearchTest {
 @Test fun matchesDiacriticsAndAlefVariants() {
  assertTrue(ArabicSearch.matches("ادب عربي", "أَدَبٌ عَرَبِيّ"))
  assertTrue(ArabicSearch.matches("الي", "إلى"))
  assertFalse(ArabicSearch.matches("تاريخ", "أَدَبٌ عَرَبِيّ"))
 }
 @Test fun matchesEveryTermAcrossAuthorAndTags() {
  assertTrue(ArabicSearch.matches("معتز الشعر", "معتز العلقمي", "الشِّعْر العربي"))
  assertFalse(ArabicSearch.matches("معتز الفلك", "معتز العلقمي", "الشعر"))
 }
}
