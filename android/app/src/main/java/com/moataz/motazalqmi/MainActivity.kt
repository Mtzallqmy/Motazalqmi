package com.moataz.motazalqmi

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import org.json.JSONObject
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow

private val Ink=Color(0xFF213835)
private val Teal=Color(0xFF0D5C63)
private val Gold=Color(0xFFC09751)
private val Paper=Color(0xFFFAF7EF)
private val DarkPaper=Color(0xFF141D1D)

private fun JSONObject.string(key:String)=optString(key, "")
private fun JSONObject.article()=BlogArticle(
    id=string("id"),slug=string("slug"),title=string("title"),excerpt=string("excerpt"),
    content=string("content"),category=string("category"),coverImage=string("coverImage"),
    author=string("author"),date=string("publishDate"),readTime=string("readTime"),featured=optBoolean("featured"))

data class BlogArticle(val id:String,val slug:String,val title:String,val excerpt:String,val content:String,
                       val category:String,val coverImage:String,val author:String,val date:String,
                       val readTime:String,val featured:Boolean)
data class BlogCategory(val id:String,val name:String,val subtitle:String)
data class BlogQuote(val text:String,val author:String,val source:String)

class BlogViewModel(app:Application):AndroidViewModel(app){
    private val repo=BlogStore.get(app)
    private val prefs=app.getSharedPreferences("reader-preferences-v2",0)
    var articles by mutableStateOf(repo.articles().map { it.article() });private set
    var categories by mutableStateOf(repo.sections().map { BlogCategory(it.string("id"),it.string("title"),it.string("subtitle")) });private set
    var quotes by mutableStateOf(repo.quotes().map { BlogQuote(it.string("text"),it.string("author"),it.string("source")) });private set
    var site by mutableStateOf(repo.site()?.string("about") ?: "مدونة ثقافية تهتم باللغة والأدب والتاريخ والفكر والشعر العربي.");private set
    var isSyncing by mutableStateOf(false);private set
    var syncError by mutableStateOf("");private set
    var isDark by mutableStateOf(prefs.getBoolean("dark",false));private set
    var fontSize by mutableFloatStateOf(prefs.getFloat("font_size",19f));private set
    var bookmarks by mutableStateOf(repo.favorites());private set
    var lastSync by mutableLongStateOf(repo.syncedAt());private set
    private var requested=false

    fun sync(){
        if(isSyncing)return
        isSyncing=true;syncError=""
        repo.sync { success,error->
            isSyncing=false
            if(success){
                articles=repo.articles().map { it.article() }
                categories=repo.sections().map { BlogCategory(it.string("id"),it.string("title"),it.string("subtitle")) }
                quotes=repo.quotes().map { BlogQuote(it.string("text"),it.string("author"),it.string("source")) }
                site=repo.site()?.string("about") ?: site
                lastSync=repo.syncedAt()
            } else syncError=error
        }
    }
    fun initialSync(){if(!requested){requested=true;sync()}}
    fun toggleBookmark(id:String){repo.toggleFavorite(id);bookmarks=repo.favorites()}
    fun setDark(value:Boolean){isDark=value;prefs.edit().putBoolean("dark",value).apply()}
    fun setFontSize(value:Float){fontSize=value.coerceIn(15f,32f);prefs.edit().putFloat("font_size",fontSize).apply()}
    fun position(id:String)=prefs.getInt("position_$id",0)
    fun savePosition(id:String,pos:Int){prefs.edit().putInt("position_$id",pos).apply()}
    fun fetchArticle(slug:String, done:(BlogArticle?)->Unit){
        articles.firstOrNull{it.slug==slug}?.let{done(it);return}
        repo.retrieveArticle(slug){json->done(json?.article())}
    }
}

class MainActivity:ComponentActivity(){
    private var openedSlug by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        intent?.data?.let{openedSlug=slugFromUrl(it)}
        setContent{val vm:BlogViewModel=viewModel();BlogApplication(vm,openedSlug){openedSlug=null}}
    }
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);openedSlug=intent.data?.let(::slugFromUrl)}
    private fun slugFromUrl(uri:Uri):String?=if(uri.scheme=="https" && uri.host=="motazalqmi.vercel.app"
        && uri.pathSegments.size==2 && uri.pathSegments[0]=="articles")uri.pathSegments[1] else null
}

