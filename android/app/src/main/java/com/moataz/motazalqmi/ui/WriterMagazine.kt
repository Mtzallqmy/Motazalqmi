package com.moataz.motazalqmi.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moataz.motazalqmi.R
import com.moataz.motazalqmi.data.QuoteDto

data class StudioSite(val tagline: String = "", val about: String = "")
data class StudioEditorial(val site: StudioSite = StudioSite(), val quotes: List<QuoteDto> = emptyList())
data class StudioEditorialFields(val tagline: String, val about: String, val quotes: List<QuoteDto>)
data class StudioEditorialRequest(val action: String = "editorial", val editorial: StudioEditorialFields)
data class StudioTelegramRequest(val action: String)
data class StudioTelegramBot(val username: String = "")
data class StudioTelegramWebhook(val active: Boolean = false, val hasRecentError: Boolean = false, val pendingUpdates: Int = 0)
data class StudioTelegramStatus(val configured: Boolean = false, val linkedAccounts: Int = 0,
    val bot: StudioTelegramBot? = null, val webhook: StudioTelegramWebhook? = null)

@Composable internal fun WriterEditorial(editorial: StudioEditorial?, busy: Boolean, reload: () -> Unit, save: (StudioEditorialFields) -> Unit) {
    if (editorial == null) {
        TextButton(onClick = reload, enabled = !busy) { Text("تحميل إعدادات المدونة") }
        return
    }
    var tagline by rememberSaveable(editorial.site.tagline) { mutableStateOf(editorial.site.tagline) }
    var about by rememberSaveable(editorial.site.about) { mutableStateOf(editorial.site.about) }
    val quotes = remember(editorial.quotes) { mutableStateListOf<QuoteDto>().apply { addAll(editorial.quotes) } }
    var confirm by rememberSaveable { mutableStateOf(false) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Text("هوية المدونة والاقتباسات", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(tagline, { tagline = it.take(240) }, Modifier.fillMaxWidth(), label = { Text("وصف المدونة") })
            OutlinedTextField(about, { about = it.take(12000) }, Modifier.fillMaxWidth(), label = { Text("نبذة المدونة") }, minLines = 4)
        }
        itemsIndexed(quotes) { index, quote ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(quote.text, { quotes[index] = quote.copy(text = it.take(1200)) }, Modifier.fillMaxWidth(), label = { Text("الاقتباس") })
                    OutlinedTextField(quote.author, { quotes[index] = quote.copy(author = it.take(160)) }, Modifier.fillMaxWidth(), label = { Text("القائل") })
                    OutlinedTextField(quote.source, { quotes[index] = quote.copy(source = it.take(160)) }, Modifier.fillMaxWidth(), label = { Text("المصدر") })
                    TextButton(onClick = { quotes.removeAt(index) }) { Text("إزالة الاقتباس") }
                }
            }
        }
        item {
            OutlinedButton(onClick = { quotes.add(QuoteDto()) }, enabled = !busy && quotes.size < 40) { Text("إضافة اقتباس") }
            Button(onClick = { confirm = true }, enabled = !busy && tagline.trim().length >= 3 && about.trim().length >= 10 && quotes.all { it.text.isNotBlank() }) {
                Text("حفظ إعدادات المدونة")
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("حفظ التغييرات على المدونة؟") },
        text = { Text("تظهر الهوية والاقتباسات المحدّثة للقرّاء عند المزامنة التالية.") },
        confirmButton = { TextButton(onClick = { confirm = false; save(StudioEditorialFields(tagline, about, quotes.toList())) }) { Text("تأكيد الحفظ") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("إلغاء") } })
}

@Composable internal fun WriterTelegram(status: StudioTelegramStatus?, busy: Boolean, check: (String) -> Unit) {
    val context = LocalContext.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            PageBanner("إدارة Telegram", "حالة البوت وحسابات الإدارة المرتبطة", R.drawable.blog_link2)
        }
        if (status != null) item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (status.configured) "بيانات البوت مهيّأة" else "بانتظار إعداد البوت", style = MaterialTheme.typography.titleMedium)
                    status.bot?.username?.takeIf(String::isNotBlank)?.let { Text("@$it") }
                    status.webhook?.let {
                        Text(if (it.active) "Webhook متصل" else "Webhook غير متصل")
                        if (it.hasRecentError) Text("سجّلت Telegram خطأً مؤخرًا", color = MaterialTheme.colorScheme.error)
                        if (it.pendingUpdates > 0) Text("التحديثات المعلّقة: ${it.pendingUpdates}")
                    }
                    Text("الحسابات المرتبطة: ${status.linkedAccounts}")
                }
            }
        }
        item {
            OutlinedButton(onClick = { check(if (status?.configured == true) "verify" else "status") }, enabled = !busy) {
                Text("التحقق من الاتصال")
            }
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://motazalqmi.vercel.app/admin"))) }
            }) { Text("الربط والإعداد من لوحة الموقع") }
        }
    }
}
