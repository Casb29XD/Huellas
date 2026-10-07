package com.desarrolloMovielexample.huella.features.profile

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.EmptyState
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTextButton
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.domain.model.AppNotification
import com.desarrolloMovielexample.huella.domain.model.NotificationType
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenRequests: (Int) -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenPublication: (String) -> Unit,
    onOpenLevels: () -> Unit,
    viewModel: NotificationsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    fun open(n: AppNotification) {
        when (n.type) {
            NotificationType.NEW_REQUEST -> onOpenRequests(1)
            NotificationType.REQUEST_ACCEPTED -> onOpenChat(n.targetId ?: "c1")
            NotificationType.POST_APPROVED, NotificationType.POST_REJECTED -> n.targetId?.let(onOpenPublication)
            NotificationType.LEVEL_UP -> onOpenLevels()
        }
    }

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
        topBar = {
            HuellaTopBar("Notificaciones", Modifier.statusBarsPadding(), onBack = onBack) {
                if (!state.isEmpty) HuellaTextButton("Marcar leídas", viewModel::markAllRead)
            }
        },
    ) { inner ->
        if (state.isEmpty) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    illustration = R.drawable.illus_empty_inbox,
                    title = "Todo tranquilo por aquí",
                    text = "Te avisaremos cuando alguien se interese en tus publicaciones o cuando subas de nivel.",
                    illustrationSize = 230.dp,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            state.groups.forEach { (day, items) ->
                item(key = day) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GroupCaption(day)
                        items.forEach { n -> NotificationCard(n, onClick = { open(n) }) }
                    }
                }
            }
        }
    }
}

private fun iconFor(type: NotificationType): Int = when (type) {
    NotificationType.NEW_REQUEST -> R.drawable.icon_nav_solicitudes
    NotificationType.REQUEST_ACCEPTED -> R.drawable.icon_accion_solicitud_enviada
    NotificationType.POST_APPROVED -> R.drawable.icon_estado_aprobada
    NotificationType.POST_REJECTED -> R.drawable.icon_estado_rechazada
    NotificationType.LEVEL_UP -> R.drawable.icon_animal_otro
}

private fun colorFor(type: NotificationType): Color = when (type) {
    NotificationType.POST_REJECTED -> HuellaColors.Error
    NotificationType.LEVEL_UP -> HuellaColors.LevelProtector
    else -> HuellaColors.Primary
}

@Composable
private fun NotificationCard(n: AppNotification, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .outlinedCard()
            .then(if (n.type == NotificationType.LEVEL_UP) Modifier.confetti() else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(colorFor(n.type), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            HuellaIcon(iconFor(n.type), tint = Color.White, size = 29.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    n.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
                    color = HuellaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(n.time, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = HuellaColors.TextTertiary, maxLines = 1)
            }
            Text(
                n.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.85.sp),
                color = HuellaColors.TextSecondary,
            )
        }
    }
}

/** Confetti pieces of the "¡Subiste de nivel!" card (positions from the design, in dp). */
private fun Modifier.confetti(): Modifier = drawBehind {
    fun piece(x: Float, y: Float, w: Float, h: Float, degrees: Float, color: Color, fromBottom: Boolean = false, round: Boolean = false) {
        val pw = w.dp.toPx()
        val ph = h.dp.toPx()
        val left = x.dp.toPx()
        val top = if (fromBottom) size.height - y.dp.toPx() - ph else y.dp.toPx()
        rotate(degrees, pivot = Offset(left + pw / 2, top + ph / 2)) {
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(pw, ph),
                cornerRadius = CornerRadius(if (round) pw / 2 else 2.dp.toPx()),
            )
        }
    }
    piece(60f, 8f, 6f, 10f, 20f, Color(0xFFF4A261))
    piece(200f, 6f, 6f, 10f, -30f, Color(0xFF2E7D5B))
    piece(300f, 14f, 8f, 8f, 0f, Color(0xFF7A9CC6), round = true)
    piece(250f, 10f, 6f, 10f, 45f, Color(0xFFE0A526), fromBottom = true)
    piece(120f, 8f, 7f, 7f, 0f, Color(0xFFD64545), fromBottom = true, round = true)
    piece(330f, 22f, 6f, 10f, -15f, Color(0xFFF4A261), fromBottom = true)
}

@Preview(showBackground = true)
@Composable
fun NotificationsScreenPreview() {
    HuellaTheme {
        NotificationsScreen(
            onBack = {},
            onOpenRequests = {},
            onOpenChat = {},
            onOpenPublication = {},
            onOpenLevels = {},
        )
    }
}
