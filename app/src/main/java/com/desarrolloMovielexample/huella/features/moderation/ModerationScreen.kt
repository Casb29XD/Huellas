package com.desarrolloMovielexample.huella.features.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import com.desarrolloMovielexample.huella.core.components.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.EmptyState
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTabs
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.components.CategoryChip
import com.desarrolloMovielexample.huella.core.components.PetImage
import com.desarrolloMovielexample.huella.core.components.huellaCard
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.domain.model.ModerationItem

/** 06 · Panel de moderación (Pendientes / Reportes) + "Cola al día" empty state. */
@Composable
fun ModerationScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: ModerationViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            HuellaTopBar(title = "Moderación", onBack = onBack) {
                ModeratorBadge(Modifier.padding(end = 4.dp))
            }
        },
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val pendingColors = if (state.stats.pending > 0) {
                    HuellaColors.WarningContainer to HuellaColors.WarningDark
                } else {
                    HuellaColors.SurfaceMuted to HuellaColors.TextSecondary
                }
                val reportedColors = if (state.stats.reported > 0) {
                    HuellaColors.ErrorContainer to HuellaColors.ErrorDark
                } else {
                    HuellaColors.SurfaceMuted to HuellaColors.TextSecondary
                }
                StatCard(state.stats.pending, "Pendientes", pendingColors.first, pendingColors.second, Modifier.weight(1f))
                StatCard(state.stats.reported, "Reportadas", reportedColors.first, reportedColors.second, Modifier.weight(1f))
                StatCard(state.stats.approvedToday, "Aprobadas hoy", HuellaColors.PrimarySoft, HuellaColors.Primary, Modifier.weight(1f))
            }

            HuellaTabs(
                tabs = listOf("Pendientes", "Reportes"),
                selectedIndex = state.selectedTab,
                onSelect = viewModel::selectTab,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
            )

            val visible = state.visibleItems
            if (state.isLoading && visible.isEmpty()) {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = HuellaColors.Primary)
                }
            } else if (state.loadError != null && visible.isEmpty()) {
                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(bottom = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        illustration = R.drawable.illus_tasks,
                        title = "No pudimos cargar la cola",
                        text = state.loadError.orEmpty(),
                        illustrationSize = 210.dp,
                        actionText = "Reintentar",
                        onAction = viewModel::retry,
                        actionOutlined = true,
                    )
                }
            } else if (visible.isEmpty()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        illustration = R.drawable.illus_tasks,
                        title = "Cola al día",
                        text = if (state.selectedTab == 0) {
                            "No hay publicaciones pendientes de revisión. Gracias por cuidar la comunidad."
                        } else {
                            "No hay publicaciones reportadas. Gracias por cuidar la comunidad."
                        },
                        illustrationSize = 230.dp,
                        // ponytail: no home callback in the fixed signature; back returns to Perfil.
                        actionText = "Ir al inicio",
                        onAction = onBack,
                        actionOutlined = true,
                    )
                }
            } else {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visible, key = { it.id }) { item ->
                        ModerationListItem(item, onClick = { onOpenItem(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeratorBadge(modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(HuellaColors.OutlineVariant, HuellaShapes.Tag)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HuellaIcon(R.drawable.icon_accion_moderacion, tint = HuellaColors.TextPrimary, size = 18.dp)
        Text("Moderador", style = MaterialTheme.typography.labelSmall, color = HuellaColors.TextPrimary)
    }
}

@Composable
private fun StatCard(value: Int, label: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(background, HuellaShapes.Card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp),
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1)
    }
}

/** Fila propia del panel: título a 2 líneas y una sola métrica de tiempo (ítems 45 y 43). */
@Composable
private fun ModerationListItem(item: ModerationItem, onClick: () -> Unit) {
    val pub = item.publication
    Row(
        Modifier
            .fillMaxWidth()
            .huellaCard()
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            PetImage(pub, Modifier.matchParentSize(), showLabel = false)
            CategoryChip(pub.category, Modifier.padding(6.dp), small = true)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                pub.title,
                style = MaterialTheme.typography.titleSmall,
                color = HuellaColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(pub.species, pub.breed, pub.city).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = HuellaColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Publicada ${pub.timeAgo} · ${pub.author.name}",
                style = MaterialTheme.typography.bodySmall,
                color = HuellaColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.isReported) {
                val count = item.reports.size
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HuellaIcon(R.drawable.icon_accion_reportar, tint = HuellaColors.Error, size = 18.dp)
                    Text(
                        if (count == 1) "1 denuncia" else "$count denuncias",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HuellaColors.ErrorDark,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ModerationScreenPreview() {
    HuellaTheme {
        ModerationScreen(
            onBack = {},
            onOpenItem = {},
        )
    }
}
