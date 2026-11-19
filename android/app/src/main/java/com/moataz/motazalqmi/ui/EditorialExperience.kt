package com.moataz.motazalqmi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.moataz.motazalqmi.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moataz.motazalqmi.data.*

private val ShelfSections = listOf("books", "arabic_lit")

@Composable internal fun EditorialHome(library: Library, settings: ReadingSettings, busy: Boolean,
    sync: () -> Unit, open: (String) -> Unit, section: (String) -> Unit, writer: () -> Unit) {
    val published = remember(library.articles, library.available) { library.articles.filter { it.id in library.available } }
    val lead = published.firstOrNull { it.featured } ?: published.firstOrNull()
    var category by rememberSaveable { mutableStateOf("") }
    var order by rememberSaveable { mutableStateOf("recent") }
    var quoteIndex by rememberSaveable { mutableIntStateOf(0) }
    val articles = remember(published, category, order) { orderArticles(published.filter { category.isEmpty() || it.category == category }, order) }
    val quote = library.quotes.getOrNull(quoteIndex % library.quotes.size.coerceAtLeast(1))
    LazyColumn(Modifier.fillMaxSize().testTag("home-feed"), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(library.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                Text(library.siteName, fontFamily = Amiri, fontSize = 36.sp, lineHeight = 49.sp, textAlign = TextAlign.Center)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("${published.size} مقالات", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        item { SectionDoors(library.sections, open = section) }
        if (lead != null) item {
            Card(onClick = { open(lead.id) }, modifier = Modifier.fillMaxWidth().testTag("featured-article-${lead.id}"),
                shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column {
                    Cover(lead.coverImage, settings)
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        val sectionTitle = library.sections.find { it.id == lead.category }?.title.orEmpty()
                        if (sectionTitle.isNotBlank()) Text(sectionTitle, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
                        Text(lead.title, fontFamily = Amiri, fontSize = 29.sp, lineHeight = 42.sp)
                        if (lead.excerpt.isNotBlank()) Text(lead.excerpt, fontFamily = Naskh, fontSize = 18.sp, lineHeight = 30.sp,
                            maxLines = 4, overflow = TextOverflow.Ellipsis)
                        AuthorIdentity(lead.author, lead.authorImage, lead.publishDate)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("قراءة المقالة", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            MagazineIcon(R.drawable.blog_arrow_left)
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("أحدث الكتابات", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                ArticleOrder(order) { order = it }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(category.isEmpty(), { category = "" }, { Text("الكل") }) }
                items(library.sections.filter { it.id != "about" }, key = { it.id }) { entry ->
                    FilterChip(category == entry.id, { category = entry.id }, { Text(entry.title) })
                }
            }
        }
        if (articles.isEmpty()) item { Empty("لا توجد مقالات في هذا الباب") }
        items(articles, key = { it.id }) { EditorialArticleCard(it, settings, open) }
        if (category.isEmpty()) ShelfSections.forEach { id ->
            val shelf = published.filter { it.category == id }.take(3)
            if (shelf.isNotEmpty()) item(key = "shelf-$id") {
                OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        MagazineIcon(sectionIcon(id))
                        Text(library.sections.find { it.id == id }?.title.orEmpty(), style = MaterialTheme.typography.headlineMedium)
                        shelf.forEach { article ->
                            HorizontalDivider()
                            TextButton(onClick = { open(article.id) }) { Text(article.title, fontFamily = Amiri, fontSize = 21.sp) }
                        }
                        TextButton(onClick = { section(id) }) { Text("جميع الكتابات") }
                    }
                }
            }
        }
        if (quote != null) item {
            OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    QuoteCard(quote, false, {}, interactive = false)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { section("quotes") }) { Text("تصفّح الاقتباسات") }
                        if (library.quotes.size > 1) IconButton(onClick = { quoteIndex = (quoteIndex + 1) % library.quotes.size }) {
                            MagazineIcon(R.drawable.blog_rotate_ccw, "اقتباس آخر")
                        }
                    }
                }
            }
        }
        item {
            TextButton(sync, enabled = !busy) { Text("تحديث المحتوى") }
            MagazineCredits(writer)
        }
    }
}

