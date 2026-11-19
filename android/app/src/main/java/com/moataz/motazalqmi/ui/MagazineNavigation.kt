@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.moataz.motazalqmi.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moataz.motazalqmi.R
import com.moataz.motazalqmi.data.Library

internal fun sectionIcon(id: String) = when (id) {
    "general" -> R.drawable.blog_feather
    "quotes" -> R.drawable.blog_quote
    "books" -> R.drawable.blog_book_open
    "pens" -> R.drawable.blog_sparkles
    "news" -> R.drawable.blog_compass
    "wisdom" -> R.drawable.blog_scroll
    "arabic_lit" -> R.drawable.blog_landmark
    "human" -> R.drawable.blog_heart_handshake
    "culture" -> R.drawable.blog_library_big
    "faith" -> R.drawable.blog_book_heart
    "language" -> R.drawable.blog_languages
    else -> R.drawable.blog_user_check
}

@Composable internal fun MagazineIcon(@DrawableRes icon: Int, description: String? = null, modifier: Modifier = Modifier) {
    Icon(painterResource(icon), description, modifier.size(22.dp))
}

@Composable internal fun MagazineHeader(home: () -> Unit, search: () -> Unit, saved: () -> Unit, writer: () -> Unit, menu: () -> Unit) {
    TopAppBar(
        title = {
            TextButton(onClick = home, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text("معتز العلقمي", fontFamily = Amiri, fontSize = 23.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        actions = {
            HeaderAction(R.drawable.blog_bookmark, "المكتبة المحفوظة", saved)
            HeaderAction(R.drawable.blog_search, "فتح البحث", search)
            HeaderAction(R.drawable.blog_feather, "لوحة الكاتب", writer, highlighted = true)
            HeaderAction(R.drawable.blog_menu, "القائمة", menu)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable private fun HeaderAction(@DrawableRes icon: Int, label: String, click: () -> Unit, highlighted: Boolean = false) {
    IconButton(onClick = click, modifier = Modifier.size(46.dp)) {
        Surface(
            shape = RoundedCornerShape(11.dp), modifier = Modifier.size(39.dp),
            color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(contentAlignment = Alignment.Center) { MagazineIcon(icon, label) }
        }
    }
}

@Composable internal fun MagazineMenu(library: Library, currentRoute: String, navigate: (String) -> Unit, dismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 510.dp).testTag("magazine-menu"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { Text("أبواب المدونة", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 8.dp)) }
            item { MenuEntry("الرئيسية", R.drawable.blog_house, currentRoute == "home") { navigate("home") } }
            items(library.sections, key = { it.id }) { section ->
                val path = "section/${section.id}"
                MenuEntry(section.title, sectionIcon(section.id), currentRoute == path) { navigate(path) }
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                MenuEntry("لوحة الكاتب", R.drawable.blog_feather, currentRoute == "writer") { navigate("writer") }
                MenuEntry("الإعدادات", R.drawable.blog_settings, currentRoute == "settings") { navigate("settings") }
            }
        }
    }
}

@Composable private fun MenuEntry(label: String, @DrawableRes icon: Int, selected: Boolean, open: () -> Unit) {
    Surface(
        onClick = open,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MagazineIcon(icon)
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
