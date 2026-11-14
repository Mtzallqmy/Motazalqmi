@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.moataz.motazalqmi.ui

import android.content.Intent
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.TextView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.moataz.motazalqmi.R
import com.moataz.motazalqmi.BuildConfig
import com.moataz.motazalqmi.data.*
import com.moataz.motazalqmi.domain.ArabicSearch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview
import java.text.DateFormat
import java.util.Date

private val tabs = listOf("home" to "الرئيسية", "search" to "البحث", "saved" to "مكتبتي", "settings" to "الإعدادات")
private fun quoteId(q: QuoteDto) = "quote:" + java.security.MessageDigest.getInstance("SHA-256").digest(q.text.toByteArray()).joinToString("") { "%02x".format(it) }
@Composable fun BlogApp(vm: BlogViewModel, deepSlug: String?) {
 val library by vm.library.collectAsStateWithLifecycle()
 val settings by vm.settings.collectAsStateWithLifecycle()
 val busy by vm.busy.collectAsStateWithLifecycle()
 val notice by vm.notice.collectAsStateWithLifecycle()
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
  if (!route.startsWith("article")) NavigationBar {
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
     "quotes" -> Quotes(library, vm::save)
     else -> ArticleList(library.articles.filter { it.id in library.available && it.category == id }, settings,
      library.sections.find { it.id == id }?.title ?: "المقالات", { nav.navigate("article/${Uri.encode(it)}") })
    }
   }
   composable("search") { Search(library, settings) { nav.navigate("article/${Uri.encode(it)}") } }
   composable("saved") { Saved(library, settings, vm::save) { nav.navigate("article/${Uri.encode(it)}") } }
   composable("settings") { SettingsPage(settings, library.syncedAt, busy, vm::configure, vm::sync, vm::clean, { nav.navigate("section/about") }) }
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
 LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
  item { Row(verticalAlignment = Alignment.CenterVertically) {
   Column(Modifier.weight(1f)) { Text("مكتبة الكلمة والفكرة", color = MaterialTheme.colorScheme.secondary); Title("مدونة معتز العلقمي") }
   IconButton(onClick = sync, enabled = !busy) { Icon(Icons.Default.Sync, "تحديث المحتوى") }
  } }
  item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(24.dp)) {
   Column(Modifier.padding(26.dp)) { Text("أهلًا بك في رحاب الكلمة", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
    Text("حيث تلتقي الكلمة بالفكر، ويزهر الأدب", Modifier.padding(top = 12.dp), fontFamily = Amiri, fontSize = 23.sp, color = MaterialTheme.colorScheme.onPrimary) }
  } }
  if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
  item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(library.sections, key = { it.id }) { c ->
   AssistChip(onClick = { section(c.id) }, label = { Text(c.title) }, leadingIcon = { Icon(Icons.Default.AutoStories, null, Modifier.size(16.dp)) })
  } } }
  published.firstOrNull { it.featured }?.let { a -> item { Title("في دائرة الضوء"); ArticleCard(a, settings, open) } }
  library.quotes.firstOrNull()?.let { q -> item { QuoteCard(q, library.states[quoteId(q)]?.saved == true, {}, false) } }
  item { Title("أحدث الكتابات") }
  if (published.isEmpty()) item { Empty() }
  items(published, key = { it.id }) { ArticleCard(it, settings, open) }
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
 if (url.startsWith("https://") && (!settings.wifiImages || wifi)) AsyncImage(model = url, contentDescription = "صورة غلاف المقال", modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
}
@Composable private fun ArticleList(articles: List<ArticleDto>, settings: ReadingSettings, title: String, open: (String) -> Unit) {
 LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
  item { Title(title) }; if (articles.isEmpty()) item { Empty() }
  items(articles, key = { it.id }) { ArticleCard(it, settings, open) }
 }
}
@Composable private fun Search(library: Library, settings: ReadingSettings, open: (String) -> Unit) {
 var query by rememberSaveable { mutableStateOf("") }
 val results = remember(query, library) { library.articles.filter { a -> a.id in library.available && ArabicSearch.matches(query, a.title, a.content, a.author, a.tags.joinToString(" "), library.sections.find { it.id == a.category }?.title ?: "") } }
 Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
  Title("ابحث بين السطور")
  OutlinedTextField(value = query, onValueChange = { query = it.take(120) }, label = { Text("عنوان، مؤلف، كلمة أو قسم") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
  ArticleList(results, settings, "${results.size} من النتائج", open)
 }
}
@Composable private fun Quotes(library: Library, save: (String) -> Unit) {
 LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
  item { Title("اقتباسات") }; if (library.quotes.isEmpty()) item { Empty() }
  items(library.quotes, key = { quoteId(it) }) { QuoteCard(it, library.states[quoteId(it)]?.saved == true, { save(quoteId(it)) }) }
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
  LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
 val engine = remember { MarkdownEngine(context) }
 val rendered by produceState<android.text.Spanned?>(null, a.content) {
  value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { engine.renderer.render(engine.renderer.parse(a.content)) }
 }
 val ink = MaterialTheme.colorScheme.onBackground.toArgb()
 val fontId = when (settings.font) { "amiri" -> R.font.amiri; "plex" -> R.font.plex; else -> R.font.naskh }
 LaunchedEffect(a.id) {
  snapshotFlow { scroll.value to scroll.maxValue }.debounce(500).collect { (offset, max) ->
   if (max > 0 && max < Int.MAX_VALUE) vm.position(a.id, 0, offset, offset.toFloat() / max)
  }
 }
 Column(Modifier.fillMaxSize()) {
  AnimatedVisibility(!focus) {
   TopAppBar(title = { Text("رحاب القراءة", fontFamily = Amiri) }, navigationIcon = {
    IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
   }, actions = {
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
   Text("${a.author}  ·  ${a.publishDate}  ·  $minutes دقائق قراءة", style = MaterialTheme.typography.bodyMedium)
   Spacer(Modifier.height(20.dp)); Cover(a.coverImage, settings); Spacer(Modifier.height(22.dp))
   if (rendered == null) CircularProgressIndicator()
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
    if (rendered != null && view.tag !== rendered) { engine.renderer.setParsedMarkdown(view, rendered!!); view.tag = rendered }
   })
   Spacer(Modifier.height(30.dp))
   val published = library.articles.filter { it.id in library.available }; val index = published.indexOfFirst { it.id == a.id }
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    published.getOrNull(index - 1)?.let { TextButton(onClick = { open(it.id) }) { Text("المقال السابق") } }
    if (index >= 0) published.getOrNull(index + 1)?.let { TextButton(onClick = { open(it.id) }) { Text("المقال التالي") } }
   }
   HorizontalDivider(); Text("برمجة وتطوير: معتز العلقمي — تعز — اليمن — 2026م", Modifier.padding(vertical = 24.dp), style = MaterialTheme.typography.bodyMedium)
  }
 }
}
private object MaterialColors { val link = android.graphics.Color.rgb(104, 142, 113) }
@Composable private fun SettingsPage(settings: ReadingSettings, syncedAt: Long, busy: Boolean, change: (ReadingSettings) -> Unit, sync: () -> Unit, clean: () -> Unit, about: () -> Unit) {
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
  Text("الإصدار ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • Android 8+")
  Title("سياسة الخصوصية")
  Text("تُحفظ المحفوظات وسجل القراءة والإعدادات على جهازك. لا يطلب التطبيق جهات الاتصال أو الموقع. تُرسل طلبات قراءة المحتوى إلى الموقع الرسمي عبر HTTPS، وقد تحتفظ الاستضافة بسجلات الاتصال المعتادة. تُحمّل أغلفة المقالات من روابطها الخارجية وفق إعداد Wi-Fi. لا يضم التطبيق أدوات إعلانات أو تحليلات أو تسجيل دخول للكاتب.", lineHeight = 28.sp)
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
