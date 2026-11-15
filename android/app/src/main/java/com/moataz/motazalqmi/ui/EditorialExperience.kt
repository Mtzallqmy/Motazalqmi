package com.moataz.motazalqmi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moataz.motazalqmi.data.*

private val Emerald = Color(0xFF0F423B)
private val Brass = Color(0xFFF0D38D)

@Composable internal fun EditorialHome(library: Library, settings: ReadingSettings, busy: Boolean, sync: () -> Unit, open: (String) -> Unit, section: (String) -> Unit) {
 val published = library.articles.filter { it.id in library.available }
 val lead = published.firstOrNull { it.featured } ?: published.firstOrNull()
 val sections = library.sections.filter { it.id != "about" }
 LazyColumn(Modifier.fillMaxSize().testTag("home-feed"), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(19.dp)) {
  item {
   Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF082B27), Color(0xFF1A685A), Emerald))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
     Surface(shape = CircleShape, color = Color.White.copy(alpha = .14f)) {
      Icon(Icons.Default.AutoStories, null, Modifier.padding(11.dp).size(26.dp), tint = Brass)
     }
     Spacer(Modifier.weight(1f))
     Text("المجلة الثقافية  •  تجربة ٣.١", color = Brass, style = MaterialTheme.typography.labelMedium)
     IconButton(onClick = sync, enabled = !busy) { Icon(Icons.Default.Sync, "تحديث المحتوى", tint = Color.White) }
    }
    Text("مساحة للكلمة التي تبقى", color = Brass, style = MaterialTheme.typography.labelMedium)
    Text(library.siteName, fontFamily = Amiri, fontSize = 38.sp, lineHeight = 52.sp, color = Color.White)
    Text(library.tagline, color = Color(0xFFE1EAE5), fontSize = 16.sp, lineHeight = 26.sp)
    HorizontalDivider(color = Color.White.copy(alpha = .24f))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
     Text(published.size.toString() + " مقالات", color = Brass, style = MaterialTheme.typography.labelMedium)
     Text(sections.size.toString() + " أبواب", color = Brass, style = MaterialTheme.typography.labelMedium)
     Text("قراءة دون اتصال", color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
   }
  }
  item {
   Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("تجوّل بين أبواب الكلمة", fontFamily = Amiri, fontSize = 28.sp, lineHeight = 38.sp)
    Text("الأدب والفكر والتاريخ بين يديك", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
     items(sections, key = { it.id }) { category ->
      ElevatedCard(onClick = { section(category.id) }, modifier = Modifier.width(176.dp), shape = RoundedCornerShape(20.dp)) {
       Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
         Icon(Icons.Default.AutoStories, null, Modifier.padding(9.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(category.title, fontFamily = Amiri, fontSize = 22.sp, maxLines = 1)
        Text(category.subtitle, style = MaterialTheme.typography.bodySmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
       }
      }
     }
    }
   }
  }
  if (lead != null) item {
   Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("في دائرة الضوء", fontFamily = Amiri, fontSize = 28.sp, lineHeight = 38.sp)
    Card(onClick = { open(lead.id) }, modifier = Modifier.fillMaxWidth().testTag("featured-article-" + lead.id), shape = RoundedCornerShape(25.dp),
     colors = CardDefaults.cardColors(containerColor = Emerald)) {
     Column {
      EditorialCover(lead.coverImage, settings)
      Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF1C6658), Color(0xFF082B27)))).padding(21.dp),
       verticalArrangement = Arrangement.spacedBy(11.dp)) {
       Text("✦  قراءة مختارة", color = Brass, style = MaterialTheme.typography.labelLarge)
       Text(lead.title, fontFamily = Amiri, fontSize = 30.sp, lineHeight = 43.sp, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
       Text(lead.excerpt, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE1EBE4), maxLines = 2, overflow = TextOverflow.Ellipsis)
       HorizontalDivider(color = Color.White.copy(alpha = .22f))
       Row(verticalAlignment = Alignment.CenterVertically) {
        Text(lead.author + "  •  " + lead.readTime, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Brass, maxLines = 1)
        Text("اقرأ ←", style = MaterialTheme.typography.labelLarge, color = Brass)
       }
      }
     }
    }
   }
  }
  val recent = library.states.values.filter { it.progress > 0f && it.progress < 1f }.maxByOrNull { it.openedAt }
  val resume = recent?.let { state -> library.articles.find { it.id == state.id } }
  if (recent != null && resume != null) item {
   ElevatedCard(onClick = { open(resume.id) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
     Text("أكمل رحلتك القرائية", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
     Text(resume.title, fontFamily = Amiri, fontSize = 22.sp, lineHeight = 30.sp)
     LinearProgressIndicator(progress = { recent.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
   }
  }
  library.quotes.firstOrNull()?.let { quote -> item {
   Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
    Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
     Text("❖  من روائع البيان", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
     Text(quote.text, fontFamily = Amiri, fontSize = 23.sp, lineHeight = 37.sp)
     Text(quote.author, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
   }
  } }
  item {
   Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text("من جديد المكتبة", fontFamily = Amiri, fontSize = 28.sp, lineHeight = 37.sp)
    Text(if (library.syncedAt > 0L) "المقالات المتاحة من الموقع • " + published.size else "المكتبة المحلية جاهزة؛ يمكنك تحديث المحتوى", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
   }
  }
  if (published.isEmpty()) item { Text("لا توجد مقالات منشورة", Modifier.padding(24.dp)) }
  items(published.filter { it.id != lead?.id }, key = { it.id }) { article -> Box(Modifier.padding(horizontal = 20.dp)) { EditorialArticleCard(article, settings, open) } }
  if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) }
 }
}

@Composable internal fun EditorialArticleCard(a: ArticleDto, settings: ReadingSettings, open: (String) -> Unit) {
 OutlinedCard(onClick = { open(a.id) }, modifier = Modifier.fillMaxWidth().testTag("article-card-" + a.id),
  shape = RoundedCornerShape(22.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
  Column {
   EditorialCover(a.coverImage, settings)
   Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
    Text(a.author + "  •  " + a.publishDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    Text(a.title, fontFamily = Amiri, fontSize = 25.sp, lineHeight = 37.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    Text(a.excerpt, style = MaterialTheme.typography.bodyMedium, lineHeight = 25.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
     Text(a.readTime, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
     Text("اقرأ المقال ←", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
   }
  }
 }
}
