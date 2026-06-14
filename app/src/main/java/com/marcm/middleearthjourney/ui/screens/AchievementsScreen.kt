package com.marcm.middleearthjourney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.marcm.middleearthjourney.JourneyState
import com.marcm.middleearthjourney.data.Achievement
import com.marcm.middleearthjourney.ui.CardBrush
import com.marcm.middleearthjourney.ui.CodexCard
import com.marcm.middleearthjourney.ui.CardQuietBrush
import com.marcm.middleearthjourney.ui.GoldBorder
import com.marcm.middleearthjourney.ui.GoldBright
import com.marcm.middleearthjourney.ui.HoyGold2
import com.marcm.middleearthjourney.ui.OnHoy
import com.marcm.middleearthjourney.ui.TextFaint
import com.marcm.middleearthjourney.ui.TextPrimary
import com.marcm.middleearthjourney.ui.TextSecondary
import com.marcm.middleearthjourney.ui.kmEs

@Composable
fun AchievementsScreen(state: JourneyState) {
    val all = state.allAchievements.sortedBy { it.unlockKm }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(state) }
        items(all) { ach ->
            AchievementRow(ach, unlocked = ach.unlockKm <= state.km)
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun Header(state: JourneyState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Logros", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                "${state.unlocked.size} / ${state.totalAchievements} desbloqueados",
                style = MaterialTheme.typography.labelLarge,
                color = GoldBright,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AchievementRow(ach: Achievement, unlocked: Boolean) {
    CodexCard(
        modifier = Modifier.fillMaxWidth(),
        brush = if (unlocked) CardBrush else CardQuietBrush,
        radius = 18.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AchievementIcon(unlocked)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    ach.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (unlocked) TextPrimary else TextSecondary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    ach.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontStyle = FontStyle.Italic,
                )
            }
            if (!unlocked) {
                Spacer(Modifier.width(10.dp))
                Text(
                    "${kmEs(ach.unlockKm, 0)} km",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextFaint,
                )
            }
        }
    }
}

@Composable
private fun AchievementIcon(unlocked: Boolean) {
    if (unlocked) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(HoyGold2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = OnHoy, modifier = Modifier.size(22.dp))
        }
    } else {
        Box(
            Modifier.size(38.dp).clip(CircleShape).border(1.5.dp, TextFaint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextFaint, modifier = Modifier.size(18.dp))
        }
    }
}
