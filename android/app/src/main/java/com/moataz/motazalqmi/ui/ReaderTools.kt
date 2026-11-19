package com.moataz.motazalqmi.ui

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.moataz.motazalqmi.data.ReadingSettings
import kotlinx.coroutines.delay

@Composable internal fun ReaderAudio(url: String, title: String) {
 val owner = LocalLifecycleOwner.current
 var player by remember(url) { mutableStateOf<MediaPlayer?>(null) }
 var ready by remember(url) { mutableStateOf(false) }
 var playing by remember(url) { mutableStateOf(false) }
 var failed by remember(url) { mutableStateOf(false) }
 var position by remember(url) { mutableIntStateOf(0) }
 var duration by remember(url) { mutableIntStateOf(0) }
 DisposableEffect(url, owner) {
  val observer = LifecycleEventObserver { _, event ->
   if (event == Lifecycle.Event.ON_STOP) { if (ready) player?.pause(); playing = false }
  }
  owner.lifecycle.addObserver(observer)
  onDispose { owner.lifecycle.removeObserver(observer); player?.release(); player = null }
 }
 LaunchedEffect(playing) {
  while (playing) { position = player?.currentPosition ?: 0; delay(400) }
 }
 fun begin() {
  if (player == null) {
   failed = false
   val media = MediaPlayer()
   player = media
   media.setOnPreparedListener { duration = it.duration.coerceAtLeast(0); ready = true; if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)){it.start(); playing = true} }
   media.setOnCompletionListener { playing = false; position = duration }
   media.setOnErrorListener { _, _, _ ->
    failed = true; playing = false; ready = false; media.release(); player = null; true
   }
   try { media.setDataSource(url); media.prepareAsync() }
   catch (_: Exception) { failed = true; media.release(); player = null }
  } else if (ready) {
   if (playing) player?.pause() else player?.start()
   playing = !playing
  }
 }
 OutlinedCard(Modifier.fillMaxWidth()) {
  Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Text(title.ifBlank { "تسجيل صوتي" }, style = MaterialTheme.typography.titleMedium)
   Row(verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = ::begin, enabled = player == null || ready) {
     Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "إيقاف مؤقت" else "تشغيل التسجيل")
    }
    Text(if (failed) "تعذّر التشغيل. اضغط للمحاولة مجددًا" else if (player != null && !ready) "جار تجهيز الصوت…"
      else "${audioTime(position)} / ${audioTime(duration)}", style = MaterialTheme.typography.bodySmall)
   }
   if (ready && duration > 0) Slider(value = position.toFloat().coerceIn(0f, duration.toFloat()),
    onValueChange = { position = it.toInt(); player?.seekTo(position) }, valueRange = 0f..duration.toFloat())
  }
 }
}

private fun audioTime(ms: Int): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ReaderSettingsSheet(settings: ReadingSettings, change: (ReadingSettings) -> Unit,
    focus: Boolean, setFocus: (Boolean) -> Unit, dismiss: () -> Unit) {
 ModalBottomSheet(onDismissRequest = dismiss) {
  Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text("مساحتك للقراءة", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
    TextButton(onClick = dismiss) { Text("تم") }
   }
   Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("حجم الخط")
    FilterChip(settings.useArticleSize, { change(settings.copy(useArticleSize = !settings.useArticleSize)) }, { Text("اختيار الكاتب") })
    Slider(settings.size, { change(settings.copy(size = it, useArticleSize = false)) }, valueRange = 16f..34f)
    Text("تباعد السطور")
    Slider(settings.spacing, { change(settings.copy(spacing = it)) }, valueRange = 1.3f..2.5f)
    Text("تباعد الفقرات")
    Slider(settings.paragraphSpacing, { change(settings.copy(paragraphSpacing = it)) }, valueRange = .6f..1.8f, steps = 1)
    ReaderOptionRow("الخط", settings.font, listOf("editor" to "اختيار الكاتب", "amiri" to "أميري", "naskh" to "نسخ", "plex" to "بلكس")) {
     change(settings.copy(font = it))
    }
    ReaderOptionRow("عرض النص", settings.textWidth, listOf("narrow" to "ضيّق", "regular" to "متوسط", "wide" to "واسع")) {
     change(settings.copy(textWidth = it))
    }
    ReaderOptionRow("محاذاة النص", settings.alignment, listOf("start" to "يمين", "justify" to "ضبط", "center" to "توسيط")) {
     change(settings.copy(alignment = it))
    }
    ReaderOptionRow("المظهر", settings.theme, listOf("system" to "النظام", "light" to "نهاري", "dark" to "ليلي")) {
     change(settings.copy(theme = it))
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
     Text("قراءة مركزة", Modifier.weight(1f))
     Switch(focus, onCheckedChange = setFocus)
    }
   }
  }
 }
}

@Composable private fun ReaderOptionRow(label: String, selected: String, options: List<Pair<String, String>>, change: (String) -> Unit) {
 Text(label)
 Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
  options.forEach { (id, name) -> FilterChip(selected == id, { change(id) }, { Text(name) }) }
 }
}

internal fun spaceReaderParagraphs(body: android.text.Spanned, ratio: Float): android.text.Spanned {
 val result = android.text.SpannableString(body)
 var lastWasNewline = false
 body.forEachIndexed { index, char ->
  if (char == '\n' && lastWasNewline) {
   val spacer = android.text.style.LineHeightSpan { _, _, _, _, _, metrics ->
    val line = (metrics.descent - metrics.ascent).coerceAtLeast(1)
    metrics.descent += ((ratio - 1f) * line).toInt()
    metrics.bottom = maxOf(metrics.bottom, metrics.descent)
   }
   result.setSpan(spacer, index, index + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  }
  lastWasNewline = char == '\n'
 }
 return result
}

@Composable internal fun ReaderVideo(url: String) {
 val owner = LocalLifecycleOwner.current
 val context = androidx.compose.ui.platform.LocalContext.current
 val view = remember(url) {
  android.widget.VideoView(context).apply {
   setMediaController(android.widget.MediaController(context))
   setVideoURI(android.net.Uri.parse(url))
  }
 }
 DisposableEffect(view, owner) {
  val observer = LifecycleEventObserver { _, event -> if(event==Lifecycle.Event.ON_STOP)view.pause() }
  owner.lifecycle.addObserver(observer)
  onDispose { owner.lifecycle.removeObserver(observer); view.stopPlayback() }
 }
 androidx.compose.ui.viewinterop.AndroidView(factory={view},modifier=Modifier.fillMaxWidth().height(220.dp))
}
