package com.desarrolloMovielexample.huella.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.theme.Manrope
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.domain.model.Level
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LevelsScreen(
    onBack: () -> Unit,
    viewModel: LevelsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = HuellaColors.Background,
        topBar = { HuellaTopBar("Niveles", Modifier.statusBarsPadding(), onBack = onBack) },
    ) { inner ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.levels) { level -> LevelCard(level, current = level == state.currentLevel) }
            item {
                Text(
                    "Cómo ganar puntos",
                    style = MaterialTheme.typography.titleMedium,
                    color = HuellaColors.TextPrimary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .outlinedCard(),
                ) {
                    state.actions.forEachIndexed { index, action ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(action.what, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary, modifier = Modifier.weight(1f))
                            Text(
                                "+${action.points}",
                                style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                color = HuellaColors.Primary,
                                modifier = Modifier
                                    .background(HuellaColors.PrimarySoft, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        if (index < state.actions.lastIndex) HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelCard(level: Level, current: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (current) level.color.copy(alpha = 0.07f) else Color.White)
            .border(1.5.dp, if (current) level.color else HuellaColors.OutlineVariant, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(level.color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            HuellaIcon(R.drawable.icon_animal_otro, tint = Color.White, size = 34.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(level.label, style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary, modifier = Modifier.weight(1f))
                if (current) {
                    Text(
                        "Tu nivel",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .background(level.color, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Text(level.range, style = MaterialTheme.typography.labelMedium, color = level.color)
            Column(Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                level.benefits.forEach { benefit ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = HuellaColors.Primary, modifier = Modifier.size(16.dp))
                        Text(
                            benefit,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                            color = HuellaColors.TextBody,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LevelsScreenPreview() {
    HuellaTheme {
        LevelsScreen(onBack = {})
    }
}
