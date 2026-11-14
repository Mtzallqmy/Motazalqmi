package com.moataz.motazalqmi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
class ReaderUiTest {
 @get:Rule val rule = createAndroidComposeRule<MainActivity>()
 @Test fun readerNavigationOpensLocalLibraryAndSettings() {
  rule.onNodeWithText("مكتبتي").performClick()
  rule.onNodeWithText("سجل القراءة").assertIsDisplayed()
  rule.onNodeWithText("الإعدادات").performClick()
  rule.onNodeWithText("على مهلٍ… كما تحب").assertIsDisplayed()
 }
}
