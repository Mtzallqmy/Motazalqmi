package com.moataz.motazalqmi

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.moataz.motazalqmi.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StudioUiTest {
 @get:Rule val rule = createComposeRule()
 @Test fun mediaDeletionRequiresConfirmationAndCancelKeepsTheFile() {
  var removed:String?=null
  val url="https://store.public.blob.vercel-storage.com/example.pdf"
  rule.setContent { MaterialTheme { NativeMediaLibrary(WriterUiState(restoring=false,media=listOf(WriterMedia(url,"example.pdf",1024))),{}, {removed=it}) } }
  rule.onNodeWithText("حذف من المخزن").performClick()
  assertNull(removed)
  rule.onNodeWithText("إلغاء").performClick()
  assertNull(removed)
  rule.onNodeWithText("example.pdf").assertIsDisplayed()
  rule.onNodeWithText("حذف من المخزن").performClick()
  rule.onNodeWithText("تأكيد الحذف").performClick()
  rule.runOnIdle { assertEquals(url,removed) }
 }
 @Test fun editingAnExistingAuthorRetainsTheirIdentity() {
  var changed:AuthorChange?=null
  rule.setContent { MaterialTheme { NativeAuthorManager(WriterUiState(restoring=false,profiles=listOf(WriterProfile(id="writer-1",name="كاتب مختلف",bio="نبذة محفوظة"))),{value,done->changed=value;done(WriterProfile(id=value.id?:"new",name=value.name,bio=value.bio))},{_,_,_->}) } }
  rule.onNodeWithText("كاتب مختلف").performClick()
  rule.onNodeWithText("تعديل ملف الكاتب").assertIsDisplayed()
  rule.onNodeWithText("الاسم الكامل").performScrollTo().performTextReplacement("الكاتب بعد التعديل")
  rule.onNodeWithText("حفظ ملف الكاتب").performScrollTo().performClick()
  rule.runOnIdle { assertEquals("writer-1",changed?.id);assertEquals("الكاتب بعد التعديل",changed?.name);assertEquals("نبذة محفوظة",changed?.bio) }
 }
}