@Composable
private fun BlogApplication(vm:BlogViewModel,pendingSlug:String?,consumed:()->Unit){
    var tab by rememberSaveable { mutableStateOf("home") }
    var category by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<BlogArticle?>(null) }
    LaunchedEffect(Unit){vm.initialSync()}
    LaunchedEffect(pendingSlug){if(!pendingSlug.isNullOrBlank())vm.fetchArticle(pendingSlug){a->if(a!=null)selected=a;consumed()}}
    val palette=if(vm.isDark) darkColorScheme(primary=Color(0xFF80C9C4),secondary=Gold,background=DarkPaper,
        surface=Color(0xFF22302E),onBackground=Color(0xFFF2EDE2),onSurface=Color(0xFFF2EDE2))
        else lightColorScheme(primary=Teal,secondary=Gold,background=Paper,surface=Color.White,onBackground=Ink,onSurface=Ink)
    MaterialTheme(colorScheme=palette,typography=Typography()) {
        BackHandler(enabled=selected!=null){selected=null}
        Scaffold(
            topBar={
                Surface(color=MaterialTheme.colorScheme.primary,contentColor=Color.White){
                    Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min=64.dp).padding(horizontal=14.dp),
                        verticalAlignment=Alignment.CenterVertically){
                        if(selected!=null)IconButton(onClick={selected=null}){
                            Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription="العودة",tint=Color.White)
                        }
                        Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                            Text("مُدَوَّنَةُ مُعْتَز العَلْقَمِي",fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=20.sp,textAlign=TextAlign.Center)
                            Text(if(selected!=null)"واحة القراءة" else "حيث تلتقي الكلمة بالفكر",fontSize=11.sp,color=Color(0xFFD5E8E5))
                        }
                        IconButton(onClick={vm.sync()},enabled=!vm.isSyncing){
                            Icon(Icons.Default.Refresh,contentDescription="مزامنة المقالات",tint=Color.White)
                        }
                    }
                }
            },
            bottomBar={if(selected==null)NavigationBar(containerColor=MaterialTheme.colorScheme.surface){
                listOf(Triple("home","الرئيسية",Icons.Default.Home),
                    Triple("sections","الأقسام",Icons.AutoMirrored.Filled.MenuBook),
                    Triple("saved","المحفوظات",Icons.Default.Bookmark),
                    Triple("more","المزيد",Icons.Default.Info)).forEach { (id,title,icon)->
                    NavigationBarItem(selected=tab==id,onClick={tab=id;search="";category=""},
                        label={Text(title,fontSize=11.sp)},icon={Icon(icon,contentDescription=title)})
                }
            }}
        ) {padding->
            Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)){
                if(vm.isSyncing)LinearProgressIndicator(Modifier.fillMaxWidth())
                if(vm.syncError.isNotBlank())Text(vm.syncError,Modifier.fillMaxWidth().padding(8.dp),color=MaterialTheme.colorScheme.error,textAlign=TextAlign.Center,fontSize=12.sp)
                val current=selected
                if(current!=null)Reader(current,vm)
                else when(tab){
                    "home"->ArticleLibrary(vm,search,{search=it},category,{category=it},{selected=it},saved=false)
                    "saved"->ArticleLibrary(vm,search,{search=it},"",{}, {selected=it},saved=true)
                    "sections"->SectionsAndQuotes(vm,{id->category=id;tab="home"})
                    else->MoreScreen(vm)
                }
            }
        }
    }
}

private fun normalizeArabic(s:String):String=s.lowercase(Locale.ROOT)
    .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"),"")
    .replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي')

