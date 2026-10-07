package com.desarrolloMovielexample.huella.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaTabs
import com.desarrolloMovielexample.huella.core.components.ModerationStatusTag
import com.desarrolloMovielexample.huella.core.components.PublicationRowCard
import androidx.compose.ui.tooling.preview.Preview
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.theme.Manrope
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.domain.model.PointsEntry
import com.desarrolloMovielexample.huella.domain.model.User

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    onOpenLevels: () -> Unit,
    onOpenPublication: (String) -> Unit,
    onOpenModeration: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(containerColor = HuellaColors.Background) { inner ->
        val user = state.user
        if (user == null) {
            Box(Modifier.fillMaxSize().padding(inner))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onEditProfile) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Editar perfil", tint = HuellaColors.TextPrimary, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = HuellaColors.TextPrimary, modifier = Modifier.size(22.dp))
                }
            }
            ProfileHeader(user, onOpenLevels)
            PointsCard(user, onOpenLevels, Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp))
            StatsRow(
                listOf(
                    user.publicationsCount.toString() to "Publicaciones",
                    user.adoptionsCount.toString() to "Adopciones concretadas",
                    user.fosterCount.toString() to "Hogares temporales",
                ),
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            )
            HuellaTabs(
                tabs = listOf("Mis publicaciones", "Historial de puntos"),
                selectedIndex = state.selectedTab,
                onSelect = viewModel::selectTab,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            )
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.selectedTab == 0) {
                    if (state.publications.isEmpty()) {
                        Text(
                            "Aún no tienes publicaciones.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HuellaColors.TextSecondary,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                    state.publications.forEach { p ->
                        PublicationRowCard(
                            publication = p,
                            onClick = { onOpenPublication(p.id) },
                            trailingTag = { ModerationStatusTag(p.status, small = true) },
                        )
                    }
                } else {
                    PointsHistory(state.history)
                }
                if (user.isModerator) ModerationEntry(onOpenModeration, Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: User, onOpenLevels: () -> Unit) {
    val level = user.level
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProfileAvatar(user.initials, level.color)
        Text(user.name, style = MaterialTheme.typography.titleLarge, color = HuellaColors.TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HuellaIcon(R.drawable.icon_attr_ubicacion, tint = HuellaColors.TextSecondary, size = 21.dp)
            Text(user.city, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = HuellaColors.TextSecondary)
        }
        Row(
            Modifier
                .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = level.color, spotColor = level.color)
                .clip(RoundedCornerShape(24.dp))
                .background(level.color)
                .clickable(onClick = onOpenLevels)
                .padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HuellaIcon(R.drawable.icon_animal_otro, tint = Color.White, size = 26.dp)
            Text(level.label, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
private fun PointsCard(user: User, onOpenLevels: () -> Unit, modifier: Modifier = Modifier) {
    val next = user.nextLevel
    Column(
        modifier
            .fillMaxWidth()
            .outlinedCard()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = HuellaColors.TextPrimary)) {
                        append(user.points.toString())
                    }
                    withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = HuellaColors.TextTertiary)) {
                        append(" puntos")
                    }
                },
                style = TextStyle(fontFamily = Manrope),
                modifier = Modifier.weight(1f),
            )
            Text(
                "Ver niveles",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = HuellaColors.Primary,
                modifier = Modifier
                    .clickable(onClick = onOpenLevels)
                    .padding(bottom = 4.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(HuellaColors.SurfaceMuted),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(user.progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(user.level.color, (next ?: user.level).color))),
            )
        }
        Text(
            buildAnnotatedString {
                if (next != null) {
                    append("Te faltan ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = HuellaColors.TextPrimary)) { append("${user.pointsToNext} puntos") }
                    append(" para ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = next.color)) { append(next.label) }
                } else {
                    append("Alcanzaste el nivel máximo")
                }
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = HuellaColors.TextSecondary,
        )
    }
}

@Composable
private fun PointsHistory(history: List<PointsEntry>) {
    Column(
        Modifier
            .fillMaxWidth()
            .outlinedCard(),
    ) {
        history.forEachIndexed { index, entry ->
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(HuellaColors.PrimarySoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = HuellaColors.Primary, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(entry.what, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary)
                    Text(entry.whenLabel, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
                }
                Text(
                    "+${entry.points}",
                    style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = HuellaColors.Primary,
                )
            }
            if (index < history.lastIndex) HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
        }
    }
}

/** Dashed "Panel de moderación · rol moderador" entry. */
@Composable
private fun ModerationEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = HuellaColors.Outline,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HuellaIcon(R.drawable.icon_accion_moderacion, tint = HuellaColors.TextSecondary, size = 26.dp)
        Text(
            "Panel de moderación",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = HuellaColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            "rol moderador",
            style = MaterialTheme.typography.labelSmall,
            color = HuellaColors.TextTertiary,
            modifier = Modifier
                .background(HuellaColors.SurfaceMuted, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    HuellaTheme {
        ProfileScreen(
            onEditProfile = {},
            onSettings = {},
            onOpenLevels = {},
            onOpenPublication = {},
            onOpenModeration = {},
        )
    }
}
