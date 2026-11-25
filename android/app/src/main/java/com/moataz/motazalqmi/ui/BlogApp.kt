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
  if (!handled && deepSlug != null) library.articles.find { it.id in library.available && (it.slug == deepSlug || "r/"+articleShortCode(it.id) == deepSlug) }?.let {
   handled = true; nav.navigate("article/${Uri.encode(it.id)}")
  }
 }
 var menuOpen by rememberSaveable { mutableStateOf(false) }
 val openArticle: (String) -> Unit = { nav.navigate("article/${Uri.encode(it)}") { launchSingleTop = true } }
 val openSection: (String) -> Unit = { nav.navigate("section/${Uri.encode(it)}") { launchSingleTop = true } }
 val openWriter: () -> Unit = { nav.navigate("writer") { launchSingleTop = true } }
 fun openTab(path: String) {
  nav.navigate(path) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
 }
 val actions = remember(library.states, vm) { ArticleActions(library.states, vm::save) }
 CompositionLocalProvider(LocalArticleActions provides actions) {
  Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
   if (!route.startsWith("article") && route != "writer") {
    MagazineHeader({ openTab("home") }, { openTab("search") }, { openTab("saved") }, openWriter, { menuOpen = true })
   }
  }, bottomBar = {
   if (!route.startsWith("article") && route != "writer") NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
    val icons = listOf(R.drawable.blog_house, R.drawable.blog_search, R.drawable.blog_bookmark, R.drawable.blog_settings)
    tabs.forEachIndexed { index, (path, title) ->
     NavigationBarItem(selected = route == path || (path == "search" && route.startsWith("search/")), onClick = { openTab(path) },
      icon = { MagazineIcon(icons[index]) }, label = { Text(title) })
    }
   }
  }) { padding ->
   NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding),
    enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
    composable("home") { EditorialHome(library, settings, busy, vm::sync, openArticle, openSection, openWriter) }
    composable("section/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
     val id = entry.arguments?.getString("id").orEmpty()
     if (id == "about") MagazineAbout(library, openWriter)
     else MagazineSection(id, library, settings, openSection, openArticle, vm::save)
    }
    composable("search") { Search(library, settings, openArticle) }
    composable("search/{term}", arguments = listOf(navArgument("term") { type = NavType.StringType })) { entry ->
     Search(library, settings, openArticle, entry.arguments?.getString("term").orEmpty())
    }
    composable("saved") { Saved(library, settings, vm::save, openArticle) }
    composable("settings") { SettingsPage(settings, library.syncedAt, busy, vm::configure, vm::sync, vm::clean, { openSection("about") }, openWriter) }
    composable("writer") {
     val writer = androidx.hilt.navigation.compose.hiltViewModel<WriterViewModel>()
     WriterPanel(writer, library.sections) { nav.popBackStack() }
    }
    composable("author/{key}", arguments = listOf(navArgument("key") { type = NavType.StringType })) { entry ->
     MagazineAuthor(entry.arguments?.getString("key").orEmpty(), library, settings, openArticle)
    }
    composable("article/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
     val article = library.articles.find { it.id == entry.arguments?.getString("id") }
     if (article == null) Empty("هذا المقال غير متاح محليًا")
     else Reader(article, library, settings, vm, { nav.popBackStack() }, openArticle, openSection,
      { nav.navigate("author/${Uri.encode(it)}") { launchSingleTop = true } },
      { nav.navigate("search/${Uri.encode(it)}") { launchSingleTop = true } })
    }
   }
  }
  if (menuOpen) MagazineMenu(library, back?.arguments?.getString("id")?.let { "section/$it" } ?: route, navigate = { path ->
   menuOpen = false
   if (path.startsWith("section/")) openSection(path.substringAfter('/'))
   else if (path == "writer") openWriter()
   else openTab(path)
  }, dismiss = { menuOpen = false })
 }
}
@Composable private fun Title(text: String) { Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp)) }
@Composable internal fun Empty(text: String = "لم يُنشر محتوى في هذا القسم بعد") {
 Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
  Icon(Icons.Default.MenuBook, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
  Text(text, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyLarge)
 }
}
@Composable private fun ArticleCard(a: ArticleDto, settings: ReadingSettings, open: (String) -> Unit) { EditorialArticleCard(a, settings, open) }
@Composable internal fun Cover(url: String, settings: ReadingSettings) {
 val context = LocalContext.current
 val cm = context.getSystemService(ConnectivityManager::class.java)
 val wifi = cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
 if (url.startsWith("https://")) SubcomposeAsyncImage(model = coil.request.ImageRequest.Builder(context).data(readerImageUrl(url))
  .networkCachePolicy(if (settings.wifiImages && !wifi) coil.request.CachePolicy.DISABLED else coil.request.CachePolicy.ENABLED)
  .diskCachePolicy(coil.request.CachePolicy.ENABLED).build(),
  contentDescription = "غلاف المقالة", modifier = Modifier.fillMaxWidth().aspectRatio(1.65f).clip(RoundedCornerShape(14.dp)),
  contentScale = androidx.compose.ui.layout.ContentScale.Crop,
  loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(30.dp)) } },
  error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MagazineIcon(R.drawable.blog_book_open, "الغلاف غير متاح حاليًا") } })
}
@Composable private fun ArticleList(articles: List<ArticleDto>, settings: ReadingSettings, title: String, open: (String) -> Unit) {
 LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
  item { Title(title) }; if (articles.isEmpty()) item { Empty() }
  items(articles, key = { it.id }) { ArticleCard(it, settings, open) }
 }
}
@Composable private fun Search(library: Library, settings: ReadingSettings, open: (String) -> Unit, initialQuery: String = "") {
 var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
 var category by rememberSaveable { mutableStateOf("") }
 var order by rememberSaveable { mutableStateOf("recent") }
 var results by remember { mutableStateOf(emptyList<ArticleDto>()) }
 LaunchedEffect(query, category, order, library.articles, library.available, library.sections) {
  kotlinx.coroutines.delay(180)
  results = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
   val found = library.articles.filter { a -> a.id in library.available && (category.isEmpty() || a.category == category) &&
    ArabicSearch.matches(query, a.title, a.content, a.excerpt, a.author, a.tags.joinToString(" "), library.sections.find { it.id == a.category }?.title.orEmpty()) }
   orderArticles(found, order)
  }
 }
 Column(Modifier.fillMaxSize()) {
  Text("البحث في المدونة",Modifier.padding(horizontal=20.dp,vertical=16.dp),style=MaterialTheme.typography.headlineMedium)
  OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal=20.dp),
   label = { Text("كلمة، عنوان، أو اسم كاتب") }, singleLine = true,
   trailingIcon = { if(query.isNotEmpty())IconButton(onClick={query=""}){Icon(Icons.Default.Close,"مسح البحث")} })
  LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   item { FilterChip(category.isEmpty(),{category=""},{Text("كل الأبواب")}) }
   items(library.sections.filter { it.id!="about" },key={it.id}) { item ->
    FilterChip(category==item.id,{category=item.id},{Text(item.title)})
   }
  }
  ArticleOrder(order) { order = it }
  Box(Modifier.weight(1f)) { ArticleList(results, settings, "${results.size} من النتائج", open) }
 }
}
@Composable internal fun QuoteCard(q: QuoteDto, saved: Boolean, save: () -> Unit, interactive: Boolean = true) {
 val clipboard = LocalClipboardManager.current; val context = LocalContext.current
 Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
  Column(Modifier.padding(24.dp)) {
   MagazineIcon(R.drawable.blog_quote)
   SelectionContainer { Text(q.text, Modifier.padding(vertical = 16.dp), fontFamily = Amiri, fontSize = 25.sp, lineHeight = 43.sp) }
   Text(listOf(q.author, q.source).filter { it.isNotBlank() }.joinToString(" · "), fontFamily = Plex)
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
 var query by rememberSaveable { mutableStateOf("") }
 val articles = library.articles.filter { a -> val s = library.states[a.id]; when (filter) { 0 -> s?.saved == true; 1 -> (s?.openedAt ?: 0) > 0; else -> (s?.openedAt ?: 0) > 0 && (s?.progress ?: 0f) < .98f } }.filter { ArabicSearch.matches(query, it.title, it.author) }.sortedByDescending { library.states[it.id]?.openedAt ?: 0L }
 Column(Modifier.fillMaxSize()) {
  Column(Modifier.padding(horizontal = 18.dp)) {
   Text("مكتبتك", style = MaterialTheme.typography.headlineMedium)
   OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
    label = { Text("بحث في المحفوظات") }, leadingIcon = { MagazineIcon(R.drawable.blog_search) })
  }
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   listOf("المحفوظات", "سجل القراءة", "أكمل القراءة").forEachIndexed { i, label -> FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(label) }) }
  }
  LazyColumn(modifier = Modifier.testTag("saved-feed"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
   if (articles.isEmpty()) item { Empty("لا توجد مقالات هنا بعد") }
   items(articles, key = { it.id }) { ArticleCard(it, settings, open) }
   if (filter == 0) items(library.quotes.filter { library.states[quoteId(it)]?.saved == true && ArabicSearch.matches(query, it.text, it.author) }, key = { quoteId(it) }) { q -> QuoteCard(q, true, { save(quoteId(q)) }) }
  }
 }
}
internal fun share(context: android.content.Context, text: String) {
 context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "مشاركة"))
}

