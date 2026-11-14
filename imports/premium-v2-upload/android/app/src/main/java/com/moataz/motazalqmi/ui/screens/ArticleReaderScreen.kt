package com.moataz.motazalqmi.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moataz.motazalqmi.BuildConfig
import com.moataz.motazalqmi.domain.text.ContentBlock
import com.moataz.motazalqmi.domain.text.MarkdownPoetryParser
import com.moataz.motazalqmi.ui.MainViewModel
import com.moataz.motazalqmi.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderScreen(
    slug: String,
    vm: MainViewModel,
    onBack: () -> Unit,
    onNavigateToArticle: (String) -> Unit
) {
    val article by vm.observeArticle(slug).collectAsState(initial = null)
    val allArticles by vm.allArticles.collectAsState()
    val settings by vm.readerSettings.collectAsState()
    val context = LocalContext.current

    var chromeVisible by remember { mutableStateOf(true) }
    var showTypographySheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val blocks = remember(article?.contentMarkdown) {
        MarkdownPoetryParser.parse(article?.contentMarkdown.orEmpty())
    }

    // Restore last scroll position once
    LaunchedEffect(article?.id) {
        val pos = article?.lastReadPosition ?: 0
        if (pos > 0) listState.scrollToItem(pos)
    }

    // Save reading progress
    val progress by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount.coerceAtLeast(1)
            (listState.firstVisibleItemIndex.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
    }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        article?.let {
            vm.saveReadingProgress(it.id, listState.firstVisibleItemIndex, progress)
        }
    }

    val bgColor = if (settings.nightReadingMode) ReadingNightBackground else MaterialTheme.colorScheme.background
    val textColor = if (settings.nightReadingMode) ReadingNightText else MaterialTheme.colorScheme.onBackground
    val bodyFont = fontFamilyFor(settings.fontOption)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            AnimatedVisibility(visible = chromeVisible) {
                Column {
                    TopAppBar(
                        title = { Text(article?.categoryName.orEmpty(), maxLines = 1) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showTypographySheet = true }) {
                                Icon(Icons.Outlined.TextFormat, contentDescription = "إعدادات الخط")
                            }
                            article?.let { art ->
                                IconButton(onClick = { vm.toggleBookmark(art) }) {
                                    Icon(
                                        if (art.isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "حفظ"
                                    )
                                }
                                IconButton(onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${art.title}\n${BuildConfig.SITE_BASE_URL}/articles/${art.slug}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة المقال"))
                                }) {
                                    Icon(Icons.Outlined.Share, contentDescription = "مشاركة")
                                }
                            }
                        }
                    )
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { padding ->
        val art = article
        if (art == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("جاري تحميل المقال...", color = textColor)
            }
        } else {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .clickable { chromeVisible = !chromeVisible },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = art.title,
                            style = MaterialTheme.typography.displaySmall,
                            color = textColor
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${art.authorName} • ${art.publishedAt.take(10)} • ${art.readingTimeMinutes} د قراءة",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (!art.coverImageUrl.isNullOrBlank()) {
                        item {
                            AsyncImage(
                                model = art.coverImageUrl,
                                contentDescription = art.title,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        }
                    }

                    items(blocks) { block ->
                        when (block) {
                            is ContentBlock.Heading -> Text(
                                text = block.text,
                                fontFamily = AmiriFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = (settings.fontSizeSp + (4 - block.level) * 2).sp,
                                color = textColor
                            )
                            is ContentBlock.Paragraph -> Text(
                                text = block.text,
                                fontFamily = bodyFont,
                                fontSize = settings.fontSizeSp.sp,
                                lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                                color = textColor
                            )
                            is ContentBlock.Blockquote -> Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "«${block.text}»",
                                    fontFamily = AmiriFamily,
                                    fontSize = settings.fontSizeSp.sp,
                                    lineHeight = (settings.fontSizeSp * 1.8f).sp,
                                    color = textColor,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                            is ContentBlock.BulletList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                block.items.forEach { itemText ->
                                    Text(
                                        text = "• $itemText",
                                        fontFamily = bodyFont,
                                        fontSize = settings.fontSizeSp.sp,
                                        lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                                        color = textColor
                                    )
                                }
                            }
                            is ContentBlock.PoetryStanza -> Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                block.verses.forEach { verse ->
                                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = verse.sadr,
                                            fontFamily = AmiriFamily,
                                            fontSize = (settings.fontSizeSp + 1f).sp,
                                            color = textColor,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Start
                                        )
                                        if (verse.ajuz.isNotBlank()) {
                                            Text(
                                                text = verse.ajuz,
                                                fontFamily = AmiriFamily,
                                                fontSize = (settings.fontSizeSp + 1f).sp,
                                                color = textColor,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }
                            }
                            ContentBlock.Divider -> HorizontalDivider()
                        }
                    }

                    // Next / Previous article navigation
                    item {
                        val idx = allArticles.indexOfFirst { it.slug == art.slug }
                        val prevArt = allArticles.getOrNull(idx - 1)
                        val nextArt = allArticles.getOrNull(idx + 1)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (prevArt != null) {
                                OutlinedButton(onClick = { onNavigateToArticle(prevArt.slug) }) {
                                    Text("المقال السابق")
                                }
                            } else Spacer(Modifier.width(1.dp))
                            if (nextArt != null) {
                                Button(onClick = { onNavigateToArticle(nextArt.slug) }) {
                                    Text("المقال التالي")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTypographySheet) {
        ModalBottomSheet(onDismissRequest = { showTypographySheet = false }) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("إعدادات القراءة", style = MaterialTheme.typography.headlineSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("وضع القراءة الليلي المريح")
                    Switch(checked = settings.nightReadingMode, onCheckedChange = { vm.setNightReading(it) })
                }
                Text("حجم الخط: ${settings.fontSizeSp.toInt()}")
                Slider(value = settings.fontSizeSp, onValueChange = { vm.setFontSize(it) }, valueRange = 14f..28f)
                Text("تباعد السطور: ${"%.2f".format(settings.lineHeightMultiplier)}")
                Slider(value = settings.lineHeightMultiplier, onValueChange = { vm.setLineHeight(it) }, valueRange = 1.4f..2.3f)
                ArabicFontOption.entries.forEach { opt ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { vm.setFontOption(opt) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = settings.fontOption == opt, onClick = { vm.setFontOption(opt) })
                        Text(opt.displayNameAr)
                    }
                }
            }
        }
    }
}