@Composable
private fun ArticleLibrary(vm:BlogViewModel,search:String,onSearch:(String)->Unit,category:String,
                           onCategory:(String)->Unit,onOpen:(BlogArticle)->Unit,saved:Boolean){
    val shown=remember(vm.articles,vm.bookmarks,search,category,saved){vm.articles.filter {a->
        (!saved||a.id in vm.bookmarks) && (category.isBlank()||a.category==category) &&
        (search.isBlank()||normalizeArabic(listOf(a.title,a.excerpt,a.content,a.author).joinToString(" ")).contains(normalizeArabic(search.trim())))
    }}
    Column(Modifier.fillMaxSize()){
        if(!saved){
            val hero=vm.articles.firstOrNull{it.featured}
            if(hero!=null && category.isEmpty() && search.isEmpty()) {
                Surface(shape=RoundedCornerShape(22.dp),tonalElevation=1.dp,
                    modifier=Modifier.fillMaxWidth().padding(horizontal=14.dp, vertical=12.dp).clickable {onOpen(hero)}){
                    Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text("✦   من عيون الأدب",color=Gold,fontSize=12.sp,fontWeight=FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(hero.title,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=21.sp,lineHeight=32.sp,maxLines=3)
                            Spacer(Modifier.height(8.dp))
                            Text("اقرأ المقالة ←",color=MaterialTheme.colorScheme.primary,fontSize=12.sp)
                        }
                        if(hero.coverImage.startsWith("https://"))AsyncImage(model=hero.coverImage,contentDescription="غلاف المقالة",modifier=Modifier.size(92.dp).padding(start=10.dp))
                    }
                }
            }
        }
        OutlinedTextField(value=search,onValueChange=onSearch,singleLine=true,
            label={Text("ابحث في مكتبة المدونة")},leadingIcon={Icon(Icons.Default.Search,contentDescription=null)},
            modifier=Modifier.fillMaxWidth().padding(horizontal=15.dp,vertical=6.dp),shape=RoundedCornerShape(18.dp))
        if(!saved){
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=12.dp, vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                FilterChip(selected=category.isEmpty(),onClick={onCategory("")},label={Text("الكل")})
                vm.categories.filter{it.id!="about"&&it.id!="quotes"}.forEach{section->
                    FilterChip(selected=category==section.id,onClick={onCategory(section.id)},label={Text(section.name)})
                }
            }
        }
        Text(if(saved)"مقالاتك المحفوظة" else "المقالات والكتابات  •  ${shown.size}",
            Modifier.padding(horizontal=18.dp,vertical=12.dp),fontSize=19.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold)
        if(shown.isEmpty()){
            Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center){
                Text(if(saved)"لم تحفظ أي مقالة بعد. اضغط رمز الحفظ داخل المقالات."
                    else "لا توجد مقالات مطابقة. جرّب بحثًا مختلفًا أو حدّث المكتبة.",textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }else LazyColumn(contentPadding=PaddingValues(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(shown,key={it.id}) {a->
                Surface(Modifier.fillMaxWidth().padding(horizontal=14.dp).clickable{onOpen(a)},
                    shape=RoundedCornerShape(18.dp),tonalElevation=2.dp){
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        if(a.coverImage.startsWith("https://"))AsyncImage(model=a.coverImage,contentDescription="صورة غلاف ${a.title}",
                            modifier=Modifier.size(94.dp).padding(end=10.dp))
                        Column(Modifier.weight(1f)){
                            Text(vm.categories.find{it.id==a.category}?.name.orEmpty()+"  •  "+a.date,
                                fontSize=11.sp,color=MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(5.dp))
                            Text(a.title,fontFamily=FontFamily.Serif,fontSize=18.sp,lineHeight=26.sp,fontWeight=FontWeight.Bold,maxLines=3,overflow=TextOverflow.Ellipsis)
                            Text(a.excerpt,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(5.dp))
                            Text("اقرأ المقالة  •  ${a.readTime}",fontSize=11.sp,color=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionsAndQuotes(vm:BlogViewModel,onCategory:(String)->Unit){
    val clipboard=LocalClipboardManager.current
    val context=LocalContext.current
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("رفوف المكتبة",fontFamily=FontFamily.Serif,fontSize=25.sp,fontWeight=FontWeight.Bold)}
        items(vm.categories.filter{it.id!="about"&&it.id!="quotes"}){c->
            Surface(shape=RoundedCornerShape(16.dp),tonalElevation=2.dp,modifier=Modifier.fillMaxWidth().clickable{onCategory(c.id)}){
                Column(Modifier.padding(16.dp)){
                    Text("✦  ${c.name}",fontSize=19.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
                    Text(c.subtitle,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item{Spacer(Modifier.height(12.dp));Text("اقتباسات",fontFamily=FontFamily.Serif,fontSize=24.sp,fontWeight=FontWeight.Bold)}
        items(vm.quotes){quote->
            Surface(shape=RoundedCornerShape(17.dp),tonalElevation=2.dp,modifier=Modifier.fillMaxWidth()){
                Column(Modifier.padding(18.dp)){
                    Text("❝  ${quote.text}",fontFamily=FontFamily.Serif,fontSize=19.sp,lineHeight=32.sp)
                    Text("— ${quote.author}  •  ${quote.source}",fontSize=12.sp,color=MaterialTheme.colorScheme.secondary)
                    Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
                        TextButton(onClick={clipboard.setText(AnnotatedString(quote.text))}){Text("نسخ الاقتباس")}
                        TextButton(onClick={shareText(context,quote.text+"\n— "+quote.author)}){Text("مشاركة")}
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreScreen(vm:BlogViewModel){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("إعدادات القراءة",fontFamily=FontFamily.Serif,fontSize=26.sp,fontWeight=FontWeight.Bold)
        Surface(shape=RoundedCornerShape(18.dp),tonalElevation=2.dp){
            Column(Modifier.padding(18.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(if(vm.isDark)Icons.Default.DarkMode else Icons.Default.LightMode,contentDescription=null)
                    Text("  الوضع الليلي",Modifier.weight(1f))
                    Switch(checked=vm.isDark,onCheckedChange=vm::setDark)
                }
                Text("حجم الخط: ${vm.fontSize.toInt()}")
                Slider(value=vm.fontSize,onValueChange=vm::setFontSize,valueRange=15f..32f,steps=16)
                Text("الخط العربي الجيد يتيح قراءة أهدأ وأطول.",fontSize=vm.fontSize.sp,fontFamily=FontFamily.Serif)
            }
        }
        Text("حول المدونة",fontFamily=FontFamily.Serif,fontSize=23.sp,fontWeight=FontWeight.Bold)
        Text(vm.site,fontSize=17.sp,lineHeight=30.sp)
        Text("برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م",fontSize=13.sp,color=MaterialTheme.colorScheme.secondary)
        Text("Android 8.0+  •  الإصدار 2.0.0  •  المحتوى من Neon PostgreSQL",fontSize=12.sp)
        Text("المزامنة الأخيرة: "+if(vm.lastSync>0)java.text.DateFormat.getDateTimeInstance().format(java.util.Date(vm.lastSync)) else "لم تجرِ بعد",fontSize=12.sp)
        Button(onClick=vm::sync,enabled=!vm.isSyncing,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Refresh,null);Text(" تحديث المقالات الآن")}
        val context=LocalContext.current
        OutlinedButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://motazalqmi.vercel.app")))},modifier=Modifier.fillMaxWidth()){
            Text("زيارة المدونة الإلكترونية")
        }
    }
}

@Composable
private fun Reader(article:BlogArticle,vm:BlogViewModel){
    val context=LocalContext.current
    val paragraphs=remember(article.content){article.content.replace("\r\n","\n").split("\n").filter{it.isNotBlank()}}
    val state=rememberLazyListState(initialFirstVisibleItemIndex=vm.position(article.id).coerceIn(0,paragraphs.size+1))
    LaunchedEffect(article.id){snapshotFlow{state.firstVisibleItemIndex}.collectLatest{vm.savePosition(article.id,it)}}
    LazyColumn(state=state,contentPadding=PaddingValues(19.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){
        item(key="cover"){
            Column {
                if(article.coverImage.startsWith("https://"))AsyncImage(model=article.coverImage,contentDescription="غلاف المقالة",modifier=Modifier.fillMaxWidth().height(185.dp))
                Spacer(Modifier.height(18.dp))
                Text(article.date+"  •  "+article.readTime,fontSize=12.sp,color=MaterialTheme.colorScheme.secondary)
                Text(article.title,fontSize=30.sp,lineHeight=44.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(article.author,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
                    OutlinedButton(onClick={vm.toggleBookmark(article.id)}){
                        Icon(if(article.id in vm.bookmarks)Icons.Default.Bookmark else Icons.Default.BookmarkBorder,contentDescription=null)
                        Text(if(article.id in vm.bookmarks)" محفوظ" else " حفظ")
                    }
                    OutlinedButton(onClick={shareText(context,article.title+"\nhttps://motazalqmi.vercel.app/articles/"+article.slug)}){
                        Icon(Icons.Default.Share,contentDescription=null);Text(" مشاركة")
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically){
                    IconButton(onClick={vm.setFontSize(vm.fontSize-1f)}){Icon(Icons.Default.TextDecrease,"تصغير الخط")}
                    Text("حجم الخط: ${vm.fontSize.toInt()}",fontSize=12.sp)
                    IconButton(onClick={vm.setFontSize(vm.fontSize+1f)}){Icon(Icons.Default.TextIncrease,"تكبير الخط")}
                }
                HorizontalDivider(color=MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(12.dp))
            }
        }
        items(paragraphs.size,key={"p$it"}){index->
            val raw=paragraphs[index].trim()
            val isHeading=raw.startsWith("#")
            val isQuote=raw.startsWith(">")
            val text=raw.removePrefix("> ").replace(Regex("^#{1,6}\\s*"),"").replace("**","")
            Text(text,fontFamily=FontFamily.Serif,
                fontSize=(vm.fontSize+if(isHeading)5f else 0f).sp,
                lineHeight=(vm.fontSize*1.95f+if(isHeading)3f else 0f).sp,
                fontWeight=if(isHeading)FontWeight.Bold else FontWeight.Normal,
                color=if(isHeading||isQuote)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier=Modifier.fillMaxWidth().padding(vertical=if(isHeading)7.dp else 2.dp),textAlign=TextAlign.Start)
        }
        item{HorizontalDivider();Spacer(Modifier.height(20.dp));Text("✦  مدونة معتز العلقمي — تعز، اليمن",Modifier.fillMaxWidth(),fontSize=13.sp,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.secondary)}
    }
}

private fun shareText(context:android.content.Context,text:String){
    val send=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)}
    context.startActivity(Intent.createChooser(send,"مشاركة من مدونة معتز العلقمي"))
}
