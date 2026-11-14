package com.moataz.motazalqmi.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.clip
import android.widget.TextView
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moataz.motazalqmi.data.ArticleDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Inject

data class WriterLogin(val email:String,val password:String,val remember:Boolean=true)
data class WriterRefresh(val grantType:String="refresh",val refreshToken:String)
data class WriterAccess(val accessToken:String="",val expiresAt:Long=0L,val refreshToken:String?=null,val refreshExpiresAt:Long=0L)
data class WriterList(val articles:List<ArticleDto> = emptyList())
data class MediaDirectRequest(val mime:String,val base64:String,val purpose:String)
data class MediaDirectResponse(val url:String="")
data class WriterAction(val action:String,val article:ArticleDto?=null,val id:String?=null)
interface WriterApi {
 @POST("api/v1/mobile-admin") suspend fun login(@Body value:WriterLogin):WriterAccess
 @POST("api/v1/mobile-admin") suspend fun refresh(@Body value:WriterRefresh):WriterAccess
 @GET("api/articles") suspend fun articles(@Query("scope") scope:String,@Header("Authorization") bearer:String):WriterList
 @POST("api/articles") suspend fun change(@Header("Authorization") bearer:String,@Body change:WriterAction)
 @POST("api/v1/media-direct") suspend fun uploadMedia(@Header("Authorization") bearer:String,@Body value:MediaDirectRequest):MediaDirectResponse
}

data class WriterUiState(val authorized:Boolean=false,val busy:Boolean=false,val restoring:Boolean=true,val error:String="",
 val articles:List<ArticleDto> = emptyList(),val editing:ArticleDto?=null,val editorOpen:Boolean=false)

@HiltViewModel class WriterViewModel @Inject constructor(
 private val api:WriterApi,private val secureSession:SecureWriterSession,
 @ApplicationContext private val context:Context
):ViewModel(){
 val ui=MutableStateFlow(WriterUiState())
 private var token:String?=null
 private var expiresAt:Long=0L
 init {
  viewModelScope.launch {
   val saved=secureSession.load()
   if(saved.isNullOrBlank()){
    ui.value=ui.value.copy(restoring=false)
   }else {
    try {
     val result=api.refresh(WriterRefresh(refreshToken=saved))
     require(result.accessToken.isNotBlank() && result.expiresAt>System.currentTimeMillis())
     token=result.accessToken;expiresAt=result.expiresAt
     ui.value=ui.value.copy(authorized=true,restoring=false)
     reload()
    }catch (_:Exception){
     secureSession.clear()
     ui.value=ui.value.copy(restoring=false,error="انتهت الجلسة المحفوظة. سجل الدخول مجددًا.")
    }
   }
  }
 }
 fun login(email:String,password:String,remember:Boolean=true){
  if(ui.value.busy)return
  viewModelScope.launch{
   ui.value=ui.value.copy(busy=true,error="")
   try {
    val session=api.login(WriterLogin(email.trim(),password,remember))
    require(session.accessToken.isNotBlank()&&session.expiresAt>System.currentTimeMillis())
    token=session.accessToken;expiresAt=session.expiresAt
    if(remember && !session.refreshToken.isNullOrBlank() && session.refreshExpiresAt>System.currentTimeMillis())
     secureSession.save(session.refreshToken,session.refreshExpiresAt)
    else secureSession.clear()
    ui.value=ui.value.copy(authorized=true,busy=false)
    reload()
   }catch (_:Exception){ui.value=ui.value.copy(busy=false,error="تعذر تسجيل الدخول. تحقق من البيانات والاتصال.") }
  }
 }
 private suspend fun credential():String {
  if(token.isNullOrBlank() || System.currentTimeMillis()>=expiresAt-60_000L){
   val saved=secureSession.load()?:throw IllegalStateException("انتهت جلسة الكاتب")
   val session=api.refresh(WriterRefresh(refreshToken=saved))
   require(session.accessToken.isNotBlank()&&session.expiresAt>System.currentTimeMillis())
   token=session.accessToken;expiresAt=session.expiresAt
  }
  return "Bearer "+token
 }
 fun reload(){
  viewModelScope.launch{
   ui.value=ui.value.copy(busy=true,error="")
   try{
    val articles=api.articles("admin",credential()).articles
    ui.value=ui.value.copy(articles=articles,busy=false,authorized=true)
   }catch (_:Exception){ui.value=ui.value.copy(busy=false,error="تعذر تحميل المقالات؛ تحقق من الاتصال أو أعد تسجيل الدخول.")}
  }
 }
 fun uploadMedia(uri:Uri,purpose:String,onDone:(String)->Unit){
  if(ui.value.busy)return
  viewModelScope.launch {
   ui.value=ui.value.copy(busy=true,error="")
   try{
    val mime=context.contentResolver.getType(uri) ?: throw IllegalArgumentException("نوع الملف غير معروف")
    val bytes=withContext(Dispatchers.IO){
     context.contentResolver.openInputStream(uri)?.use { stream->
      val out=ByteArrayOutputStream()
      val buffer=ByteArray(32768)
      while(true){
       val size=stream.read(buffer)
       if(size<0)break
       if(out.size()+size>1800000)throw IllegalArgumentException("الملف أكبر من 1.8 ميجابايت")
       out.write(buffer,0,size)
      }
      out.toByteArray()
     } ?: throw IllegalArgumentException("تعذر قراءة الملف")
    }
    val response=api.uploadMedia(credential(),MediaDirectRequest(mime,Base64.encodeToString(bytes,Base64.NO_WRAP),purpose))
    check(response.url.startsWith("https://"))
    onDone(response.url)
    ui.value=ui.value.copy(busy=false,error="")
   }catch(e:Exception){
    ui.value=ui.value.copy(busy=false,error=e.message?.take(150)?:"تعذر رفع الملف. الملفات الكبيرة تُرفع من لوحة الموقع.")
   }
  }
 }
 fun edit(value:ArticleDto?){ui.value=ui.value.copy(editing=value,editorOpen=true,error="")}
 fun closeEditor(){ui.value=ui.value.copy(editorOpen=false,error="")}
 fun save(value:ArticleDto){
  viewModelScope.launch{
   ui.value=ui.value.copy(busy=true,error="")
   try{
    api.change(credential(),WriterAction("save",article=value))
    ui.value=ui.value.copy(editorOpen=false,busy=false)
    reload()
   }catch (_:Exception){ui.value=ui.value.copy(busy=false,error="تعذر حفظ المقال؛ تحقق من الرابط الوصفي وبيانات المقال.")}
  }
 }
 fun delete(id:String){
  viewModelScope.launch{
   ui.value=ui.value.copy(busy=true,error="")
   try{
    api.change(credential(),WriterAction("delete",id=id))
    ui.value=ui.value.copy(busy=false,editorOpen=false)
    reload()
   }catch (_:Exception){ui.value=ui.value.copy(busy=false,error="فشل الحذف. تحقق من الاتصال والصلاحية.")}
  }
 }
 fun logout(){secureSession.clear();token=null;expiresAt=0;ui.value=WriterUiState(restoring=false)}
 override fun onCleared(){token=null;super.onCleared()}
}

