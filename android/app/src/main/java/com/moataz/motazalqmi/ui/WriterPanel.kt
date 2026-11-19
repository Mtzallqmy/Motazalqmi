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
data class WriterList(val articles: List<ArticleDto> = emptyList(), val editorial: StudioEditorial? = null)
data class MediaDirectRequest(val mime:String,val base64:String,val purpose:String)
data class MediaDirectResponse(val url:String="")
data class WriterAction(val action:String,val expectedVersion:String?=null,val article:ArticleDto?=null,val id:String?=null)
data class WriterProfile(val id:String="",val name:String="",val image:String="",val bio:String="",val link:String="")
data class WriterProfiles(val authors:List<WriterProfile> = emptyList())
interface WriterApi {
 @POST("api/articles") suspend fun saveEditorial(@Header("Authorization") bearer: String, @Body value: StudioEditorialRequest)
 @POST("api/v1/telegram-pair") suspend fun telegram(@Header("Authorization") bearer: String, @Body value: StudioTelegramRequest): StudioTelegramStatus
 @GET("api/v1/assets") suspend fun media(@Header("Authorization") bearer:String,@Query("cursor") cursor:String?):WriterMediaLibrary
 @POST("api/v1/media-delete") suspend fun removeMedia(@Header("Authorization") bearer:String,@Body value:MediaDeleteRequest)
 @POST("api/v1/authors") suspend fun saveAuthor(@Header("Authorization") bearer:String,@Body value:AuthorChange):WriterAuthorResponse
 @GET("api/v1/authors") suspend fun authors():WriterProfiles
 @POST("api/v1/media-ticket") suspend fun ticket(@Header("Authorization") bearer:String,@Body value:TicketRequest):UploadTicket
 @retrofit2.http.Headers("x-api-version: 13","x-vercel-blob-access: public")
 @retrofit2.http.PUT("https://vercel.com/api/blob/") suspend fun putBlob(@Query("pathname") path:String,
  @Header("Authorization") scopedToken:String,@Header("x-content-type") mime:String,@Header("x-content-length") length:String,
  @Body body:okhttp3.RequestBody):MediaDirectResponse
 @POST("api/v1/mobile-admin") suspend fun login(@Body value:WriterLogin):WriterAccess
 @POST("api/v1/mobile-admin") suspend fun refresh(@Body value:WriterRefresh):WriterAccess
 @GET("api/articles") suspend fun articles(@Query("scope") scope:String,@Header("Authorization") bearer:String):WriterList
 @POST("api/articles") suspend fun change(@Header("Authorization") bearer:String,@Body change:WriterAction)
 @POST("api/v1/media-direct") suspend fun uploadMedia(@Header("Authorization") bearer:String,@Body value:MediaDirectRequest):MediaDirectResponse
}

data class WriterUiState(val authorized:Boolean=false,val busy:Boolean=false,val restoring:Boolean=true,val error:String="",
 val editorial: StudioEditorial? = null, val telegram: StudioTelegramStatus? = null,
 val studio:String="articles",val notice:String="",val media:List<WriterMedia> = emptyList(),val mediaCursor:String?=null,val pendingMedia:List<WriterPendingMedia> = emptyList(),val profiles:List<WriterProfile> = emptyList(),val progress:Int?=null,val articles:List<ArticleDto> = emptyList(),val editing:ArticleDto?=null,val editorOpen:Boolean=false)

