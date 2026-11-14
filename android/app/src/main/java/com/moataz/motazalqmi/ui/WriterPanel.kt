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

data class WriterLogin(val email:String,val password:String)
data class WriterAccess(val accessToken:String="",val expiresAt:Long=0L)
data class WriterList(val articles:List<ArticleDto> = emptyList())
data class WriterAction(val action:String,val article:ArticleDto?=null,val id:String?=null)
interface WriterApi {
 @POST("api/v1/mobile-admin") suspend fun login(@Body value:WriterLogin):WriterAccess
 @GET("api/articles") suspend fun articles(@Query("scope") scope:String,@Header("Authorization") bearer:String):WriterList
 @POST("api/articles") suspend fun change(@Header("Authorization") bearer:String,@Body change:WriterAction)
}

data class WriterUiState(val authorized:Boolean=false,val busy:Boolean=false,val error:String="",
 val articles:List<ArticleDto> = emptyList(),val editing:ArticleDto?=null,val editorOpen:Boolean=false)

@HiltViewModel class WriterViewModel @Inject constructor(private val api:WriterApi):ViewModel(){
 val ui=MutableStateFlow(WriterUiState())
 // Deliberately ephemeral: never store the password or bearer token on disk.
 private var token:String?=null
 private var expiresAt:Long=0
 fun login(email:String,password:String){
  if(ui.value.busy)return
  viewModelScope.launch{
   ui.value=ui.value.copy(busy=true,error="")
   try {
    val session=api.login(WriterLogin(email.trim(),password))
    require(session.accessToken.isNotBlank() && session.expiresAt>System.currentTimeMillis())
    token=session.accessToken;expiresAt=session.expiresAt
    ui.value=ui.value.copy(authorized=true,busy=false)
    reload()
   }catch (_:Exception){ui.value=ui.value.copy(busy=false,error="تعذر تسجيل الدخول. تحقق من البيانات والاتصال.") }
  }
 }
 private fun credential():String {
  check(token!=null&&System.currentTimeMillis()<expiresAt){"انتهت جلسة الكاتب"}
  return "Bearer "+token
 }
 fun reload(){
  viewModelScope.launch{
   ui.value=ui.value.copy(busy=true,error="")
   try{
    val articles=api.articles("admin",credential()).articles
    ui.value=ui.value.copy(articles=articles,busy=false,authorized=true)
   }catch (_:Exception){ui.value=ui.value.copy(busy=false,error="تعذر تحميل مقالات الكاتب. أعد تسجيل الدخول إذا انتهت الجلسة.")}
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
 fun logout(){token=null;expiresAt=0;ui.value=WriterUiState()}
 override fun onCleared(){token=null;super.onCleared()}
}

@Composable fun WriterPanel(vm:WriterViewModel, back:()->Unit){
 val state by vm.ui.collectAsState()
 var email by remember{mutableStateOf("")}
 var password by remember{mutableStateOf("")}
 Column(Modifier.fillMaxSize().padding(20.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Text("لوحة الكاتب",style=MaterialTheme.typography.headlineMedium)
   TextButton(onClick=back){Text("عودة")}
  }
  if(state.error.isNotBlank())Text(state.error,color=MaterialTheme.colorScheme.error)
  if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(!state.authorized){
   Spacer(Modifier.height(20.dp))
   Text("إدارة مقالات Neon من التطبيق. بيانات الكاتب محمية، ولا تُحفظ كلمة المرور.")
   OutlinedTextField(email,{email=it},label={Text("البريد الإداري")},singleLine=true,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email))
   OutlinedTextField(password,{password=it},label={Text("كلمة المرور")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
   Button(onClick={vm.login(email,password);password=""},enabled=!state.busy&&email.isNotBlank()&&password.isNotBlank()){Text("دخول الكاتب")}
  } else if(state.editorOpen){
   WriterEditor(state.editing,state.busy,vm::save,vm::closeEditor,vm::delete)
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

@Composable private fun WriterEditor(original:ArticleDto?,busy:Boolean,save:(ArticleDto)->Unit,cancel:()->Unit,delete:(String)->Unit){
 var title by rememberSaveable(original?.id){mutableStateOf(original?.title.orEmpty())}
 var slug by rememberSaveable(original?.id){mutableStateOf(original?.slug.orEmpty())}
 var category by rememberSaveable(original?.id){mutableStateOf(original?.category?:"general")}
 var excerpt by rememberSaveable(original?.id){mutableStateOf(original?.excerpt.orEmpty())}
 var readTime by rememberSaveable(original?.id){mutableStateOf(original?.readTime?:"5 دقائق")}
 var content by rememberSaveable(original?.id){mutableStateOf(original?.content.orEmpty())}
 var cover by rememberSaveable(original?.id){mutableStateOf(original?.coverImage.orEmpty())}
 var tags by rememberSaveable(original?.id){mutableStateOf(original?.tags?.joinToString("،").orEmpty())}
 var featured by rememberSaveable(original?.id){mutableStateOf(original?.featured?:false)}
 var confirm by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text(if(original==null)"مقال جديد" else "تعديل المقال",style=MaterialTheme.typography.titleLarge)
  OutlinedTextField(title,{title=it.take(220)},label={Text("العنوان")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(slug,{slug=it.take(150)},label={Text("الرابط الوصفي")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(category,{category=it.take(60)},label={Text("معرّف القسم")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(cover,{cover=it.take(2048)},label={Text("رابط صورة الغلاف HTTPS")},modifier=Modifier.fillMaxWidth())
  if(cover.startsWith("https://"))coil.compose.AsyncImage(model=cover,contentDescription="معاينة صورة الغلاف",modifier=Modifier.fillMaxWidth().height(160.dp),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
  OutlinedTextField(excerpt,{excerpt=it.take(1000)},label={Text("ملخص المقال")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(readTime,{readTime=it.take(100)},label={Text("مدة القراءة التقديرية")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(tags,{tags=it.take(500)},label={Text("وسوم مفصولة بفواصل")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(content,{content=it.take(180000)},label={Text("النص الكامل (Markdown)")},minLines=10,modifier=Modifier.fillMaxWidth())
  Row(verticalAlignment=Alignment.CenterVertically){
   Text("مقال مميّز",Modifier.weight(1f));Switch(featured,onCheckedChange={featured=it})
  }
  val valid=title.isNotBlank()&&content.isNotBlank()&&(cover.isBlank()||cover.startsWith("https://"))
  fun prepared(status:String):ArticleDto{
   val articleSlug=slug.ifBlank{ "article-"+System.currentTimeMillis() }
   return (original?:ArticleDto()).copy(title=title.trim(),slug=articleSlug,category=category,
    content=content,excerpt=excerpt,coverImage=cover,readTime=readTime,featured=featured,status=status,
    author=original?.author?.takeIf{it.isNotBlank()} ?: "معتز العلقمي",
    publishDate=original?.publishDate?.takeIf{it.isNotBlank()} ?: java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date()),
    tags=tags.split('،',',').map{it.trim()}.filter{it.isNotBlank()})
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
