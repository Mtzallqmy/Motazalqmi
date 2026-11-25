package com.moataz.motazalqmi.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moataz.motazalqmi.R
import com.moataz.motazalqmi.data.*

@Composable internal fun SectionDoors(sections: List<SectionDto>, selected: String = "", open: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sections.filter { it.id != "about" }, key = { it.id }) { section ->
            FilterChip(selected == section.id, { open(section.id) }, { Text(section.title) },
                leadingIcon = { MagazineIcon(sectionIcon(section.id)) })
        }
    }
}

@Composable internal fun PageBanner(title: String, subtitle: String, icon: Int, detail: String = "") {
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MagazineIcon(icon)
            Text(title, fontFamily = Amiri, fontSize = 30.sp, lineHeight = 44.sp)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyLarge)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable internal fun MagazineSection(id: String, library: Library, settings: ReadingSettings,
    section: (String) -> Unit, open: (String) -> Unit, save: (String) -> Unit) {
    val info = library.sections.firstOrNull { it.id == id }
    var topic by rememberSaveable(id) { mutableStateOf("") }
    val articles = library.articles.filter { it.id in library.available && it.category == id && (topic.isEmpty() || topic in it.tags) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageBanner(info?.title ?: "القسم غير موجود", info?.subtitle.orEmpty(), sectionIcon(id), "${articles.size} مادة منشورة") }
        item { SectionDoors(library.sections, id, section) }
        if (id == HEALTH_SECTION) {
            item { HealthNotice() }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(topic.isEmpty(), { topic = "" }, { Text("جميع المقالات") }) }
                    items(HealthTopics) { name -> FilterChip(topic == name, { topic = name }, { Text(name) }) }
                }
            }
        }
        if (id == "quotes") items(library.quotes, key = { quoteId(it) }) { quote ->
            val key = quoteId(quote)
            QuoteCard(quote, library.states[key]?.saved == true, { save(key) })
        }
        if (articles.isEmpty() && (id != "quotes" || library.quotes.isEmpty())) item { Empty() }
        items(articles, key = { it.id }) { EditorialArticleCard(it, settings, open) }
    }
}

@Composable internal fun MagazineAuthor(key: String, library: Library, settings: ReadingSettings, open: (String) -> Unit) {
    val profile = library.authors.find { it.id == key || it.name == key }
    val name = profile?.name ?: library.articles.firstOrNull { it.authorId == key }?.author ?: key
    val articles = library.articles.filter { article -> article.id in library.available &&
        if (article.authorId.isNotBlank()) article.authorId == (profile?.id ?: key) else article.author == name }
    val context = LocalContext.current
    val link = profile?.link?.takeIf { it.startsWith("https://") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AuthorIdentity(name, profile?.image?.takeIf { it.isNotBlank() } ?: articles.firstOrNull()?.authorImage.orEmpty(), "ملف الكاتب")
                    profile?.bio?.takeIf { it.isNotBlank() }?.let { Text(it, fontFamily = Naskh, fontSize = 21.sp, lineHeight = 37.sp) }
                    if (link != null) OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) } }) {
                        MagazineIcon(R.drawable.blog_link2)
                        Spacer(Modifier.width(8.dp)); Text("موقع الكاتب")
                    }
                }
            }
        }
        item { Text("كتابات $name (${articles.size})", style = MaterialTheme.typography.headlineMedium) }
        if (articles.isEmpty()) item { Empty("لا توجد كتابات منشورة لهذا الكاتب") }
        items(articles, key = { it.id }) { EditorialArticleCard(it, settings, open) }
    }
}

@Composable internal fun MagazineAbout(library: Library, writer: () -> Unit) {
    val pillars = listOf(
        Triple(R.drawable.blog_landmark, "أصالة التراث العربي", "استكشاف روائع الشعر وكتب البيان وسير الأدباء والحكماء."),
        Triple(R.drawable.blog_feather, "إبداع القلم والفكر", "مقالات وتأملات في قضايا الإنسان والثقافة."),
        Triple(R.drawable.blog_book_open, "صحبة الكتاب", "قراءات ومراجعات من بطون الكتب القديمة والحديثة."),
        Triple(R.drawable.blog_health, "الصحة والدواء", "أدلة توعوية بمراجع منشورة عن سلامة الدواء وأسئلة الصيدلي.")
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageBanner("حول مدونة معتز العلقمي", library.tagline, R.drawable.blog_user_check) }
        item {
            AuthorIdentity("معتز العلقمي", "", "صاحب المدونة الثقافية")
            Text("رسالة المدونة ورؤيتها", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
            Text(library.about, fontFamily = Naskh, fontSize = 21.sp, lineHeight = 38.sp)
        }
        items(pillars, key = { it.second }) { (icon, title, text) ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MagazineIcon(icon)
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item { MagazineCredits(writer) }
        item { Text("الخطوط العربية مرخّصة وفق SIL OFL 1.1. أيقونات Lucide مرخّصة وفق ISC.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable internal fun MagazineCredits(writer: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("معتز العلقمي", fontFamily = Amiri, fontSize = 27.sp)
            Text("برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = writer) { MagazineIcon(R.drawable.blog_feather); Spacer(Modifier.width(8.dp)); Text("الدخول إلى لوحة الكاتب") }
        }
    }
}
