package com.moataz.motazalqmi.ui

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
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
@Composable internal fun ReaderSettingsSheet(settings: ReadingSettings, change: (ReadingSettings) -> Unit, dismiss: () -> Unit) {
 ModalBottomSheet(onDismissRequest = dismiss) {
  Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
   Text("مساحتك للقراءة", style = MaterialTheme.typography.headlineMedium)
   Text("حجم الخط · ${settings.size.toInt()}")
   Slider(settings.size, { change(settings.copy(size = it)) }, valueRange = 16f..34f)
   Text("تباعد السطور")
   Slider(settings.spacing, { change(settings.copy(spacing = it)) }, valueRange = 1.3f..2.4f)
   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf("amiri" to "أميري", "naskh" to "نسخ", "plex" to "بلكس").forEach { (id, name) ->
     FilterChip(settings.font == id, { change(settings.copy(font = id)) }, { Text(name) })
    }
   }
   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf("system" to "النظام", "light" to "نهاري", "dark" to "ليلي").forEach { (id, name) ->
     FilterChip(settings.theme == id, { change(settings.copy(theme = id)) }, { Text(name) })
    }
   }
  }
 }
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