internal fun orderArticles(articles: List<ArticleDto>, order: String): List<ArticleDto> = when (order) {
    "oldest" -> articles.sortedBy { it.publishDate }
    "title" -> {
        val collator = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("ar"))
        articles.sortedWith { first, second -> collator.compare(first.title, second.title) }
    }
    else -> articles.sortedByDescending { it.publishDate }
}

@Composable internal fun ArticleOrder(order: String, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = mapOf("recent" to "الأحدث أولًا", "oldest" to "الأقدم أولًا", "title" to "حسب العنوان")
    Box {
        TextButton(onClick = { expanded = true }) { Text(options[order].orEmpty()) }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { (id, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; change(id) }) }
        }
    }
}

internal data class ArticleActions(val states: Map<String, ReaderState>, val toggle: (String) -> Unit)
internal val LocalArticleActions = compositionLocalOf<ArticleActions?> { null }

@Composable internal fun EditorialArticleCard(a: ArticleDto, settings: ReadingSettings, open: (String) -> Unit) {
    OutlinedCard(onClick = { open(a.id) }, modifier = Modifier.fillMaxWidth().testTag("article-card-${a.id}"),
        shape = RoundedCornerShape(22.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Cover(a.coverImage, settings)
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthorIdentity(a.author, a.authorImage, a.publishDate)
                Text(a.title, fontFamily = Amiri, fontSize = 24.sp, lineHeight = 35.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (a.excerpt.isNotBlank()) Text(a.excerpt, style = MaterialTheme.typography.bodyMedium, lineHeight = 25.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(a.readTime, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                    ArticleActionsRow(a)
                }
            }
        }
    }
}

@Composable internal fun ArticleActionsRow(article: ArticleDto) {
    val actions = LocalArticleActions.current ?: return
    val context = LocalContext.current
    val liked = actions.states["liked:${article.id}"]?.saved == true
    val saved = actions.states[article.id]?.saved == true
    IconButton(onClick = { actions.toggle("liked:${article.id}") }, modifier = Modifier.semantics { selected = liked }) {
        if (liked) Icon(Icons.Default.Favorite, "أعجبني النص", tint = MaterialTheme.colorScheme.secondary)
        else MagazineIcon(R.drawable.blog_heart, "إعجاب")
    }
    IconButton(onClick = { actions.toggle(article.id) }, modifier = Modifier.semantics { selected = saved }) {
        if (saved) Icon(Icons.Default.Bookmark, "إزالة من المحفوظات") else MagazineIcon(R.drawable.blog_bookmark, "حفظ المقالة")
    }
    IconButton(onClick = { share(context, article.title + "\nhttps://motazalqmi.vercel.app/articles/" + android.net.Uri.encode(article.slug)) }) {
        MagazineIcon(R.drawable.blog_share2, "مشاركة المقالة")
    }
}

@Composable internal fun AuthorIdentity(name: String, image: String, detail: String) {
 Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
  Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(38.dp)) {
   val placeholder: @Composable () -> Unit = { Box(contentAlignment=Alignment.Center){Text(name.take(1).ifBlank{"ك"},fontFamily=Amiri)} }
   if(image.startsWith("https://"))SubcomposeAsyncImage(model=readerImageUrl(image),contentDescription=null,
    modifier=Modifier.fillMaxSize().clip(CircleShape),contentScale=androidx.compose.ui.layout.ContentScale.Crop,
    loading={placeholder()},error={placeholder()})
   else placeholder()
  }
  Column {
   Text(name.ifBlank{"كاتب المقالة"},style=MaterialTheme.typography.labelLarge)
   if(detail.isNotBlank())Text(detail,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
  }
 }
}
