package com.moataz.motazalqmi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moataz.motazalqmi.domain.model.Article
import com.moataz.motazalqmi.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onArticleClick: (String) -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onSearchClick: () -> Unit
) {
    val categories by vm.categories.collectAsState()
    val featured by vm.featuredArticle.collectAsState()
    val articles by vm.allArticles.collectAsState()
    val quotes by vm.quotes.collectAsState()
    val isSyncing by vm.isSyncing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("مدونة معتز العلقمي", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "للغة والأدب والتاريخ والفكر والشعر العربي",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Outlined.Search, contentDescription = "بحث")
                    }
                    IconButton(onClick = { vm.syncNow() }) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = "مزامنة")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Literary greeting
            item {
                Text(
                    text = "طابَ يومُك بالقراءةِ والمعرفة",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Category chips (All 8 official sections)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        AssistChip(
                            onClick = { onCategoryClick(cat.id, cat.nameAr) },
                            label = { Text(cat.nameAr) }
                        )
                    }
                }
            }

            // Featured Article
            featured?.let { art ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onArticleClick(art.slug) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("المقالة المميزة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            Text(art.title, style = MaterialTheme.typography.headlineMedium)
                            Text(art.excerpt, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text("${art.authorName} • ${art.readingTimeMinutes} د قراءة", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Daily Quote
            quotes.firstOrNull()?.let { q ->
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("اقتباس مختار", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            Text("«${q.text}»", style = MaterialTheme.typography.bodyLarge)
                            Text("— ${q.author}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Latest Articles
            item {
                Text("أحدث المقالات", style = MaterialTheme.typography.headlineSmall)
            }

            if (articles.isEmpty()) {
                item {
                    EmptyLiteraryState(
                        title = "لا توجد مقالات مخزّنة محليًا بعد",
                        subtitle = "اضغط على زر المزامنة لجلب أحدث المقالات من مدونة معتز العلقمي."
                    )
                }
            } else {
                items(articles, key = { it.id }) { article ->
                    ArticleListCard(article = article, onClick = { onArticleClick(article.slug) })
                }
            }
        }
    }
}

@Composable
fun ArticleListCard(article: Article, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(article.categoryName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(article.title, style = MaterialTheme.typography.headlineSmall)
            if (article.excerpt.isNotBlank()) {
                Text(article.excerpt, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${article.authorName} • ${article.publishedAt.take(10)} • ${article.readingTimeMinutes} د",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyLiteraryState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
