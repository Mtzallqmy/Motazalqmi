package com.moataz.motazalqmi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moataz.motazalqmi.data.preferences.ReaderPreferencesRepository
import com.moataz.motazalqmi.data.repository.BlogRepository
import com.moataz.motazalqmi.domain.model.*
import com.moataz.motazalqmi.ui.theme.AppThemeMode
import com.moataz.motazalqmi.ui.theme.ArabicFontOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: BlogRepository,
    private val prefs: ReaderPreferencesRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allArticles: StateFlow<List<Article>> = repository.observeAllArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredArticle: StateFlow<Article?> = repository.observeFeaturedArticle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val quotes: StateFlow<List<Quote>> = repository.observeQuotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteQuotes: StateFlow<List<Quote>> = repository.observeFavoriteQuotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedArticles: StateFlow<List<Article>> = repository.observeBookmarkedArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyArticles: StateFlow<List<Article>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unfinishedArticles: StateFlow<List<Article>> = repository.observeUnfinished()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readerSettings: StateFlow<ReaderSettings> = prefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReaderSettings())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Article>> = _searchQuery
        .debounce(200)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList()) else repository.searchArticles(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeedCategories()
            syncNow()
        }
    }

    fun observeByCategory(categoryId: String): Flow<List<Article>> =
        repository.observeArticlesByCategory(categoryId)

    fun observeArticle(slug: String): Flow<Article?> =
        repository.observeArticleBySlug(slug)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            when (val res = repository.syncIncremental()) {
                is SyncResult.Success -> _syncMessage.value = "تمت المزامنة بنجاح"
                is SyncResult.Error -> _syncMessage.value = res.message
            }
            _isSyncing.value = false
        }
    }

    fun clearSyncMessage() { _syncMessage.value = null }

    fun toggleBookmark(article: Article) {
        viewModelScope.launch { repository.toggleBookmark(article.id, !article.isBookmarked) }
    }

    fun toggleFavoriteQuote(quote: Quote) {
        viewModelScope.launch { repository.toggleFavoriteQuote(quote.id, !quote.isFavorite) }
    }

    fun saveReadingProgress(articleId: String, position: Int, progress: Float) {
        viewModelScope.launch { repository.saveReadingProgress(articleId, position, progress) }
    }

    fun setThemeMode(mode: AppThemeMode) { viewModelScope.launch { prefs.setThemeMode(mode) } }
    fun setFontOption(opt: ArabicFontOption) { viewModelScope.launch { prefs.setFontOption(opt) } }
    fun setFontSize(sizeSp: Float) { viewModelScope.launch { prefs.setFontSize(sizeSp) } }
    fun setLineHeight(mult: Float) { viewModelScope.launch { prefs.setLineHeight(mult) } }
    fun setNightReading(enabled: Boolean) { viewModelScope.launch { prefs.setNightReading(enabled) } }
    fun setWifiOnly(enabled: Boolean) { viewModelScope.launch { prefs.setWifiOnlyImages(enabled) } }
    fun clearCache() { viewModelScope.launch { repository.clearTemporaryCachePreservingSaved() } }
}
