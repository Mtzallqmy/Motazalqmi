package com.moataz.motazalqmi.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class WriterMedia(val url:String="",val pathname:String="",val size:Long=0)
data class WriterPendingMedia(val url:String="")
data class WriterMediaLibrary(val items:List<WriterMedia> = emptyList(),val cursor:String?=null,val pendingDeletions:List<WriterPendingMedia> = emptyList())
data class MediaDeleteRequest(val url:String,val confirm:Boolean=true)
data class AuthorChange(val id:String?=null,val name:String,val bio:String="",val image:String="",val link:String="")
data class WriterAuthorResponse(val author:WriterProfile)

@Composable internal fun NativeMediaLibrary(state:WriterUiState,load:(String?)->Unit,remove:(String)->Unit) {
 var query by rememberSaveable { mutableStateOf("") }
 var deletion by rememberSaveable { mutableStateOf<String?>(null) }
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text("مكتبة الوسائط",style=MaterialTheme.typography.headlineSmall)
  OutlinedTextField(query,{query=it},label={Text("بحث في الملفات المعروضة")},modifier=Modifier.fillMaxWidth())
  OutlinedButton(onClick={load(null)},enabled=!state.busy){Text("تحديث المكتبة")}
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   items(state.media.filter { it.pathname.contains(query,true) },key={it.url}) { file ->
    OutlinedCard(Modifier.fillMaxWidth()) {
     Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text(file.pathname,style=MaterialTheme.typography.titleSmall)
      Text(((file.size+1023)/1024).toString()+" KB",style=MaterialTheme.typography.bodySmall)
      if(Regex("\\.(jpg|jpeg|png|webp|gif)$",RegexOption.IGNORE_CASE).containsMatchIn(file.url))
       coil.compose.AsyncImage(model=file.url,contentDescription="معاينة الملف",modifier=Modifier.fillMaxWidth().heightIn(max=160.dp))
      TextButton(onClick={deletion=file.url},enabled=!state.busy){Text("حذف من المخزن")}
     }
    }
   }
   items(state.pendingMedia,key={it.url}) { pending ->
    OutlinedButton(onClick={deletion=pending.url},enabled=!state.busy){Text("إكمال حذف "+pending.url.substringAfterLast('/'))}
   }
   if(state.media.isEmpty()&&!state.busy)item { Text("لا توجد ملفات معروضة. حدّث المكتبة أو ارفع ملفات من محرر المقالة.") }
   if(state.mediaCursor!=null)item { OutlinedButton(onClick={load(state.mediaCursor)},enabled=!state.busy){Text("المزيد من الملفات")} }
  }
 }
 deletion?.let { url ->
  AlertDialog(onDismissRequest={deletion=null},title={Text("حذف الملف من المخزن؟")},
   text={Text("يتحقق الخادم من المقالات والمسودات وملفات الكتّاب والنسخ السابقة، ويمنع حذف الملف المستخدم.")},
   confirmButton={TextButton(onClick={deletion=null;remove(url)}){Text("تأكيد الحذف")}},
   dismissButton={TextButton(onClick={deletion=null}){Text("إلغاء")}})
 }
}

@Composable internal fun NativeAuthorManager(state:WriterUiState,save:(AuthorChange,(WriterProfile)->Unit)->Unit,upload:(Uri,String,(String)->Unit)->Unit) {
 var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
 var name by rememberSaveable { mutableStateOf("") }
 var bio by rememberSaveable { mutableStateOf("") }
 var image by rememberSaveable { mutableStateOf("") }
 var link by rememberSaveable { mutableStateOf("") }
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
  if(uri!=null)upload(uri,"author"){image=it}
 }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text("ملفات الكتّاب",style=MaterialTheme.typography.headlineSmall)
  OutlinedButton(onClick={selectedId=null;name="";bio="";image="";link=""},enabled=!state.busy){Text("كاتب جديد")}
  state.profiles.forEach { profile ->
   OutlinedCard(onClick={selectedId=profile.id;name=profile.name;bio=profile.bio;image=profile.image;link=profile.link},modifier=Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) { AuthorIdentity(profile.name,profile.image,"تحرير بيانات الكاتب") }
   }
  }
  Text(if(selectedId==null)"إضافة ملف كاتب" else "تعديل ملف الكاتب",style=MaterialTheme.typography.titleLarge)
  OutlinedTextField(name,{name=it.take(150)},label={Text("الاسم الكامل")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(bio,{bio=it.take(3000)},label={Text("نبذة الكاتب")},minLines=3,modifier=Modifier.fillMaxWidth())
  OutlinedTextField(image,{image=it.take(2048)},label={Text("رابط صورة الكاتب")},modifier=Modifier.fillMaxWidth())
  OutlinedButton(onClick={picker.launch("image/*")},enabled=!state.busy){Text("رفع صورة من الجهاز")}
  OutlinedTextField(link,{link=it.take(2048)},label={Text("الموقع الشخصي HTTPS")},modifier=Modifier.fillMaxWidth())
  Button(onClick={save(AuthorChange(selectedId,name.trim(),bio.trim(),image.trim(),link.trim())){profile->
   selectedId=profile.id;name=profile.name;bio=profile.bio;image=profile.image;link=profile.link
  }},enabled=!state.busy&&name.isNotBlank()&&(image.isBlank()||image.startsWith("https://"))&&(link.isBlank()||link.startsWith("https://"))) { Text("حفظ ملف الكاتب") }
  Spacer(Modifier.height(20.dp))
 }
}
