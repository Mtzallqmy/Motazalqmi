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
fun BookmarksScreen(vm: MainViewModel, onArticleClick: (String) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val saved by vm.bookmarkedArticles.collectAsState()
    val unfinished by vm.unfinishedArticles.collectAsState()
    val history by vm.historyArticles.collectAsState()
    val favQuotes by vm.favoriteQuotes.collectAsState()

    val tabs = listOf("المحفوظة", "غير المكتملة", "السجل", "الاقتباسات")

    Scaffold(topBar = { TopAppBar(title = { Text("مكتبتي المحلية") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { idx, label ->
                    Tab(selected = tab == idx, onClick = { tab = idx }, text = { Text(label) })
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    0 -> if (saved.isEmpty()) item { EmptyLiteraryState("لا توجد مقالات محفوظة", "احفظ المقالات لقراءتها لاحقًا دون إنترنت.") }
                         else items(saved, key = { it.id }) { ArticleListCard(it) { onArticleClick(it.slug) } }
                    1 -> if (unfinished.isEmpty()) item { EmptyLiteraryState("لا توجد قراءات غير مكتملة", "ستظهر هنا المقالات التي بدأت قراءتها ولم تُتمّها.") }
                         else items(unfinished, key = { it.id }) { ArticleListCard(it) { onArticleClick(it.slug) } }
                    2 -> if (history.isEmpty()) item { EmptyLiteraryState("سجل القراءة فارغ", "تُحفظ المقالات التي تقرؤها هنا محليًا.") }
                         else items(history, key = { it.id }) { ArticleListCard(it) { onArticleClick(it.slug) } }
                    3 -> if (favQuotes.isEmpty()) item { EmptyLiteraryState("لا توجد اقتباسات مفضلة", "أضف الاقتباسات إلى المفضلة للوصول السريع.") }
                         else items(favQuotes, key = { it.id }) { q ->
                             Card(Modifier.fillMaxWidth()) {
                                 Column(Modifier.padding(16.dp)) {
                                     Text("«${q.text}»", style = MaterialTheme.typography.bodyLarge)
                                     Text("— ${q.author}", style = MaterialTheme.typography.labelMedium)
                                 }
                             }
                         }
                }
            }
        }
    }
}