@HiltViewModel class WriterViewModel @Inject constructor(
 private val api:WriterApi,private val secureSession:SecureWriterSession,
 @ApplicationContext private val context:Context
):ViewModel(){
 val ui=MutableStateFlow(WriterUiState())
 private val drafts=context.getSharedPreferences("writer-drafts-v3",Context.MODE_PRIVATE)
 private val gson=com.google.gson.Gson()
 fun recovery(id:String?):ArticleDto?=runCatching { gson.fromJson(drafts.getString(id?:"new",null),ArticleDto::class.java) }.getOrNull()
 fun keepDraft(id:String?,value:ArticleDto){ drafts.edit().putString(id?:"new",gson.toJson(value)).apply() }
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
    val result = api.articles("admin", credential())
    ui.value = ui.value.copy(articles = result.articles, editorial = result.editorial, busy = false, authorized = true)
   }catch (_:Exception){ui.value=ui.value.copy(busy=false,error="تعذر تحميل المقالات؛ تحقق من الاتصال أو أعد تسجيل الدخول.")}
  }
 }
 private var uploadJob:kotlinx.coroutines.Job?=null
 fun cancelUpload(){uploadJob?.cancel()}
 fun uploadMedia(uri:Uri,purpose:String,onDone:(String)->Unit){
  if(uploadJob?.isActive==true)return
  uploadJob=viewModelScope.launch {
   ui.value=ui.value.copy(busy=true,progress=0,error="")
   try{
    val url=uploadWriterFile(context,api,uri,purpose,credential()){p->ui.value=ui.value.copy(progress=p)}
    require(url.startsWith("https://"))
    onDone(url)
   }catch(e:kotlinx.coroutines.CancellationException){throw e}
   catch(_:Exception){ui.value=ui.value.copy(error="تعذّر رفع الملف. تحقق من النوع والاتصال ثم أعد اختيار الملف للمحاولة.")}
   finally{ui.value=ui.value.copy(busy=false,progress=null)}
  }
 }
 fun edit(value:ArticleDto?){
  ui.value=ui.value.copy(editing=value,editorOpen=true,error="")
  viewModelScope.launch { try{ui.value=ui.value.copy(profiles=api.authors().authors)}catch(_:java.io.IOException){}catch(_:retrofit2.HttpException){} }
 }
 fun closeEditor(){ui.value=ui.value.copy(editorOpen=false,error="")}
 fun save(value:ArticleDto){
  viewModelScope.launch{
   ui.value=ui.value.copy(busy=true,error="")
   try{
    api.change(credential(),WriterAction("save",article=value,expectedVersion=value.updatedAt.takeIf{it.isNotBlank()}))
    drafts.edit().remove(ui.value.editing?.id?:"new").apply()
    ui.value=ui.value.copy(editorOpen=false,busy=false)
    reload()
   }catch (e:Exception){ui.value=ui.value.copy(busy=false,error=studioError(e,"تعذر حفظ المقال؛ تحقق من الرابط الوصفي وبيانات المقال."))}
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
 fun selectStudio(value:String){
  ui.value=ui.value.copy(studio=value,error="",notice="")
  when(value){
   "media"->loadMedia(null)
   "telegram" -> checkTelegram("status")
   "authors"->viewModelScope.launch { try{ui.value=ui.value.copy(profiles=api.authors().authors)}catch(_:Exception){ui.value=ui.value.copy(error="تعذر تحميل الكتّاب")} }
  }
 }
 private fun studioError(error:Exception,fallback:String):String =
  if(error is retrofit2.HttpException)runCatching {
   gson.fromJson(error.response()?.errorBody()?.string(),com.google.gson.JsonObject::class.java).get("error").asString
  }.getOrDefault(fallback) else fallback
 fun loadMedia(cursor:String?){
  if(ui.value.busy)return
  viewModelScope.launch {
   ui.value=ui.value.copy(busy=true,error="")
   try{
    val page=api.media(credential(),cursor)
    ui.value=ui.value.copy(media=if(cursor==null)page.items else (ui.value.media+page.items).distinctBy{it.url},
      mediaCursor=page.cursor,pendingMedia=page.pendingDeletions)
   }catch(e:kotlinx.coroutines.CancellationException){throw e}
   catch(e:Exception){ui.value=ui.value.copy(error=studioError(e,"تعذر فتح مكتبة الوسائط"))}
   finally{ui.value=ui.value.copy(busy=false)}
  }
 }
 fun deleteMedia(url:String){
  if(ui.value.busy)return
  viewModelScope.launch {
   ui.value=ui.value.copy(busy=true,error="",notice="")
   try {
    api.removeMedia(credential(),MediaDeleteRequest(url))
    ui.value=ui.value.copy(media=ui.value.media.filter{it.url!=url},pendingMedia=ui.value.pendingMedia.filter{it.url!=url},notice="حُذف الملف من المخزن")
   }catch(e:kotlinx.coroutines.CancellationException){throw e}
   catch(e:Exception){ui.value=ui.value.copy(error=studioError(e,"تعذر الحذف. حدّث المكتبة وأعد المحاولة."))}
   finally{ui.value=ui.value.copy(busy=false)}
  }
 }
 fun saveProfile(value:AuthorChange,done:(WriterProfile)->Unit){
  if(ui.value.busy)return
  viewModelScope.launch {
   ui.value=ui.value.copy(busy=true,error="",notice="")
   try{
    val profile=api.saveAuthor(credential(),value).author
    ui.value=ui.value.copy(profiles=ui.value.profiles.filter{it.id!=profile.id}+profile,notice="حُفظ ملف الكاتب")
    done(profile)
   }catch(e:kotlinx.coroutines.CancellationException){throw e}
   catch(e:Exception){ui.value=ui.value.copy(error=studioError(e,"تعذر حفظ ملف الكاتب"))}
   finally{ui.value=ui.value.copy(busy=false)}
  }
 }
 fun saveEditorial(value: StudioEditorialFields) {
  if (ui.value.busy) return
  viewModelScope.launch {
   ui.value = ui.value.copy(busy = true, error = "", notice = "")
   try {
    api.saveEditorial(credential(), StudioEditorialRequest(editorial = value))
    ui.value = ui.value.copy(notice = "حُفظت إعدادات المدونة")
    reload()
   } catch (error: kotlinx.coroutines.CancellationException) { throw error }
   catch (error: Exception) { ui.value = ui.value.copy(error = studioError(error, "تعذّر حفظ إعدادات المدونة")) }
   finally { ui.value = ui.value.copy(busy = false) }
  }
 }
 fun checkTelegram(action: String) {
  if (ui.value.busy) return
  viewModelScope.launch {
   ui.value = ui.value.copy(busy = true, error = "")
   try { ui.value = ui.value.copy(telegram = api.telegram(credential(), StudioTelegramRequest(action))) }
   catch (error: kotlinx.coroutines.CancellationException) { throw error }
   catch (error: Exception) { ui.value = ui.value.copy(error = studioError(error, "تعذّر التحقق من Telegram")) }
   finally { ui.value = ui.value.copy(busy = false) }
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
 var query by rememberSaveable { mutableStateOf("") }
 var filter by rememberSaveable { mutableStateOf("all") }
 Column(Modifier.fillMaxSize().padding(20.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    MagazineIcon(com.moataz.motazalqmi.R.drawable.blog_feather)
    Text("لوحة الكاتب", style = MaterialTheme.typography.headlineMedium)
   }
   TextButton(onClick=back){Text("عودة")}
  }
  if(state.notice.isNotBlank())Text(state.notice,color=MaterialTheme.colorScheme.primary)
  if(state.error.isNotBlank())Text(state.error,color=MaterialTheme.colorScheme.error)
  state.progress?.let { Text("رفع الملف · $it٪"); TextButton(onClick=vm::cancelUpload){Text("إلغاء الرفع")} }
  if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(state.restoring){ Text("جار استعادة جلسة الكاتب المحفوظة…"); return@Column }
  if(state.authorized&&!state.editorOpen)Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   listOf("articles" to "المقالات","media" to "الوسائط","authors" to "الكتّاب", "editorial" to "الهوية والاقتباسات", "telegram" to "Telegram").forEach{(id,label)->
    FilterChip(state.studio==id,{vm.selectStudio(id)},{Text(label)})
   }
  }
  if(!state.authorized){
   Spacer(Modifier.height(20.dp))
   Text("سجّل الدخول لتحرير المقالات وإدارة الكتّاب والوسائط.")
   OutlinedTextField(email,{email=it},label={Text("البريد الإداري")},singleLine=true,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email))
   OutlinedTextField(password,{password=it},label={Text("كلمة المرور")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
   Row(verticalAlignment=Alignment.CenterVertically){Text("تذكر دخولي بأمان على هذا الجهاز (30 يومًا)",Modifier.weight(1f));Switch(remember,onCheckedChange={remember=it})}
   Button(onClick={vm.login(email,password,remember);password=""},enabled=!state.busy&&email.isNotBlank()&&password.isNotBlank()){Text("دخول الكاتب")}
  } else if(state.editorOpen){
   WriterEditor(state.editing,remember(state.editing?.id){vm.recovery(state.editing?.id)},vm::keepDraft,state.profiles,state.busy,vm::save,vm::closeEditor,vm::delete,vm::uploadMedia)
  } else if(state.studio=="media"){NativeMediaLibrary(state,vm::loadMedia,vm::deleteMedia)}
  else if(state.studio=="authors"){NativeAuthorManager(state,vm::saveProfile,vm::uploadMedia)}
  else if (state.studio == "editorial") { WriterEditorial(state.editorial, state.busy, vm::reload, vm::saveEditorial) }
  else if (state.studio == "telegram") { WriterTelegram(state.telegram, state.busy, vm::checkTelegram) }
  else {
   val articles = state.articles.filter { article ->
    val status = if (articleIsScheduled(article)) "scheduled" else article.status
    (filter == "all" || filter == status) && com.moataz.motazalqmi.domain.ArabicSearch.matches(query, article.title, article.author)
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    Button(onClick={vm.edit(null)}){Text("مقال جديد")}
    TextButton(onClick=vm::reload){Text("تحديث")}
    TextButton(onClick=vm::logout){Text("تسجيل الخروج")}
   }
   OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("ابحث في المنشورات") })
   Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    listOf("all" to "الكل", "published" to "منشور", "draft" to "مسودات", "scheduled" to "مجدول").forEach { (id, label) ->
     FilterChip(filter == id, { filter = id }, { Text(label) })
    }
   }
   Text("إدارة المنشورات (${articles.size})", style = MaterialTheme.typography.titleMedium)
   LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){
    items(articles,key={it.id}){a->
     Card(onClick={vm.edit(a)},shape=RoundedCornerShape(16.dp)){
      Column(Modifier.fillMaxWidth().padding(16.dp)){
       Text(a.title,style=MaterialTheme.typography.titleMedium)
       Text((if(articleIsScheduled(a))"مجدول إلى "+scheduleToLocal(a.scheduledAt) else if(a.status=="published")"منشور" else "مسودة")+" • "+a.category)
       Text("اضغط لتعديل المقال",style=MaterialTheme.typography.bodySmall)
      }
     }
    }
   }
  }
 }
}