@Composable fun WriterPanel(vm:WriterViewModel, back:()->Unit){
 val state by vm.ui.collectAsState()
 var email by remember{mutableStateOf("")}
 var password by remember{mutableStateOf("")}
 var remember by rememberSaveable{mutableStateOf(true)}
 Column(Modifier.fillMaxSize().padding(20.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Text("لوحة الكاتب",style=MaterialTheme.typography.headlineMedium)
   TextButton(onClick=back){Text("عودة")}
  }
  if(state.error.isNotBlank())Text(state.error,color=MaterialTheme.colorScheme.error)
  if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(state.restoring){ Text("جار استعادة جلسة الكاتب المحفوظة…"); return@Column }
  if(!state.authorized){
   Spacer(Modifier.height(20.dp))
   Text("إدارة مقالات Neon من التطبيق. بيانات الكاتب محمية، ولا تُحفظ كلمة المرور.")
   OutlinedTextField(email,{email=it},label={Text("البريد الإداري")},singleLine=true,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email))
   OutlinedTextField(password,{password=it},label={Text("كلمة المرور")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
   Row(verticalAlignment=Alignment.CenterVertically){Text("تذكر دخولي بأمان على هذا الجهاز (30 يومًا)",Modifier.weight(1f));Switch(remember,onCheckedChange={remember=it})}
   Button(onClick={vm.login(email,password,remember);password=""},enabled=!state.busy&&email.isNotBlank()&&password.isNotBlank()){Text("دخول الكاتب")}
  } else if(state.editorOpen){
   WriterEditor(state.editing,state.busy,vm::save,vm::closeEditor,vm::delete,vm::uploadMedia)
  } else {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    Button(onClick={vm.edit(null)}){Text("مقال جديد")}
    TextButton(onClick=vm::reload){Text("تحديث")}
    TextButton(onClick=vm::logout){Text("تسجيل الخروج")}
   }
   Text("المقالات المنشورة والمسودات: "+state.articles.size)
   LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){
    items(state.articles,key={it.id}){a->
     Card(onClick={vm.edit(a)},shape=RoundedCornerShape(16.dp)){
      Column(Modifier.fillMaxWidth().padding(16.dp)){
       Text(a.title,style=MaterialTheme.typography.titleMedium)
       Text((if(a.status=="published")"منشور" else "مسودة")+" • "+a.category)
       Text("اضغط لتعديل المقال",style=MaterialTheme.typography.bodySmall)
      }
     }
    }
   }
  }
 }
}

