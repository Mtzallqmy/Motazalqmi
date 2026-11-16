@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.moataz.motazalqmi.ui

import android.content.Intent
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.TextView
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.SubcomposeAsyncImage
import com.moataz.motazalqmi.R
import com.moataz.motazalqmi.BuildConfig
import com.moataz.motazalqmi.data.*
import com.moataz.motazalqmi.domain.ArabicSearch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview
import java.text.DateFormat
import java.util.Date
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlinx.coroutines.launch

private val tabs = listOf("home" to "الرئيسية", "search" to "البحث", "saved" to "مكتبتي", "settings" to "الإعدادات")
@Composable fun BlogApp(vm: BlogViewModel, deepSlug: String?) {
 val library by vm.library.collectAsStateWithLifecycle()
 val settings by vm.settings.collectAsStateWithLifecycle()
 val busy by vm.busy.collectAsStateWithLifecycle()
 val notice by vm.notice.collectAsStateWithLifecycle()
 val owner=LocalLifecycleOwner.current
 DisposableEffect(owner) {
  val observer=LifecycleEventObserver { _,event ->
   if(event==Lifecycle.Event.ON_RESUME)vm.syncIfStale()
  }
  owner.lifecycle.addObserver(observer)
  onDispose { owner.lifecycle.removeObserver(observer) }
 }
 val nav = rememberNavController()
 val back by nav.currentBackStackEntryAsState()
 val route = back?.destination?.route ?: "home"
 val snackbar = remember { SnackbarHostState() }
 LaunchedEffect(notice) { if (notice.isNotBlank()) { snackbar.showSnackbar(notice); vm.notice.value = "" } }
 var handled by rememberSaveable { mutableStateOf(false) }
 LaunchedEffect(library.articles, deepSlug) {
  if (!handled && deepSlug != null) library.articles.find { it.slug == deepSlug }?.let {
   handled = true; nav.navigate("article/${Uri.encode(it.id)}")
  }
 }
 Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
  if (!route.startsWith("article") && route != "writer") NavigationBar {
   tabs.forEachIndexed { index, (path, title) -> NavigationBarItem(selected = route == path,
    onClick = { nav.navigate(path) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
    icon = { Icon(listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.Bookmarks, Icons.Default.Settings)[index], title) }, label = { Text(title) }) }
  }
 }) { padding ->
  NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding),
   enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
   composable("home") { Home(library, settings, busy, vm::sync, { nav.navigate("article/${Uri.encode(it)}") }, { nav.navigate("section/${Uri.encode(it)}") }) }
   composable("section/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
    val id = entry.arguments?.getString("id").orEmpty()
    when (id) {
     "about" -> About(library.about)
     "quotes" -> Quotes(library, settings, vm::save) { nav.navigate("article/${Uri.encode(it)}") }
     else -> ArticleList(library.articles.filter { it.id in library.available && it.category == id }, settings,
      library.sections.find { it.id == id }?.title ?: "المقالات", { nav.navigate("article/${Uri.encode(it)}") })
    }
   }
   composable("search") { Search(library, settings) { nav.navigate("article/${Uri.encode(it)}") } }
   composable("saved") { Saved(library, settings, vm::save) { nav.navigate("article/${Uri.encode(it)}") } }
   composable("settings") { SettingsPage(settings, library.syncedAt, busy, vm::configure, vm::sync, vm::clean, { nav.navigate("section/about") }, { nav.navigate("writer") }) }
   composable("writer") { val writer = androidx.hilt.navigation.compose.hiltViewModel<WriterViewModel>(); WriterPanel(writer) { nav.popBackStack() } }
   composable("article/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
    val id = entry.arguments?.getString("id").orEmpty()
    val article = library.articles.find { it.id == id }
    if (article == null) Empty("هذا المقال غير متاح محليًا")
    else Reader(article, library, settings, vm, { nav.popBackStack() }, { nav.navigate("article/${Uri.encode(it)}") })
   }
  }
 }
}
@Composable private fun Title(text: String) { Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp)) }
@Composable private fun Empty(text: String = "لم يُنشر محتوى في هذا القسم بعد") {
 Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
  Icon(Icons.Default.MenuBook, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
  Text(text, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyLarge)
 }
}
@Composable private fun Home(library: Library, settings: ReadingSettings, busy: Boolean, sync: () -> Unit, open: (String) -> Unit, section: (String) -> Unit) { EditorialHome(library, settings, busy, sync, open, section) }
@Composable private fun ArticleCard(a: ArticleDto, settings: ReadingSettings, open: (String) -> Unit) { EditorialArticleCard(a, settings, open) }
@Composable internal fun Cover(url: String, settings: ReadingSettings) {
 val context = LocalContext.current
 val cm = context.getSystemService(ConnectivityManager::class.java)
 val wifi = cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
 if (url.startsWith("https://")) SubcomposeAsyncImage(model = coil.request.ImageRequest.Builder(context).data(readerImageUrl(url))
  .networkCachePolicy(if (settings.wifiImages && !wifi) coil.request.CachePolicy.DISABLED else coil.request.CachePolicy.ENABLED)
  .diskCachePolicy(coil.request.CachePolicy.ENABLED).build(),
  contentDescription = "صورة من المقال", modifier = Modifier.fillMaxWidth().height(180.dp),
  contentScale = androidx.compose.ui.layout.ContentScale.Crop,
  loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(30.dp)) } },
  error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("تعذر عرض الصورة. جرّب اتصالًا آخر.") } })
}
@Composable private fun ArticleList(articles: List<ArticleDto>, settings: ReadingSettings, title: String, open: (String) -> Unit) {
 LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
  item { Title(title) }; if (articles.isEmpty()) item { Empty() }
  items(articles, key = { it.id }) { ArticleCard(it, settings, open) }
 }
}
@Composable private fun Search(library: Library, settings: ReadingSettings, open: (String) -> Unit) {
 var query by rememberSaveable { mutableStateOf("") }
 var category by rememberSaveable { mutableStateOf("") }
 var oldest by rememberSaveable { mutableStateOf(false) }
 var results by remember { mutableStateOf(emptyList<ArticleDto>()) }
 LaunchedEffect(query, category, oldest, library) {
  kotlinx.coroutines.delay(180)
  results = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
   val found = library.articles.filter { a -> a.id in library.available && (category.isEmpty() || a.category == category) &&
    ArabicSearch.matches(query, a.title, a.content, a.author, a.tags.joinToString(" "), library.sections.find { it.id == a.category }?.title.orEmpty()) }
   if(oldest)found.sortedBy { it.publishDate } else found.sortedByDescending { it.publishDate }
  }
 }
 Column(Modifier.fillMaxSize()) {
  Text("اكتشف نصّك القادم",Modifier.padding(horizontal=20.dp,vertical=16.dp),style=MaterialTheme.typography.headlineMedium)
  OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal=20.dp),
   label = { Text("كلمة، عنوان، أو اسم كاتب") }, singleLine = true,
   trailingIcon = { if(query.isNotEmpty())IconButton(onClick={query=""}){Icon(Icons.Default.Close,"مسح البحث")} })
  LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   item { FilterChip(category.isEmpty(),{category=""},{Text("كل الأبواب")}) }
   items(library.sections.filter { it.id!="about" },key={it.id}) { item ->
    FilterChip(category==item.id,{category=item.id},{Text(item.title)})
   }
  }
  TextButton(onClick={oldest=!oldest},modifier=Modifier.padding(horizontal=12.dp)) { Text(if(oldest)"الأقدم أولًا ↓" else "الأحدث أولًا ↓") }
  Box(Modifier.weight(1f)) { ArticleList(results, settings, "${results.size} من النتائج", open) }
 }
}
@Composable private fun Quotes(library: Library, settings: ReadingSettings, save: (String) -> Unit, open: (String) -> Unit) {
 LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
  item { Title("اقتباسات") }; if (library.quotes.isEmpty()) item { Empty() }
  items(library.quotes, key = { quoteId(it) }) { QuoteCard(it, library.states[quoteId(it)]?.saved == true, { save(quoteId(it)) }) }
  items(library.articles.filter { it.id in library.available && it.category == "quotes" }, key = { it.id }) { ArticleCard(it, settings, open) }
 }
}
@Composable private fun QuoteCard(q: QuoteDto, saved: Boolean, save: () -> Unit, interactive: Boolean = true) {
 val clipboard = LocalClipboardManager.current; val context = LocalContext.current
 Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
  Column(Modifier.padding(24.dp)) {
   Icon(Icons.Default.FormatQuote, null, tint = MaterialTheme.colorScheme.primary)
   SelectionContainer { Text(q.text, Modifier.padding(vertical = 16.dp), fontFamily = Amiri, fontSize = 25.sp, lineHeight = 43.sp) }
   Text(q.author + " · " + q.source, fontFamily = Plex)
   if (interactive) Row {
    IconButton(onClick = save) { Icon(if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "حفظ الاقتباس") }
    IconButton(onClick = { clipboard.setText(AnnotatedString(q.text + "\n" + q.author)) }) { Icon(Icons.Default.ContentCopy, "نسخ الاقتباس") }
    IconButton(onClick = { share(context, q.text + "\n" + q.author + "\nhttps://motazalqmi.vercel.app") }) { Icon(Icons.Default.Share, "مشاركة الاقتباس") }
   }
  }
 }
}
@Composable private fun Saved(library: Library, settings: ReadingSettings, save: (String) -> Unit, open: (String) -> Unit) {
 var filter by rememberSaveable { mutableStateOf(0) }
 val articles = library.articles.filter { a -> val s = library.states[a.id]; when (filter) { 0 -> s?.saved == true; 1 -> (s?.openedAt ?: 0) > 0; else -> (s?.openedAt ?: 0) > 0 && (s?.progress ?: 0f) < .98f } }.sortedByDescending { library.states[it.id]?.openedAt ?: 0L }
 Column(Modifier.fillMaxSize()) {
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   listOf("المحفوظات", "سجل القراءة", "أكمل القراءة").forEachIndexed { i, label -> FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(label) }) }
  }
  LazyColumn(modifier = Modifier.testTag("saved-feed"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
   if (articles.isEmpty()) item { Empty("لا توجد مقالات هنا بعد") }
   items(articles, key = { it.id }) { ArticleCard(it, settings, open) }
   if (filter == 0) items(library.quotes.filter { library.states[quoteId(it)]?.saved == true }, key = { quoteId(it) }) { q -> QuoteCard(q, true, { save(quoteId(q)) }) }
  }
 }
}
private fun share(context: android.content.Context, text: String) {
 context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "مشاركة"))
}