@Composable private fun WriterEditor(original:ArticleDto?,recovered:ArticleDto?,keep:(String?,ArticleDto)->Unit,profiles:List<WriterProfile>,busy:Boolean,save:(ArticleDto)->Unit,cancel:()->Unit,delete:(String)->Unit,uploadMedia:(Uri,String,(String)->Unit)->Unit){
 val initial=recovered?:original
 var title by rememberSaveable(initial?.id){mutableStateOf(initial?.title.orEmpty())}
 var author by rememberSaveable(initial?.id){mutableStateOf(initial?.author?:"معتز العلقمي")}
 var authorImage by rememberSaveable(initial?.id){mutableStateOf(initial?.authorImage.orEmpty())}
 var slug by rememberSaveable(initial?.id){mutableStateOf(initial?.slug.orEmpty())}
 var scheduleLocal by rememberSaveable(initial?.id){mutableStateOf(scheduleToLocal(initial?.scheduledAt.orEmpty()))}
 var category by rememberSaveable(initial?.id){mutableStateOf(initial?.category?:"general")}
 var excerpt by rememberSaveable(initial?.id){mutableStateOf(initial?.excerpt.orEmpty())}
 var readTime by rememberSaveable(initial?.id){mutableStateOf(initial?.readTime?:"5 دقائق")}
 var content by rememberSaveable(initial?.id){mutableStateOf(initial?.content.orEmpty())}
 var cover by rememberSaveable(initial?.id){mutableStateOf(initial?.coverImage.orEmpty())}
 var tags by rememberSaveable(initial?.id){mutableStateOf(initial?.tags?.joinToString("،").orEmpty())}
 var featured by rememberSaveable(initial?.id){mutableStateOf(initial?.featured?:false)}
 var fontFamily by rememberSaveable(initial?.id){mutableStateOf(initial?.fontFamily?:"amiri")}
 var fontSize by rememberSaveable(initial?.id){mutableIntStateOf(initial?.fontSize?:22)}
 var preview by rememberSaveable(original?.id){mutableStateOf(false)}
 val context=LocalContext.current
 val markdown=remember(context){MarkdownEngine(context)}

 var selectedAuthor by rememberSaveable(initial?.id){mutableStateOf(initial?.authorId.orEmpty())}
 var start by rememberSaveable(initial?.id){mutableIntStateOf(content.length)}
 var end by rememberSaveable(initial?.id){mutableIntStateOf(content.length)}
 fun insertAtSelection(value:String){
  val a=minOf(start,end).coerceIn(0,content.length)
  val b=maxOf(start,end).coerceIn(a,content.length)
  content=content.replaceRange(a,b,value)
  start=a+value.length;end=start
 }
 var selectedPurpose by remember{mutableStateOf("media")}
 val mediaPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
  if(uri!=null)uploadMedia(uri,selectedPurpose){url->
   when(selectedPurpose){
    "cover"->cover=url
    "author"->authorImage=url
    else->insertAtSelection("\n\n![ملف مرفق]("+url+")\n\n")
   }
  }
 }
 fun pick(purpose:String){
  selectedPurpose=purpose
  mediaPicker.launch(if(purpose=="cover"||purpose=="author")"image/*" else "*/*")
 }
 var confirm by remember{mutableStateOf(false)}
 var confirmPublish by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text(if(original==null)"مقال جديد" else "تعديل المقال",style=MaterialTheme.typography.titleLarge)
  OutlinedTextField(title,{title=it.take(220)},label={Text("العنوان")},modifier=Modifier.fillMaxWidth())
  if(profiles.isNotEmpty())Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   profiles.forEach { profile -> FilterChip(selected=selectedAuthor==profile.id,onClick={
    selectedAuthor=profile.id;author=profile.name;authorImage=profile.image
   },label={Text(profile.name)}) }
  }
  OutlinedTextField(author,{author=it.take(150);selectedAuthor=""},label={Text("اسم كاتب المقالة")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(authorImage,{authorImage=it.take(2048)},label={Text("رابط صورة الكاتب HTTPS")},modifier=Modifier.fillMaxWidth())
  OutlinedButton(onClick={pick("author")},enabled=!busy){Text("رفع صورة الكاتب من الهاتف (حتى 100 ميجابايت)")}
  if(authorImage.startsWith("https://")){
   coil.compose.AsyncImage(model=authorImage,contentDescription="صورة الكاتب",
    modifier=Modifier.size(70.dp).clip(androidx.compose.foundation.shape.CircleShape),
    contentScale=androidx.compose.ui.layout.ContentScale.Crop)
  }
  OutlinedTextField(scheduleLocal,{scheduleLocal=it.take(30)},label={Text("موعد النشر yyyy-MM-dd HH:mm")},placeholder={Text("2027-01-20 20:30")},modifier=Modifier.fillMaxWidth())
  Text("التوقيت: "+java.time.ZoneId.systemDefault().id+". اترك الموعد فارغًا للنشر الفوري.",style=MaterialTheme.typography.bodySmall)
  OutlinedTextField(slug,{slug=it.take(150)},label={Text("الرابط الوصفي")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(category,{category=it.take(60)},label={Text("معرّف القسم")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(cover,{cover=it.take(2048)},label={Text("رابط صورة الغلاف HTTPS")},modifier=Modifier.fillMaxWidth())
  OutlinedButton(onClick={pick("cover")},enabled=!busy){Text("رفع صورة الغلاف من الهاتف (حتى 100 ميجابايت)")}
  if(cover.startsWith("https://"))coil.compose.AsyncImage(model=cover,contentDescription="معاينة صورة الغلاف",modifier=Modifier.fillMaxWidth().height(160.dp),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
  OutlinedTextField(excerpt,{excerpt=it.take(1000)},label={Text("ملخص المقال")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(readTime,{readTime=it.take(100)},label={Text("مدة القراءة التقديرية")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(tags,{tags=it.take(500)},label={Text("وسوم مفصولة بفواصل")},modifier=Modifier.fillMaxWidth())
  OutlinedButton(onClick={pick("media")},enabled=!busy){Text("إضافة مرفق من الجهاز (حتى 100 ميجابايت)")}
  Text("أدوات تنسيق المقال",style=MaterialTheme.typography.titleMedium)
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   AssistChip(onClick={insertAtSelection("\n## عنوان فرعي\n")},label={Text("عنوان")})
   AssistChip(onClick={insertAtSelection("**نص عريض**")},label={Text("عريض")})
   AssistChip(onClick={insertAtSelection("*نص مائل*")},label={Text("مائل")})
   AssistChip(onClick={insertAtSelection("\n> اقتباس أو بيت شعر\n")},label={Text("اقتباس")})
   AssistChip(onClick={insertAtSelection("\n- عنصر من القائمة\n")},label={Text("قائمة")})
   AssistChip(onClick={insertAtSelection("[عنوان الرابط](https://example.com)")},label={Text("رابط")})
   AssistChip(onClick={insertAtSelection("![وصف الصورة](https://example.com/photo.jpg)")},label={Text("صورة")})
   AssistChip(onClick={insertAtSelection("![عنوان الفيديو](https://example.com/video.mp4)")},label={Text("فيديو")})
   AssistChip(onClick={insertAtSelection("![تسجيل صوتي](https://example.com/audio.mp3)")},label={Text("صوت")})
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
   OutlinedTextField(androidx.compose.ui.text.input.TextFieldValue(content,androidx.compose.ui.text.TextRange(start.coerceIn(0,content.length),end.coerceIn(0,content.length))),
    {if(it.text.length<=180000){content=it.text;start=it.selection.start;end=it.selection.end}},label={Text("النص الكامل (Markdown)")},minLines=12,modifier=Modifier.fillMaxWidth())
  }
  Row(verticalAlignment=Alignment.CenterVertically){
   Text("مقال مميّز",Modifier.weight(1f));Switch(featured,onCheckedChange={featured=it})
  }
  val scheduleValid=runCatching{scheduleFromLocal(scheduleLocal)}.isSuccess
  if(!scheduleValid)Text("اكتب موعدًا صحيحًا مثل 2027-01-20 20:30",color=MaterialTheme.colorScheme.error)
  val valid=scheduleValid&&title.isNotBlank()&&content.isNotBlank()&&(cover.isBlank()||cover.startsWith("https://"))&&(authorImage.isBlank()||authorImage.startsWith("https://"))
  fun prepared(status:String):ArticleDto{
   val articleSlug=slug.ifBlank{ "article-"+System.currentTimeMillis() }
   return (initial?:ArticleDto()).copy(title=title.trim(),slug=articleSlug,category=category,
    content=content,excerpt=excerpt,coverImage=cover,readTime=readTime,featured=featured,status=status,scheduledAt=runCatching{scheduleFromLocal(scheduleLocal)}.getOrDefault(""),
    author=author.trim(),authorId=selectedAuthor,authorImage=authorImage.trim(),
    publishDate=original?.publishDate?.takeIf{it.isNotBlank()} ?: java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date()),
    tags=tags.split('،',',').map{it.trim()}.filter{it.isNotBlank()},fontFamily=fontFamily,fontSize=fontSize)
  }
  LaunchedEffect(scheduleLocal,title,author,selectedAuthor,authorImage,slug,category,excerpt,readTime,content,cover,tags,featured,fontFamily,fontSize){
   if(title.isNotBlank() || content.isNotBlank() || original!=null || recovered!=null)keep(original?.id,prepared("draft").copy(slug=slug))
  }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Button(onClick={confirmPublish=true},enabled=valid&&!busy){Text(if(scheduleLocal.isBlank())"نشر" else "جدولة")}
   OutlinedButton(onClick={save(prepared("draft"))},enabled=valid&&!busy){Text("مسودة")}
   TextButton(onClick=cancel){Text("إلغاء")}
  }
  if(confirmPublish)AlertDialog(onDismissRequest={confirmPublish=false},title={Text(if(scheduleLocal.isBlank())"نشر المقالة للقراء؟" else "جدولة المقالة حسب الموعد؟")},
   text={Text(title)},confirmButton={TextButton(onClick={confirmPublish=false;save(prepared("published"))}){Text("تأكيد النشر")}},
   dismissButton={TextButton(onClick={confirmPublish=false}){Text("إلغاء")}})
  if(original!=null)TextButton(onClick={confirm=true},enabled=!busy){Text("حذف المقال",color=MaterialTheme.colorScheme.error)}
  Spacer(Modifier.height(28.dp))
 }
 if(confirm&&original!=null)AlertDialog(onDismissRequest={confirm=false},title={Text("حذف المقال نهائيًا؟")},
  text={Text("سيحذف المقال من Neon والموقع وتطبيقات القراءة عند المزامنة.")},
  confirmButton={TextButton(onClick={delete(original.id);confirm=false}){Text("حذف")}},
  dismissButton={TextButton(onClick={confirm=false}){Text("إلغاء")}})
}
