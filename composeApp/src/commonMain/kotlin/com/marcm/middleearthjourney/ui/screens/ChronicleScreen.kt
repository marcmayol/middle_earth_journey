package com.marcm.middleearthjourney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcm.middleearthjourney.data.Direction
import com.marcm.middleearthjourney.data.RouteId
import com.marcm.middleearthjourney.ui.CardBrush
import com.marcm.middleearthjourney.ui.Eyebrow
import com.marcm.middleearthjourney.ui.GoldBorder
import com.marcm.middleearthjourney.ui.GoldBright
import com.marcm.middleearthjourney.ui.TextFaint
import com.marcm.middleearthjourney.ui.TextPrimary
import com.marcm.middleearthjourney.ui.TextSecondary
import com.marcm.middleearthjourney.ui.cinematic.collectionFor
import com.marcm.middleearthjourney.ui.kmEs

@Composable
fun ChronicleScreen(
    routeId: RouteId,
    direction: Direction,
    km: Double,
    onPlayChapter: (Int) -> Unit,
) {
    val collection = collectionFor(routeId, direction)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Eyebrow(collection.eyebrow, align = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(collection.title, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 27.sp), color = TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(collection.tagline, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = TextSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Box(Modifier.width(54.dp).height(1.dp).background(GoldBright.copy(alpha = 0.6f)))
            }
        }
        itemsIndexed(collection.chapters) { i, chap ->
            val unlocked = km >= chap.unlockKm
            val rowMod = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBrush)
                .border(1.dp, GoldBorder, RoundedCornerShape(16.dp))
                .let { if (unlocked) it.clickable { onPlayChapter(i) } else it }
                .padding(horizontal = 14.dp, vertical = 15.dp)
            Row(modifier = rowMod, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .border(1.dp, (if (unlocked) GoldBright else TextFaint).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (unlocked) {
                        Text("%02d".format(i + 1), style = MaterialTheme.typography.titleSmall, color = GoldBright)
                    } else {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextFaint, modifier = Modifier.size(17.dp))
                    }
                }
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        chap.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        color = if (unlocked) TextPrimary else TextSecondary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (unlocked) chap.sub else "Se desbloquea a los ${kmEs(chap.unlockKm, 0)} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextFaint,
                    )
                }
                if (unlocked) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir", tint = GoldBright)
                } else {
                    Icon(Icons.Outlined.Lock, contentDescription = "Bloqueado", tint = TextFaint, modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            Text(
                "Toca un capítulo para reproducirlo · con voz y sonido.",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = TextFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}
