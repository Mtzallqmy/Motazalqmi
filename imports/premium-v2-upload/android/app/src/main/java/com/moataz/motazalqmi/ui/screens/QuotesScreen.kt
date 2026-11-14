package com.moataz.motazalqmi.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moataz.motazalqmi.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesScreen(vm: MainViewModel) {
    val quotes by vm.quotes.collectAsState()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("اقتباسات أدبية") }) }) { padding ->
        if (quotes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyLiteraryState("لا توجد اقتباسات محفوظة بعد", "تُجلب الاقتباسات تلقائيًا من واجهة المدونة عند المزامنة.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quotes, key = { it.id }) { q ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("«${q.text}»", style = MaterialTheme.typography.bodyLarge)
                            Text("— ${q.author}${q.source?.let { " ($it)" }.orEmpty()}", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("quote", "«${q.text}» — ${q.author}"))
                                }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ") }
                                IconButton(onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "«${q.text}» — ${q.author}")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "مشاركة الاقتباس"))
                                }) { Icon(Icons.Outlined.Share, contentDescription = "مشاركة") }
                                IconButton(onClick = { vm.toggleFavoriteQuote(q) }) {
                                    Icon(if (q.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "مفضلة")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
