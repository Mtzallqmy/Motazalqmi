package com.moataz.motazalqmi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moataz.motazalqmi.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    categoryId: String,
    categoryName: String,
    vm: MainViewModel,
    onBack: () -> Unit,
    onArticleClick: (String) -> Unit
) {
    val articles by vm.observeByCategory(categoryId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName, style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        if (articles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyLiteraryState(
                    title = "لا توجد منشورات في «$categoryName» حاليًا",
                    subtitle = "ستظهر المقالات هنا تلقائيًا فور نشرها ومزامنتها مع الموقع."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(articles, key = { it.id }) { art ->
                    ArticleListCard(article = art, onClick = { onArticleClick(art.slug) })
                }
            }
        }
    }
}
