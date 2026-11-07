package com.desarrolloMovielexample.huella.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.desarrolloMovielexample.huella.core.components.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.domain.model.Badge
import com.desarrolloMovielexample.huella.domain.model.ProfileStats

@Composable
fun DetailedStatsSection(stats: ProfileStats, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, HuellaColors.Divider, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Estado de publicaciones", style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), color = HuellaColors.TextPrimary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill(
                count = stats.activeCount,
                label = "Activas",
                containerColor = HuellaColors.PrimarySoft,
                contentColor = HuellaColors.Primary,
                modifier = Modifier.weight(1f)
            )
            StatPill(
                count = stats.finishedCount,
                label = "Finalizadas",
                containerColor = HuellaColors.PrimaryContainer,
                contentColor = HuellaColors.PrimaryDark,
                modifier = Modifier.weight(1f)
            )
            StatPill(
                count = stats.pendingCount,
                label = "En revisi�n",
                containerColor = HuellaColors.WarningContainer,
                contentColor = HuellaColors.WarningDark,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatPill(
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = contentColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgesSection(badges: List<Badge>, modifier: Modifier = Modifier) {
    if (badges.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, HuellaColors.Divider, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.MilitaryTech, contentDescription = null, tint = HuellaColors.Primary, modifier = Modifier.size(20.dp))
            Text("Insignias obtenidas (${badges.size})", style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), color = HuellaColors.TextPrimary)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            badges.forEach { badge ->
                BadgeChip(badge)
            }
        }
    }
}

@Composable
private fun BadgeChip(badge: Badge) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(HuellaColors.PrimarySoft)
            .border(1.dp, HuellaColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val icon = when (badge) {
            Badge.MODERADOR -> Icons.Outlined.Shield
            Badge.ADOPCION_EXITOSA -> Icons.Outlined.Favorite
            Badge.PRIMER_POST -> Icons.Outlined.CheckCircle
            else -> Icons.Outlined.EmojiEvents
        }
        Icon(icon, contentDescription = null, tint = HuellaColors.Primary, modifier = Modifier.size(16.dp))
        Text(badge.label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = HuellaColors.Primary)
    }
}
