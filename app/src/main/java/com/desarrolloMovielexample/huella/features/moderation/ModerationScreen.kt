package com.desarrolloMovielexample.huella.features.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.desarrolloMovielexample.huella.core.components.ModerationStatusTag
import com.desarrolloMovielexample.huella.core.components.PublicationRowCard
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import com.desarrolloMovielexample.huella.domain.model.ModerationItem
import com.desarrolloMovielexample.huella.domain.model.PostStatus

/** 06 · Panel de moderación (Pendientes / Reportes) + "Cola al día" empty state. */
@Composable
fun ModerationScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: ModerationViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
            if (visible.isEmpty()) {
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

@Composable
private fun ModerationListItem(item: ModerationItem, onClick: () -> Unit) {
    val pendingTag: (@Composable () -> Unit)? =
        if (item.isReported) null else ({ ModerationStatusTag(PostStatus.EN_REVISION, small = true) })
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PublicationRowCard(publication = item.publication, onClick = onClick, trailingTag = pendingTag)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HuellaIcon(R.drawable.icon_estado_en_revision, tint = HuellaColors.TextTertiary, size = 18.dp)
                Text(item.waitTime, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
            }
            if (item.isReported) {
                val count = item.reports.size
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HuellaIcon(R.drawable.icon_accion_reportar, tint = HuellaColors.Error, size = 18.dp)
                    Text(
                        if (count == 1) "1 denuncia" else "$count denuncias",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HuellaColors.Error,
                    )
                }
            } else {
                Text(item.publication.author.name, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
            }
        }
    }
}
