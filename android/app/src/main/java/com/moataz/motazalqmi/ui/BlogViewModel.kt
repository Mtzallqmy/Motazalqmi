package com.moataz.motazalqmi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moataz.motazalqmi.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class BlogViewModel @Inject constructor(private val repository: BlogRepository, private val preferences: Settings) : ViewModel() {
 val library = repository.library.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Library())
 val settings = preferences.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingSettings())
 val busy = MutableStateFlow(false)
 val notice = MutableStateFlow("")
 private val initialized = MutableStateFlow(false)
 companion object { @Volatile var autoSyncEnabled: Boolean = true }
 private fun mayAutoSync() = autoSyncEnabled && System.getProperty("motazalqmi.test.offline") != "true"
 init { viewModelScope.launch { try { repository.initialize(); initialized.value = true; if (mayAutoSync()) sync() } catch (_: Exception) { notice.value = "تعذر تجهيز المكتبة المحلية" } } }
 fun syncIfStale() {
  if (!mayAutoSync() || !initialized.value) return
  if(System.currentTimeMillis()-library.value.syncedAt>60_000L)sync()
 }
 fun sync() { if (busy.value) return; viewModelScope.launch {
  busy.value = true
  try { repository.sync(); notice.value = "تم تحديث المكتبة" }
  catch (e: CancellationException) { throw e }
  catch (_: Exception) { notice.value = "تعذر الاتصال. المحتوى المحفوظ متاح للقراءة." }
  finally { busy.value = false }
 } }
 fun save(id: String) { viewModelScope.launch { repository.toggleSaved(id) } }
 fun position(id: String, block: Int, offset: Int, progress: Float) { viewModelScope.launch { repository.progress(id, block, offset, progress) } }
 fun configure(value: ReadingSettings) { viewModelScope.launch { preferences.update(value) } }
 fun clean() { viewModelScope.launch { repository.cleanCache(); notice.value = "تم تنظيف المحتوى غير المحفوظ والصور المؤقتة" } }
}
