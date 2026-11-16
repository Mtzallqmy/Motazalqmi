package com.moataz.motazalqmi.ui

import androidx.compose.foundation.background
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
import com.moataz.motazalqmi.BuildConfig
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
   Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
     Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
      Icon(Icons.Default.AutoStories, null, Modifier.padding(11.dp).size(26.dp), tint = MaterialTheme.colorScheme.primary)
     }
     Spacer(Modifier.weight(1f))
     Text("المجلة الثقافية  •  "+BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
     IconButton(onClick = sync, enabled = !busy) { Icon(Icons.Default.Sync, "تحديث المحتوى", tint = MaterialTheme.colorScheme.primary) }
    }
    Text(library.siteName, fontFamily = Amiri, fontSize = 38.sp, lineHeight = 52.sp, color = MaterialTheme.colorScheme.onBackground)
    Text(library.tagline, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, lineHeight = 26.sp)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
     Text(published.size.toString() + " مقالات", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
     Text(sections.size.toString() + " أبواب", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
     Text("قراءة دون اتصال", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
    }
   }
  }
  if (lead != null) item {
   Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("في دائرة الضوء", fontFamily = Amiri, fontSize = 28.sp, lineHeight = 38.sp)
    Card(onClick = { open(lead.id) }, modifier = Modifier.fillMaxWidth().testTag("featured-article-" + lead.id), shape = RoundedCornerShape(25.dp),
     colors = CardDefaults.cardColors(containerColor = Emerald)) {
     Column {
      Cover(lead.coverImage, settings)
      Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF1C6658), Color(0xFF082B27)))).padding(21.dp),
       verticalArrangement = Arrangement.spacedBy(11.dp)) {
       Text("قراءة مختارة", color = Brass, style = MaterialTheme.typography.labelLarge)
       Text(lead.title, fontFamily = Amiri, fontSize = 30.sp, lineHeight = 43.sp, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
       Text(lead.excerpt, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE1EBE4), maxLines = 2, overflow = TextOverflow.Ellipsis)
       HorizontalDivider(color = Color.White.copy(alpha = .22f))
       Row(verticalAlignment = Alignment.CenterVertically) {
        Text(listOf(lead.author,lead.readTime).filter { it.isNotBlank() }.joinToString("  ·  "), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Brass, maxLines = 1)
        Text("اقرأ ←", style = MaterialTheme.typography.labelLarge, color = Brass)
       }
      }
     }
    }
   }
  }
  item {
   Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("أبواب المجلة", fontFamily = Amiri, fontSize = 28.sp, lineHeight = 38.sp)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
     items(sections, key = { it.id }) { category ->
      ElevatedCard(onClick = { section(category.id) }, modifier = Modifier.width(156.dp), shape = RoundedCornerShape(20.dp)) {
       Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
         Icon(sectionIcon(category.id), null, Modifier.padding(9.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(category.title, fontFamily = Amiri, fontSize = 22.sp, lineHeight = 34.sp, maxLines = 2)
        Text(category.subtitle, style = MaterialTheme.typography.bodySmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
     Text("من روائع البيان", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
     Text(quote.text, fontFamily = Amiri, fontSize = 23.sp, lineHeight = 37.sp)
     Text(quote.author, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
     TextButton(onClick={section("quotes")}) { Text("تصفّح الاقتباسات") }
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
   Cover(a.coverImage, settings)
   Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
    AuthorIdentity(a.author,a.authorImage,a.publishDate)
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

private fun sectionIcon(id: String) = when(id) {
 "general" -> Icons.Default.Edit
 "quotes" -> Icons.Default.FormatQuote
 "books" -> Icons.Default.MenuBook
 "pens" -> Icons.Default.Create
 "news" -> Icons.Default.Public
 "wisdom" -> Icons.Default.Lightbulb
 else -> Icons.Default.AutoStories
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