@Composable private fun WriterEditor(original:ArticleDto?,busy:Boolean,save:(ArticleDto)->Unit,cancel:()->Unit,delete:(String)->Unit,uploadMedia:(Uri,String,(String)->Unit)->Unit){
 var title by rememberSaveable(original?.id){mutableStateOf(original?.title.orEmpty())}
 var author by rememberSaveable(original?.id){mutableStateOf(original?.author?.ifBlank{"معتز العلقمي"}?:"معتز العلقمي")}
 var authorImage by rememberSaveable(original?.id){mutableStateOf(original?.authorImage.orEmpty())}
 var slug by rememberSaveable(original?.id){mutableStateOf(original?.slug.orEmpty())}
 var category by rememberSaveable(original?.id){mutableStateOf(original?.category?:"general")}
 var excerpt by rememberSaveable(original?.id){mutableStateOf(original?.excerpt.orEmpty())}
 var readTime by rememberSaveable(original?.id){mutableStateOf(original?.readTime?:"5 دقائق")}
 var content by rememberSaveable(original?.id){mutableStateOf(original?.content.orEmpty())}
 var cover by rememberSaveable(original?.id){mutableStateOf(original?.coverImage.orEmpty())}
 var tags by rememberSaveable(original?.id){mutableStateOf(original?.tags?.joinToString("،").orEmpty())}
 var featured by rememberSaveable(original?.id){mutableStateOf(original?.featured?:false)}
 var fontFamily by rememberSaveable(original?.id){mutableStateOf(original?.fontFamily?:"amiri")}
 var fontSize by rememberSaveable(original?.id){mutableIntStateOf(original?.fontSize?:22)}
 var preview by rememberSaveable(original?.id){mutableStateOf(false)}
 val context=LocalContext.current
 val markdown=remember(context){MarkdownEngine(context)}

 var selectedPurpose by remember{mutableStateOf("media")}
 val mediaPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
  if(uri!=null)uploadMedia(uri,selectedPurpose){url->
   when(selectedPurpose){
    "cover"->cover=url
    "author"->authorImage=url
    else->content+="\n\n![ملف مرفق]("+url+")\n\n"
   }
  }
 }
 fun pick(purpose:String){
  selectedPurpose=purpose
  mediaPicker.launch(if(purpose=="cover"||purpose=="author")"image/*" else "*/*")
 }
 var confirm by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text(if(original==null)"مقال جديد" else "تعديل المقال",style=MaterialTheme.typography.titleLarge)
  OutlinedTextField(title,{title=it.take(220)},label={Text("العنوان")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(author,{author=it.take(150)},label={Text("اسم كاتب المقالة")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(authorImage,{authorImage=it.take(2048)},label={Text("رابط صورة الكاتب HTTPS")},modifier=Modifier.fillMaxWidth())
  OutlinedButton(onClick={pick("author")},enabled=!busy){Text("رفع صورة الكاتب من الهاتف (حتى 1.8 ميجابايت)")}
  if(authorImage.startsWith("https://")){
   coil.compose.AsyncImage(model=authorImage,contentDescription="صورة الكاتب",
    modifier=Modifier.size(70.dp).clip(androidx.compose.foundation.shape.CircleShape),
    contentScale=androidx.compose.ui.layout.ContentScale.Crop)
  }
  OutlinedTextField(slug,{slug=it.take(150)},label={Text("الرابط الوصفي")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(category,{category=it.take(60)},label={Text("معرّف القسم")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(cover,{cover=it.take(2048)},label={Text("رابط صورة الغلاف HTTPS")},modifier=Modifier.fillMaxWidth())
  OutlinedButton(onClick={pick("cover")},enabled=!busy){Text("رفع صورة الغلاف من الهاتف (حتى 1.8 ميجابايت)")}
  if(cover.startsWith("https://"))coil.compose.AsyncImage(model=cover,contentDescription="معاينة صورة الغلاف",modifier=Modifier.fillMaxWidth().height(160.dp),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
  OutlinedTextField(excerpt,{excerpt=it.take(1000)},label={Text("ملخص المقال")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(readTime,{readTime=it.take(100)},label={Text("مدة القراءة التقديرية")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(tags,{tags=it.take(500)},label={Text("وسوم مفصولة بفواصل")},modifier=Modifier.fillMaxWidth())
  OutlinedButton(onClick={pick("media")},enabled=!busy){Text("إضافة مرفق من الجهاز (حتى 1.8 ميجابايت)")}
  Text("أدوات تنسيق المقال",style=MaterialTheme.typography.titleMedium)
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   AssistChip(onClick={content+="\n## عنوان فرعي\n"},label={Text("عنوان")})
   AssistChip(onClick={content+="**نص عريض**"},label={Text("عريض")})
   AssistChip(onClick={content+="*نص مائل*"},label={Text("مائل")})
   AssistChip(onClick={content+="\n> اقتباس أو بيت شعر\n"},label={Text("اقتباس")})
   AssistChip(onClick={content+="\n- عنصر من القائمة\n"},label={Text("قائمة")})
   AssistChip(onClick={content+="[عنوان الرابط](https://example.com)"},label={Text("رابط")})
   AssistChip(onClick={content+="![وصف الصورة](https://example.com/photo.jpg)"},label={Text("صورة")})
   AssistChip(onClick={content+="![عنوان الفيديو](https://example.com/video.mp4)"},label={Text("فيديو")})
   AssistChip(onClick={content+="![تسجيل صوتي](https://example.com/audio.mp3)"},label={Text("صوت")})
  }
  Text("خط المقالة",style=MaterialTheme.typography.titleSmall)
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
   listOf("amiri" to "أميري","naskh" to "نسخ","tajawal" to "تجوال").forEach{(id,label)->
    FilterChip(selected=fontFamily==id,onClick={fontFamily=id},label={Text(label)})
   }
  }
  Text("حجم الخط: $fontSize")
  Slider(value=fontSize.toFloat(),onValueChange={fontSize=it.toInt()},valueRange=16f..32f,steps=15)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Text("الكلمات: "+content.trim().split(Regex("\\s+")).count{it.isNotBlank()})
   TextButton(onClick={preview=!preview}){Text(if(preview)"متابعة التحرير" else "معاينة حيّة")}
  }
  if(preview){
   AndroidView(factory={TextView(it).apply{
    textDirection=android.view.View.TEXT_DIRECTION_FIRST_STRONG_RTL
    layoutDirection=android.view.View.LAYOUT_DIRECTION_RTL
    setPadding(24,24,24,24)
   }},modifier=Modifier.fillMaxWidth().heightIn(min=240.dp),update={view->
    markdown.render(view,content)
    view.textSize=fontSize.toFloat()
    view.typeface=when(fontFamily){
     "tajawal"->android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL)
     "naskh"->android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL)
     else->android.graphics.Typeface.create("serif",android.graphics.Typeface.NORMAL)
    }
   })
  }else {
   OutlinedTextField(content,{content=it.take(180000)},label={Text("النص الكامل (Markdown)")},minLines=12,modifier=Modifier.fillMaxWidth())
  }
  Row(verticalAlignment=Alignment.CenterVertically){
   Text("مقال مميّز",Modifier.weight(1f));Switch(featured,onCheckedChange={featured=it})
  }
  val valid=title.isNotBlank()&&content.isNotBlank()&&(cover.isBlank()||cover.startsWith("https://"))&&(authorImage.isBlank()||authorImage.startsWith("https://"))
  fun prepared(status:String):ArticleDto{
   val articleSlug=slug.ifBlank{ "article-"+System.currentTimeMillis() }
   return (original?:ArticleDto()).copy(title=title.trim(),slug=articleSlug,category=category,
    content=content,excerpt=excerpt,coverImage=cover,readTime=readTime,featured=featured,status=status,
    author=author.trim().ifBlank{"معتز العلقمي"},authorImage=authorImage.trim(),
    publishDate=original?.publishDate?.takeIf{it.isNotBlank()} ?: java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date()),
    tags=tags.split('،',',').map{it.trim()}.filter{it.isNotBlank()},fontFamily=fontFamily,fontSize=fontSize)
  }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Button(onClick={save(prepared("published"))},enabled=valid&&!busy){Text("نشر")}
   OutlinedButton(onClick={save(prepared("draft"))},enabled=valid&&!busy){Text("مسودة")}
   TextButton(onClick=cancel){Text("إلغاء")}
  }
  if(original!=null)TextButton(onClick={confirm=true},enabled=!busy){Text("حذف المقال",color=MaterialTheme.colorScheme.error)}
  Spacer(Modifier.height(28.dp))
 }
 if(confirm&&original!=null)AlertDialog(onDismissRequest={confirm=false},title={Text("حذف المقال نهائيًا؟")},
  text={Text("سيحذف المقال من Neon والموقع وتطبيقات القراءة عند المزامنة.")},
  confirmButton={TextButton(onClick={delete(original.id);confirm=false}){Text("حذف")}},
  dismissButton={TextButton(onClick={confirm=false}){Text("إلغاء")}})
}
