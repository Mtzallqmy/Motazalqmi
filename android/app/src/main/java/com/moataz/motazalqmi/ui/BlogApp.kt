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
@Composable private fun Home(library: Library, settings: ReadingSettings, busy: Boolean, sync: () -> Unit, open: (String) -> Unit, section: (String) -> Unit) {
 val published = library.articles.filter { it.id in library.available }
 LazyColumn(Modifier.fillMaxSize().testTag("home-feed"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
  item { Row(verticalAlignment = Alignment.CenterVertically) {
   Column(Modifier.weight(1f)) { Text("مكتبة الكلمة والفكرة", color = MaterialTheme.colorScheme.secondary); Title(library.siteName) }
   IconButton(onClick = sync, enabled = !busy) { Icon(Icons.Default.Sync, "تحديث المحتوى") }
  } }
  val lead = published.firstOrNull { it.featured } ?: published.firstOrNull()
  lead?.let { a -> item {
   Card(onClick = { open(a.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
    if (a.coverImage.isNotBlank()) Cover(a.coverImage, settings)
    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
     Text("في دائرة الضوء", style = MaterialTheme.typography.labelLarge)
     Text(a.title, fontFamily = Amiri, fontSize = 32.sp, lineHeight = 46.sp)
     Text(listOf(a.author, a.readTime).filter { it.isNotBlank() }.joinToString("  ·  "), style = MaterialTheme.typography.bodyMedium)
    }
   }
  } }
  val recent = library.states.values.filter { it.progress > 0f && it.progress < 1f }.maxByOrNull { it.openedAt }
  val resume = recent?.let { state -> library.articles.find { it.id == state.id } }
  if (recent != null && resume != null) item {
   OutlinedCard(onClick = { open(resume.id) }, modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
     Text("متابعة القراءة", color = MaterialTheme.colorScheme.primary)
     Text(resume.title, style = MaterialTheme.typography.titleLarge)
     LinearProgressIndicator(progress = { recent.progress }, modifier = Modifier.fillMaxWidth())
    }
   }
  }
  if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
  item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(library.sections, key = { it.id }) { c ->
   AssistChip(onClick = { section(c.id) }, label = { Text(c.title) }, leadingIcon = { Icon(Icons.Default.AutoStories, null, Modifier.size(16.dp)) })
  } } }

  library.quotes.firstOrNull()?.let { q -> item { QuoteCard(q, library.states[quoteId(q)]?.saved == true, {}, false) } }
  item { Title("أحدث الكتابات") }
  if (published.isEmpty()) item { Empty() }
  items(published.filter { it.id != lead?.id }, key = { it.id }) { ArticleCard(it, settings, open) }
 }
}
@Composable private fun ArticleCard(a: ArticleDto, settings: ReadingSettings, open: (String) -> Unit) {
 Card(onClick = { open(a.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
  Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
   Cover(a.coverImage, settings)
   Text(a.publishDate + "  ·  " + a.author, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
   Text(a.title, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Amiri), lineHeight = 36.sp)
   Text(a.excerpt, maxLines = 3, style = MaterialTheme.typography.bodyMedium, lineHeight = 26.sp)
   Text("تابع القراءة  ←", color = MaterialTheme.colorScheme.primary)
  }
 }
}
@Composable private fun Cover(url: String, settings: ReadingSettings) {
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
 var results by remember { mutableStateOf(emptyList<ArticleDto>()) }
 LaunchedEffect(query, library) {
  kotlinx.coroutines.delay(200)
  results = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
   library.articles.filter { a -> a.id in library.available && ArabicSearch.matches(query, a.title, a.content, a.author, a.tags.joinToString(" "), library.sections.find { it.id == a.category }?.title ?: "") }
  }
 }
 Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
  Title("ابحث بين السطور")
  OutlinedTextField(value = query, onValueChange = { query = it.take(120) }, label = { Text("عنوان، مؤلف، كلمة أو قسم") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
  ArticleList(results, settings, "${results.size} من النتائج", open)
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
 val articles = library.articles.filter { a -> val s = library.states[a.id]; when (filter) { 0 -> s?.saved == true; 1 -> (s?.openedAt ?: 0) > 0; else -> (s?.openedAt ?: 0) > 0 && (s?.progress ?: 0f) < .98f } }
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
 var rendered by remember(a.content) { mutableStateOf<android.text.Spanned?>(null) }
 LaunchedEffect(a.content) {
  rendered = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { engine.renderer.render(engine.renderer.parse(a.content)) }
 }
 LaunchedEffect(rendered, a.id) {
  if (rendered != null && !restored) {
   withFrameNanos { }; withFrameNanos { }
   scroll.scrollTo(state?.offset