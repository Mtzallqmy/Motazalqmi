package com.moataz.motazalqmi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.moataz.motazalqmi.R

internal const val HEALTH_SECTION = "health"
internal const val HEALTH_NOTICE = "محتوى للتوعية العامة. استشر طبيبًا أو صيدليًا بشأن التشخيص أو الجرعة أو تغيير العلاج."
internal val HealthTopics = listOf("سلامة الدواء", "المضادات الحيوية", "اسأل الصيدلي")

@Composable internal fun HealthDoor(open: () -> Unit) {
    Card(onClick = open, modifier = Modifier.fillMaxWidth().testTag("health-door"), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            MagazineIcon(R.drawable.blog_health)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("الصحة والدواء", style = MaterialTheme.typography.titleLarge)
                Text("أدلة عربية عن سلامة الدواء وأسئلة الصيدلي", style = MaterialTheme.typography.bodyMedium)
            }
            MagazineIcon(R.drawable.blog_arrow_left)
        }
    }
}

@Composable internal fun HealthNotice() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()) {
        Text(HEALTH_NOTICE, Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
    }
}
