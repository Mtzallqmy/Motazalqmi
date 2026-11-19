package com.moataz.motazalqmi.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.moataz.motazalqmi.R
import com.moataz.motazalqmi.data.TickerDto
import kotlinx.coroutines.delay

@Composable internal fun MagazineTicker(ticker: TickerDto, openArticle: (String) -> Boolean, openSection: (String) -> Unit, latest: () -> Unit) {
    if (!ticker.enabled || ticker.items.isEmpty()) return
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var index by remember(ticker) { mutableIntStateOf(0) }
    var paused by rememberSaveable { mutableStateOf(false) }
    val allowMotion = remember(context) { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    LaunchedEffect(ticker, owner, paused, index, allowMotion) {
        if (allowMotion && !paused && ticker.items.size > 1) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(ticker.intervalSeconds.coerceIn(5, 16) * 1000L)
            index = (index + 1) % ticker.items.size
        }
    }
    fun follow(link: String) {
        val destination = Uri.parse(if (link.startsWith('/')) "https://motazalqmi.vercel.app$link" else link)
        if (destination.scheme != "https" || destination.host.isNullOrBlank() || destination.userInfo != null) return
        if (destination.host == "motazalqmi.vercel.app") {
            val parts = destination.pathSegments
            if (parts.size == 2 && parts[0] == "articles" && openArticle(parts[1])) return
            if (parts.size == 2 && parts[0] == "category") { openSection(parts[1]); return }
            if (destination.fragment == "latest" && parts.isEmpty()) { latest(); return }
        }
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, destination)) }
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
            AnimatedContent(targetState = ticker.items[index], label = "announcement") { item ->
                Column(Modifier.fillMaxWidth().then(if (item.link.isNotBlank()) Modifier.clickable { follow(item.link) } else Modifier),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MagazineIcon(R.drawable.blog_megaphone)
                        Text(when (item.kind) { "announcement" -> "إعلان"; "phrase" -> "عبارة مختارة"; else -> "جديد المدونة" },
                            style = MaterialTheme.typography.labelMedium)
                    }
                    Text(item.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (ticker.items.size > 1) Row(Modifier.align(Alignment.End)) {
                IconButton(onClick = { index = (index + ticker.items.size - 1) % ticker.items.size }) { Icon(Icons.Default.ChevronRight, "الإعلان السابق") }
                IconButton(onClick = { paused = !paused }) { Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, if (paused) "تشغيل الشريط" else "إيقاف الشريط") }
                IconButton(onClick = { index = (index + 1) % ticker.items.size }) { Icon(Icons.Default.ChevronLeft, "الإعلان التالي") }
            }
        }
    }
}
