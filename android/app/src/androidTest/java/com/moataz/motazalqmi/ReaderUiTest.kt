package com.moataz.motazalqmi

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.moataz.motazalqmi.ui.BlogViewModel
import java.io.File
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class ReaderUiTest {
 companion object {
  @JvmStatic @BeforeClass
  fun disableAutoSyncForDeterministicOfflineTests() {
   BlogViewModel.autoSyncEnabled = false
  }
  @JvmStatic @AfterClass
  fun restoreAutoSync() {
   BlogViewModel.autoSyncEnabled = true
  }
 }
 @get:Rule val rule = createAndroidComposeRule<MainActivity>()
 private fun capture(name: String) {
  rule.waitForIdle()
  val instrumentation = InstrumentationRegistry.getInstrumentation()
  val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
  val file = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots/$name.png")
  file.parentFile?.mkdirs()
  file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
  bitmap.recycle()
 }
 @Test fun readerNavigationOpensLocalLibraryAndSettings() {
  rule.onNodeWithText("مكتبتي").performClick()
  rule.onNodeWithText("سجل القراءة").assertIsDisplayed()
  rule.onNodeWithText("الإعدادات").performClick()
  rule.onNodeWithText("على مهلٍ… كما تحب").assertIsDisplayed()
  capture("settings")
 }
 @Test fun publishedSeedArticleCanBeOpenedAndBookmarked() {
  val seedId = "seed-1"
  val featuredTag = "featured-article-$seedId"
  val savedCardTag = "article-card-$seedId"
  // The featured card now follows the masthead and horizontal section carousel,
  // so it is not necessarily composed until the LazyColumn is scrolled.
  rule.waitUntil(20000) {
   rule.onAllNodesWithText("7 مقالات").fetchSemanticsNodes().isNotEmpty()
  }
  rule.onNodeWithTag("home-feed").performScrollToNode(hasTestTag(featuredTag))
  rule.onNodeWithTag(featuredTag).assertIsDisplayed()
  rule.onNodeWithTag(featuredTag).assertIsDisplayed().performClick()
  rule.waitUntil(10000) {
   rule.onAllNodesWithContentDescription("حفظ للقراءة لاحقًا").fetchSemanticsNodes().isNotEmpty()
  }
  rule.onNodeWithContentDescription("حفظ للقراءة لاحقًا").assertIsDisplayed().performClick()
  capture("reader")
  rule.onNodeWithContentDescription("رجوع").performClick()
  rule.onNodeWithText("مكتبتي").performClick()
  rule.waitUntil(10000) {
   rule.onAllNodesWithTag(savedCardTag).fetchSemanticsNodes().isNotEmpty()
  }
  rule.onNodeWithTag("saved-feed").performScrollToNode(hasTestTag(savedCardTag))
  rule.onNodeWithTag(savedCardTag).assertIsDisplayed()
 }
 @Test fun readerOffersNativeIndexAndAppearanceSheet() {
  rule.waitUntil(20000) { rule.onAllNodesWithText("7 مقالات").fetchSemanticsNodes().isNotEmpty() }
  rule.onNodeWithTag("home-feed").performScrollToNode(hasTestTag("featured-article-seed-1"))
  rule.onNodeWithTag("featured-article-seed-1").performClick()
  rule.onNodeWithContentDescription("إعدادات القراءة").performClick()
  rule.onNodeWithText("مساحتك للقراءة").assertIsDisplayed()
  rule.onNodeWithText("نهاري").performClick()
  rule.onNodeWithText("تم").performClick()
  rule.onNodeWithContentDescription("فهرس المقالة").performClick()
  rule.onNodeWithText("فهرس المقالة").assertIsDisplayed()
  rule.onNodeWithText("إغلاق").performClick()
  capture("reader-tools")
 }

}