@OptIn(FlowPreview::class)
@Composable private fun Reader(a: ArticleDto, library: Library, settings: ReadingSettings, vm: BlogViewModel, back: () -> Unit, open: (String) -> Unit, section: (String) -> Unit, author: (String) -> Unit, search: (String) -> Unit) {
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
 val profile = library.authors.firstOrNull { it.id == a.authorId }
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
 val chosenFont = if (settings.font == "editor") a.fontFamily else settings.font
 val fontId = when (chosenFont) { "amiri" -> R.font.amiri; "plex", "tajawal" -> R.font.plex; else -> R.font.naskh }
 val size = if (settings.useArticleSize) a.fontSize.toFloat().coerceIn(16f, 34f) else settings.size
 val maxWidth = when (settings.textWidth) { "narrow" -> 470.dp; "wide" -> 800.dp; else -> 640.dp }
 val gutter = when (settings.textWidth) { "narrow" -> 35.dp; "wide" -> 16.dp; else -> 24.dp }
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
    if (headings.isNotEmpty()) IconButton(onClick = { showIndex = true }) { MagazineIcon(R.drawable.blog_list, "فهرس المقالة") }
    IconButton(onClick = { showSettings = true }) { MagazineIcon(R.drawable.blog_sliders_horizontal, "إعدادات القراءة") }
    IconButton(onClick = { vm.save(a.id) }) { Icon(if (state?.saved == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "حفظ للقراءة لاحقًا") }
    IconButton(onClick = { share(context, a.title + "\n" + articleShareUrl(a.id)) }) { MagazineIcon(R.drawable.blog_share2, "مشاركة المقال") }
   })
  }
  LinearProgressIndicator(progress = { if (scroll.maxValue > 0 && scroll.maxValue < Int.MAX_VALUE) scroll.value.toFloat() / scroll.maxValue else 0f }, modifier = Modifier.fillMaxWidth())
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
   TextButton(onClick = { focus = !focus }) { Text(if (focus) "إظهار الأدوات" else "قراءة مركزة") }
   if (!focus) {
    IconButton(onClick = { vm.configure(settings.copy(size = (size - 1).coerceAtLeast(16f), useArticleSize = false)) }) { Icon(Icons.Default.TextDecrease, "تصغير الخط") }
    IconButton(onClick = { vm.configure(settings.copy(size = (size + 1).coerceAtMost(34f), useArticleSize = false)) }) { Icon(Icons.Default.TextIncrease, "تكبير الخط") }
    IconButton(onClick = { vm.configure(settings.copy(theme = if (settings.theme == "dark") "light" else "dark")) }) { Icon(Icons.Default.DarkMode, "تبديل وضع القراءة الليلي") }
   }
  }
  Column(Modifier.weight(1f).align(Alignment.CenterHorizontally).widthIn(max = maxWidth).fillMaxWidth().verticalScroll(scroll).padding(horizontal = gutter, vertical = 16.dp)) {
   if (a.id !in library.available) Text("نسخة محفوظة؛ هذا المقال لم يعد منشورًا على الموقع.", color = MaterialTheme.colorScheme.secondary)
   TextButton(onClick = { section(a.category) }) { MagazineIcon(sectionIcon(a.category)); Spacer(Modifier.width(7.dp)); Text(library.sections.find { it.id == a.category }?.title.orEmpty()) }
   Text(a.title, Modifier.padding(vertical = 18.dp), fontFamily = Amiri, fontSize = 32.sp, lineHeight = 48.sp)
   val minutes = (a.content.split(Regex("\\s+")).size / 180).coerceAtLeast(1)
   Box(Modifier.clickable(enabled = a.author.isNotBlank() || a.authorId.isNotBlank()) {
    author(a.authorId.ifBlank { a.author })
   }.testTag("reader-author").padding(vertical = 6.dp)) {
    AuthorIdentity(profile?.name ?: a.author, profile?.image?.takeIf { it.isNotBlank() } ?: a.authorImage, "${a.publishDate} · $minutes دقائق قراءة")
   }
   Spacer(Modifier.height(14.dp)); Cover(a.coverImage, settings)
   if (a.excerpt.isNotBlank()) Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
    Text(a.excerpt, Modifier.padding(16.dp), fontFamily = Naskh, fontSize = 21.sp, lineHeight = 36.sp)
   }
   if (a.category == HEALTH_SECTION && HEALTH_NOTICE !in a.content) HealthNotice()
   if (!focus) ArticleActionsRow(a)
   Spacer(Modifier.height(14.dp))
   if (rendered == null) CircularProgressIndicator()
   // Render image blocks at the exact position they appear in the article.
   // The original TextView-only Markwon renderer silently omitted Markdown image bytes.
   parts.forEachIndexed { index, part ->
    key(index) {
     Column(Modifier.fillMaxWidth().onGloballyPositioned { offsets[index] = it.positionInParent().y.toInt() }) {
     if(part.videoUrl!=null){
      Text(part.label.ifBlank{"فيديو مرفق"},style=MaterialTheme.typography.titleSmall)
      ReaderVideo(part.videoUrl)
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
      val spaced = remember(parsed, settings.paragraphSpacing) { spaceReaderParagraphs(parsed, settings.paragraphSpacing) }
      AndroidView(factory = { TextView(it).apply {
       textDirection = android.view.View.TEXT_DIRECTION_FIRST_STRONG_RTL
       layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
       setTextIsSelectable(true)
       movementMethod = android.text.method.LinkMovementMethod.getInstance()
      } }, modifier = Modifier.fillMaxWidth(), update = { view ->
       view.setTextColor(ink); view.setLinkTextColor(MaterialColors.link)
       view.typeface = ResourcesCompat.getFont(context, fontId) ?: Typeface.DEFAULT
       view.textSize = size
       view.gravity = if (settings.alignment == "center") android.view.Gravity.CENTER_HORIZONTAL else android.view.Gravity.START
       view.applyReaderJustification(settings.alignment == "justify")
       view.setLineSpacing(0f, settings.spacing)
       if (view.tag !== spaced) { engine.renderer.setParsedMarkdown(view, spaced); view.tag = spaced }
      })
     }
    }
   }
   }
   if (!focus && a.tags.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
    items(a.tags.distinct()) { tag -> AssistChip(onClick = { search(tag) }, label = { Text("#$tag") }) }
   }
   Spacer(Modifier.height(30.dp))
   val published = library.articles.filter { it.id in library.available }
   val candidates = published.filter { it.id != a.id }
   val related = candidates.filter { it.category == a.category || it.tags.any(a.tags::contains) }.ifEmpty { candidates }.take(2)
   if (!focus && related.isNotEmpty()) {
    Text("مقالات أخرى قد تروق لك", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
    related.forEach { entry ->
     OutlinedCard(onClick = { open(entry.id) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
      Text(entry.title, Modifier.padding(14.dp), fontFamily = Amiri, fontSize = 21.sp)
     }
    }
   }
   val index = published.indexOfFirst { it.id == a.id }
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    published.getOrNull(index - 1)?.let { TextButton(onClick = { open(it.id) }) { Text("المقال السابق") } }
    if (index >= 0) published.getOrNull(index + 1)?.let { TextButton(onClick = { open(it.id) }) { Text("المقال التالي") } }
   }
   HorizontalDivider(); Text("برمجة وتطوير: معتز العلقمي — تعز — اليمن — 2026م", Modifier.padding(vertical = 24.dp), style = MaterialTheme.typography.bodyMedium)
  }
 }
 if (showSettings) ReaderSettingsSheet(settings, vm::configure, focus, { focus = it }) { showSettings = false }
 if (showIndex) AlertDialog(onDismissRequest = { showIndex = false }, title = { Text("فهرس المقالة") },
  text = { Column(Modifier.verticalScroll(rememberScrollState())) {
   headings.forEach { (index, title) -> TextButton(onClick = {
    showIndex = false
    scope.launch { scroll.animateScrollTo((offsets[index] ?: 0).coerceIn(0, scroll.maxValue)) }
   }) { Text(title) } }
  } }, confirmButton = { TextButton(onClick = { showIndex = false }) { Text("إغلاق") } })

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
  OutlinedButton(onClick = writer) { Icon(Icons.Default.Edit, null); Text("لوحة الكاتب — المقالات والكتّاب والوسائط") }
  Text("الجديد في تجربة 3.4: أبواب المجلة، ملفات الكتّاب، وفهرس الكتابات والمحفوظات بتصميم متناسق مع الموقع.")
  Text("الإصدار ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • Android 8+")
  Title("سياسة الخصوصية")
  Text("تُحفظ المحفوظات وسجل القراءة والإعدادات على جهازك. لا يطلب التطبيق جهات الاتصال أو الموقع. تُرسل طلبات قراءة المحتوى إلى الموقع الرسمي عبر HTTPS، وقد تحتفظ الاستضافة بسجلات الاتصال المعتادة. تُحمّل أغلفة المقالات من روابطها الخارجية وفق إعداد Wi-Fi. لا يضم التطبيق أدوات إعلانات أو تحليلات. لوحة الكاتب تتطلب مصادقة مستقلة وآمنة.", lineHeight = 28.sp)
 }
 if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("تنظيف التخزين المحلي؟") }, text = { Text("ستبقى المقالات المحفوظة والإعدادات وسجل القراءة. يمكن جلب بقية المقالات بالمزامنة مجددًا.") }, confirmButton = { TextButton({ clean(); confirm = false }) { Text("تنظيف") } }, dismissButton = { TextButton({ confirm = false }) { Text("إلغاء") } })
}
