package com.moataz.motazalqmi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moataz.motazalqmi.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: MainViewModel, onArticleClick: (String) -> Unit) {
    val query by vm.searchQuery.collectAsState()
    val results by vm.searchResults.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("البحث في المدونة") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.updateSearchQuery(it) },
                label = { Text("ابحث في العناوين، النصوص، الأقسام، أو الكاتب...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (query.isNotBlank() && results.isEmpty()) {
                EmptyLiteraryState("لا توجد نتائج مطابقة", "جرّب كلمات مفتاحية أخرى؛ البحث يدعم تجاهل التشكيل واختلاف الهمزات.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(results, key = { it.id }) { art ->
                        ArticleListCard(article = art, onClick = { onArticleClick(art.slug) })
                    }
                }
            }
        }
    }
}
