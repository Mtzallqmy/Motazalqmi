package com.moataz.motazalqmi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moataz.motazalqmi.BuildConfig
import com.moataz.motazalqmi.ui.MainViewModel
import com.moataz.motazalqmi.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.readerSettings.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("الإعدادات وحول التطبيق") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Text("المظهر", style = MaterialTheme.typography.titleMedium) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { vm.setThemeMode(mode) },
                            label = { Text(mode.labelAr) }
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("تحميل الصور الكبيرة عبر Wi-Fi فقط")
                    Switch(checked = settings.wifiOnlyImages, onCheckedChange = { vm.setWifiOnly(it) })
                }
            }
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("المزامنة والتخزين المحلي", style = MaterialTheme.typography.titleMedium)
                        Text("آخر مزامنة ناجحة: ${settings.lastSyncIso ?: "لم تتم بعد"}", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.syncNow() }) { Text("مزامنة الآن") }
                            OutlinedButton(onClick = { vm.clearCache() }) { Text("تنظيف المؤقت") }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("حول تطبيق مدونة معتز العلقمي", style = MaterialTheme.typography.titleMedium)
                        Text("الإصدار: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        Text("برمجة وتطوير: معتز العلقمي — تعز — اليمن — 2026م")
                        Text(
                            "الخصوصية: لا يجمع التطبيق بيانات شخصية ولا يطلب صلاحيات غير ضرورية، وتُحفظ المحفوظات وسجل القراءة محليًا على جهازك فقط.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