@OptIn(FlowPreview::class)
@Composable private fun Reader(a: ArticleDto, library: Library, settings: ReadingSettings, vm: BlogViewModel, back: () -> Unit, open: (String) -> Unit) {
 val context = LocalContext.current
 val state = library.states[a.id]
 val scroll = rememberScrollState(state?.offset ?: 0)
 var focus by rememberSaveable(a.id) { mutableStateOf(false) }
 var restored by remember(a.id) { mutableStateOf(false) }
 val engine = remember { MarkdownEngine(context) }
 val parts = remember(a.content) { readerParts(a.content) }
 val headings = remember(parts) { parts.mapIndexedNotNull { index, part -> partHeading(part)?.let { index to it } } }
 val offsets = remember(a.id, a.content) { mutableMapOf<Int, Int>() }
 val scope = rememberCoroutineScope()
 var showIndex by rememberSaveable(a.id) { mutableStateOf(false) }
 var showSettings by rememberSaveable(a.id) { mutableStateOf(false) }
 var showAuthor by rememberSaveable(a.id) { mutableStateOf(false) }
 var rendered by remember(a.content) { mutableStateOf<android.text.Spanned?>(null) }
 LaunchedEffect(a.content) {
  rendered = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { engine.renderer.render(engine.renderer.parse(a.content)) }
 }
 LaunchedEffect(rendered, a.id) {
  if (rendered != null && !restored) {
   withFrameNanos { }; withFrameNanos { }
   scroll.scrollTo(state?.offset ?: 0); restored = true
  }
 }
 val ink = MaterialTheme.colorScheme.onBackground.toArgb()
 val fontId = when (settings.font) { "amiri" -> R.font.amiri; "plex" -> R.font.plex; else -> R.font.naskh }
 LaunchedEffect(a.id) {
  snapshotFlow { scroll.value to scroll.maxValue }.debounce(500).collect { (offset, max) ->
   if (restored && max > 0 && max < Int.MAX_VALUE) vm.position(a.id, 0, offset, offset.toFloat() / max)
  }
 }
 Column(Modifier.fillMaxSize()) {
  AnimatedVisibility(!focus) {
   TopAppBar(title = { Text("رحاب القراءة", fontFamily = Amiri) }, navigationIcon = {
    IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
   }, actions = {
    if (headings.isNotEmpty()) IconButton(onClick = { showIndex = true }) { Icon(Icons.Default.Toc, "فهرس المقالة") }
    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Tune, "إعدادات القراءة") }
    IconButton(onClick = { vm.save(a.id) }) { Icon(if (state?.saved == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "حفظ للقراءة لاحقًا") }
    IconButton(onClick = { share(context, a.title + "\nhttps://motazalqmi.vercel.app/articles/" + Uri.encode(a.slug)) }) { Icon(Icons.Default.Share, "مشاركة المقال") }
   })
  }
  LinearProgressIndicator(progress = { if (scroll.maxValue > 0 && scroll.maxValue < Int.MAX_VALUE) scroll.value.toFloat() / scroll.maxValue else 0f }, modifier = Modifier.fillMaxWidth())
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
   TextButton(onClick = { focus = !focus }) { Text(if (focus) "إظهار الأدوات" else "قراءة مركزة") }
   if (!focus) {
    IconButton(onClick = { vm.configure(settings.copy(size = (settings.size - 1).coerceAtLeast(16f))) }) { Icon(Icons.Default.TextDecrease, "تصغير الخط") }
    IconButton(onClick = { vm.configure(settings.copy(size = (settings.size + 1).coerceAtMost(34f))) }) { Icon(Icons.Default.TextIncrease, "تكبير الخط") }
    IconButton(onClick = { vm.configure(settings.copy(theme = if (settings.theme == "dark") "light" else "dark")) }) { Icon(Icons.Default.DarkMode, "تبديل وضع القراءة الليلي") }
   }
  }
  Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 24.dp, vertical = 16.dp)) {
   if (a.id !in library.available) Text("نسخة محفوظة؛ هذا المقال لم يعد منشورًا على الموقع.", color = MaterialTheme.colorScheme.secondary)
   Text(library.sections.find { it.id == a.category }?.title ?: "", color = MaterialTheme.colorScheme.secondary)
   Text(a.title, Modifier.padding(vertical = 18.dp), fontFamily = Amiri, fontSize = 32.sp, lineHeight = 48.sp)
   val minutes = (a.content.split(Regex("\\s+")).size / 180).coerceAtLeast(1)
   Row(Modifier.clickable(enabled = a.author.isNotBlank()) { showAuthor = true },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
    if(a.authorImage.startsWith("https://")){
     coil.compose.AsyncImage(model=readerImageUrl(a.authorImage),contentDescription="صورة الكاتب",
      modifier=Modifier.size(52.dp).clip(CircleShape).border(2.dp,MaterialTheme.colorScheme.secondary,CircleShape),
      contentScale=androidx.compose.ui.layout.ContentScale.Crop)
    }else{
     Surface(shape=CircleShape,color=MaterialTheme.colorScheme.secondaryContainer,modifier=Modifier.size(52.dp)){
      Box(contentAlignment=Alignment.Center){Text(a.author.take(1).ifEmpty{"ك"},style=MaterialTheme.typography.titleLarge)}
     }
    }
    Column {
     Text("بقلم",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
     Text(a.author.ifBlank{"كاتب المقالة"},style=MaterialTheme.typography.titleMedium)
     Text("${a.publishDate} · $minutes دقائق قراءة",style=MaterialTheme.typography.bodySmall)
    }
   }
   Spacer(Modifier.height(20.dp)); Cover(a.coverImage, settings); Spacer(Modifier.height(22.dp))
   if (rendered == null) CircularProgressIndicator()
   // Render image blocks at the exact position they appear in the article.
   // The original TextView-only Markwon renderer silently omitted Markdown image bytes.
   parts.forEachIndexed { index, part ->
    key(index) {
     Column(Modifier.fillMaxWidth().onGloballyPositioned { offsets[index] = it.positionInParent().y.toInt() }) {
     if(part.videoUrl!=null){
      Text(part.label.ifBlank{"فيديو مرفق"},style=MaterialTheme.typography.titleSmall)
      AndroidView(factory={ctx->
       VideoView(ctx).apply{
        setMediaController(MediaController(ctx))
        setVideoURI(Uri.parse(part.videoUrl))
        setOnErrorListener{_,_,_->true}
       }
      },modifier=Modifier.fillMaxWidth().height(220.dp))
      Text("اضغط تشغيل للمشاهدة؛ يلزم اتصال الإنترنت.",style=MaterialTheme.typography.labelSmall)
      Spacer(Modifier.height(16.dp))
     }else if(part.audioUrl!=null){
      ReaderAudio(part.audioUrl, part.label)
      Spacer(Modifier.height(16.dp))
     }else if(part.documentUrl!=null){
      val link=part.documentUrl
      OutlinedButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(link)))};Unit}){
       Icon(Icons.Default.OpenInNew,null)
       Text(part.label.ifBlank{"فتح الملف المرفق"})
      }
     }else if (part.imageUrl != null) {
      coil.compose.AsyncImage(model=readerImageUrl(part.imageUrl),contentDescription=part.label,
       modifier=Modifier.fillMaxWidth().heightIn(max=700.dp),contentScale=androidx.compose.ui.layout.ContentScale.FillWidth)
      if(part.label.isNotBlank())Text(part.label,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=8.dp))
      Spacer(Modifier.height(14.dp))
     } else if (!part.text.isNullOrBlank()) {
      val partContent = part.text.orEmpty()
      val parsed = remember(partContent) { engine.renderer.render(engine.renderer.parse(partContent)) }
      AndroidView(factory = { TextView(it).apply {
       textDirection = android.view.View.TEXT_DIRECTION_FIRST_STRONG_RTL
       layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
       setTextIsSelectable(true)
       movementMethod = android.text.method.LinkMovementMethod.getInstance()
      } }, modifier = Modifier.fillMaxWidth(), update = { view ->
       view.setTextColor(ink); view.setLinkTextColor(MaterialColors.link)
       view.typeface = ResourcesCompat.getFont(context, fontId) ?: Typeface.DEFAULT
       view.textSize = settings.size
       view.setLineSpacing(0f, settings.spacing)
       if (view.tag !== parsed) { engine.renderer.setParsedMarkdown(view, parsed); view.tag = parsed }
      })
     }
    }
   }
   }
   Spacer(Modifier.height(30.dp))
   val published = library.articles.filter { it.id in library.available }; val index = published.indexOfFirst { it.id == a.id }
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    published.getOrNull(index - 1)?.let { TextButton(onClick = { open(it.id) }) { Text("المقال السابق") } }
    if (index >= 0) published.getOrNull(index + 1)?.let { TextButton(onClick = { open(it.id) }) { Text("المقال التالي") } }
   }
   HorizontalDivider(); Text("برمجة وتطوير: معتز العلقمي — تعز — اليمن — 2026م", Modifier.padding(vertical = 24.dp), style = MaterialTheme.typography.bodyMedium)
  }
 }
 if (showSettings) ReaderSettingsSheet(settings, vm::configure) { showSettings = false }
 if (showIndex) AlertDialog(onDismissRequest = { showIndex = false }, title = { Text("فهرس المقالة") },
  text = { Column(Modifier.verticalScroll(rememberScrollState())) {
   headings.forEach { (index, title) -> TextButton(onClick = {
    showIndex = false
    scope.launch { scroll.animateScrollTo((offsets[index] ?: 0).coerceIn(0, scroll.maxValue)) }
   }) { Text(title) } }
  } }, confirmButton = { TextButton(onClick = { showIndex = false }) { Text("إغلاق") } })
 if (showAuthor) AlertDialog(onDismissRequest = { showAuthor = false }, title = { Text("كتابات "+a.author) },
  text = { Column(Modifier.verticalScroll(rememberScrollState())) {
   library.articles.filter { it.id in library.available && (if(a.authorId.isNotBlank())it.authorId==a.authorId else it.author==a.author) }.forEach { entry ->
    TextButton(onClick = { showAuthor = false; open(entry.id) }) { Text(entry.title) }
   }
  } }, confirmButton = { TextButton(onClick = { showAuthor = false }) { Text("إغلاق") } })
}
private object MaterialColors { val link = android.graphics.Color.rgb(104, 142, 113) }
@Composable private fun SettingsPage(settings: ReadingSettings, syncedAt: Long, busy: Boolean, change: (ReadingSettings) -> Unit, sync: () -> Unit, clean: () -> Unit, about: () -> Unit, writer: () -> Unit) {
 var confirm by remember { mutableStateOf(false) }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
  Title("على مهلٍ… كما تحب")
  Text("المظهر")
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("system" to "النظام", "light" to "نهاري", "dark" to "ليلي").forEach { (id, label) -> FilterChip(settings.theme == id, { change(settings.copy(theme = id)) }, { Text(label) }) } }
  Text("حجم خط القراءة: ${settings.size.toInt()}")
  Slider(value = settings.size, onValueChange = { change(settings.copy(size = it)) }, valueRange = 16f..34f)
  Text("تباعد السطور")
  Slider(value = settings.spacing, onValueChange = { change(settings.copy(spacing = it)) }, valueRange = 1.3f..2.4f)
  Text("خط القراءة")
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("naskh" to "نسخ", "amiri" to "أميري", "plex" to "بلكس").forEach { (id, label) -> FilterChip(settings.font == id, { change(settings.copy(font = id)) }, { Text(label) }) } }
  Text("قِفا نبكِ من ذكرى حبيبٍ ومنزلِ", fontFamily = when (settings.font) { "amiri" -> Amiri; "plex" -> Plex; else -> Naskh }, fontSize = settings.size.sp, lineHeight = (settings.size * settings.spacing).sp)
  HorizontalDivider()
  Row(verticalAlignment = Alignment.CenterVertically) { Text("تحميل الصور عبر Wi-Fi فقط", Modifier.weight(1f)); Switch(settings.wifiImages, { change(settings.copy(wifiImages = it)) }) }
  Text(if (syncedAt == 0L) "لم تتم مزامنة الشبكة بعد؛ المحتوى التأسيسي متاح." else "آخر مزامنة: " + DateFormat.getDateTimeInstance().format(Date(syncedAt)))
  Button(sync, enabled = !busy) { Icon(Icons.Default.Sync, null); Text("مزامنة المحتوى الآن") }
  Text("يتحقق التطبيق في الخلفية دوريًا عند توفر الاتصال والبطارية المناسبة. قد يؤخر النظام التنفيذ.", style = MaterialTheme.typography.bodyMedium)
  OutlinedButton({ confirm = true }) { Text("تنظيف المحتوى غير المحفوظ والصور المؤقتة") }
  TextButton(about) { Text("حول المدونة والتطبيق") }
  OutlinedButton(onClick = writer) { Icon(Icons.Default.Edit, null); Text("لوحة الكاتب — إدارة المقالات") }
  Text("الجديد في تجربة 3.2: فهرس تفاعلي، تشغيل صوتي داخل المقالة، اكتشاف الكتّاب، وبحث حسب الأبواب.")
  Text("الإصدار ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • Android 8+")
  Title("سياسة الخصوصية")
  Text("تُحفظ المحفوظات وسجل القراءة والإعدادات على جهازك. لا يطلب التطبيق جهات الاتصال أو الموقع. تُرسل طلبات قراءة المحتوى إلى الموقع الرسمي عبر HTTPS، وقد تحتفظ الاستضافة بسجلات الاتصال المعتادة. تُحمّل أغلفة المقالات من روابطها الخارجية وفق إعداد Wi-Fi. لا يضم التطبيق أدوات إعلانات أو تحليلات. لوحة الكاتب تتطلب مصادقة مستقلة وآمنة.", lineHeight = 28.sp)
 }
 if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("تنظيف التخزين المحلي؟") }, text = { Text("ستبقى المقالات المحفوظة والإعدادات وسجل القراءة. يمكن جلب بقية المقالات بالمزامنة مجددًا.") }, confirmButton = { TextButton({ clean(); confirm = false }) { Text("تنظيف") } }, dismissButton = { TextButton({ confirm = false }) { Text("إلغاء") } })
}
@Composable private fun About(about: String) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
  Icon(Icons.Default.AutoStories, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
  Title("مدونة معتز العلقمي")
  Text(about.replace("\\n", "\n"), fontFamily = Naskh, fontSize = 23.sp, lineHeight = 42.sp)
  HorizontalDivider()
  Text("برمجة وتطوير: معتز العلقمي — تعز — اليمن — 2026م", fontFamily = Amiri, fontSize = 23.sp)
  Text("Motaz Alalqami • ${BuildConfig.VERSION_NAME}")
  Text("الخطوط: Amiri، Noto Naskh Arabic، IBM Plex Sans Arabic — مرخصة وفق SIL Open Font License 1.1. ملفات الترخيص مرفقة مع الشفرة.")
 }
}
