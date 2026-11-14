package com.moataz.motazalqmi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test
class ReaderUiTest {
 @get:Rule val rule = createAndroidComposeRule<MainActivity>()
 private fun capture(name: String) {
  rule.waitForIdle()
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  val bitmap=instrumentation.uiAutomation.takeScreenshot() ?: return
  val file=File(instrumentation.targetContext.getExternalFilesDir(null),"screenshots/$name.png")
  file.parentFile?.mkdirs();file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
 }
 @Test fun readerNavigationOpensLocalLibraryAndSettings() {
  rule.onNodeWithText("مكتبتي").performClick()
  rule.onNodeWithText("سجل القراءة").assertIsDisplayed()
  rule.onNodeWithText("الإعدادات").performClick()
  rule.onNodeWithText("على مهلٍ… كما تحب").assertIsDisplayed()
  capture("settings")
 }
 @Test fun publishedSeedArticleCanBeOpenedAndBookmarked() {
  val title="سحر البيان في معلقة امرئ القيس: قراءة في هندسة الصورة الشعرية"
  rule.waitUntil(15000) { rule.onAllNodesWithText("في دائرة الضوء").fetchSemanticsNodes().isNotEmpty() }
  rule.onNodeWithTag("home-feed").performScrollToNode(hasText(title))
  rule.onAllNodesWithText(title).onFirst().performClick()
  rule.onNodeWithContentDescription("حفظ للقراءة لاحقًا").assertIsDisplayed().performClick()
  capture("reader")
  rule.onNodeWithContentDescription("رجوع").performClick()
  rule.onNodeWithText("مكتبتي").performClick()
  rule.onNodeWithTag("saved-feed").performScrollToNode(hasText(title))
  rule.onNodeWithText(title).assertIsDisplayed()
 }
}
