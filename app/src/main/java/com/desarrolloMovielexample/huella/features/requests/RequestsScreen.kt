package com.desarrolloMovielexample.huella.features.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.Avatar
import com.desarrolloMovielexample.huella.core.components.EmptyState
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTabs
import com.desarrolloMovielexample.huella.core.components.HuellaTextButton
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.components.PublicationRowCard
import com.desarrolloMovielexample.huella.core.components.RequestStatusTag
import com.desarrolloMovielexample.huella.core.components.SecondaryButton
import com.desarrolloMovielexample.huella.core.components.StripedPlaceholder
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.core.theme.toneColor
import com.desarrolloMovielexample.huella.domain.model.Applicant
import com.desarrolloMovielexample.huella.domain.model.RequestStatus
import androidx.compose.ui.tooling.preview.Preview

private const val CHAT_ID = "c1" // ponytail: FakeRepository has a single conversation; map applicant -> conversation when a backend exists.

@Composable
fun RequestsScreen(
    initialTab: Int,
    onOpenPublication: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onExplore: () -> Unit,
    viewModel: RequestsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(initialTab) { viewModel.setInitialTab(initialTab) }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            Text(
                "Mis solicitudes",
                style = MaterialTheme.typography.titleLarge,
                color = HuellaColors.TextPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
            )
            HuellaTabs(
                tabs = listOf("Enviadas", "Recibidas"),
                selectedIndex = state.selectedTab,
                onSelect = viewModel::selectTab,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            )
            if (state.selectedTab == 0) {
                if (state.sent.isEmpty()) {
                    CenteredEmpty {
                        EmptyState(
                            illustration = R.drawable.illus_relationship,
                            title = "Aún no has enviado solicitudes",
                            text = "Cuando encuentres una mascota que te robe el corazón, toca “Me interesa adoptar” y aparecerá aquí.",
                            illustrationSize = 230.dp,
                            actionText = "Explorar mascotas",
                            onAction = onExplore,
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(state.sent, key = { it.request.id }) { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                PublicationRowCard(
                                    publication = item.publication,
                                    onClick = { onOpenPublication(item.publication.id) },
                                    trailingTag = { RequestStatusTag(item.request.status) },
                                )
                                Text(
                                    item.request.whenLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HuellaColors.TextTertiary,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                if (state.received.isEmpty()) {
                    CenteredEmpty {
                        EmptyState(
                            illustration = R.drawable.illus_relationship,
                            title = "Aún no has recibido solicitudes",
                            text = "Cuando alguien se interese en tus publicaciones, verás aquí su mensaje para que puedas responder.",
                            illustrationSize = 230.dp,
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(state.received, key = { it.publication.id }) { group ->
                            ReceivedGroupCard(
                                group = group,
                                busyApplicantId = state.busyApplicantId,
                                onOpenPublication = { onOpenPublication(group.publication.id) },
                                onRespond = { applicant, accept -> viewModel.respond(applicant, group.publication.id, accept) },
                                onContact = { onOpenChat(CHAT_ID) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.acceptedPublicationId != null) {
        RequestAcceptedDialog(
            closing = state.isClosing,
            onClosePublication = viewModel::closePublication,
            onKeepOpen = viewModel::keepOpen,
        )
    }
}

@Composable
private fun CenteredEmpty(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ReceivedGroupCard(
    group: ReceivedGroupUi,
    busyApplicantId: String?,
    onOpenPublication: () -> Unit,
    onRespond: (Applicant, Boolean) -> Unit,
    onContact: () -> Unit,
) {
    val p = group.publication
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, HuellaColors.Divider, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPublication),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StripedPlaceholder(
                tone = toneColor(p.tone),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    p.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
                    color = HuellaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${p.species} · ${p.breed} · ${p.city.substringBefore(",")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HuellaColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Ver publicación", tint = HuellaColors.TextTertiary, modifier = Modifier.size(22.dp))
        }
        group.applicants.forEach { applicant ->
            ApplicantCard(
                applicant = applicant,
                busy = busyApplicantId == applicant.id,
                onReject = { onRespond(applicant, false) },
                onAccept = { onRespond(applicant, true) },
                onContact = onContact,
            )
        }
    }
}

@Composable
private fun ApplicantCard(
    applicant: Applicant,
    busy: Boolean,
    onReject: () -> Unit,
    onAccept: () -> Unit,
    onContact: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HuellaColors.Background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(applicant.initials, applicant.level, size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    applicant.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
                    color = HuellaColors.TextPrimary,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HuellaIcon(R.drawable.icon_animal_otro, tint = applicant.level.color, size = 18.dp)
                    Text(applicant.level.label, style = MaterialTheme.typography.labelMedium, color = applicant.level.color)
                }
            }
            Text(applicant.timeAgo, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
        }
        Text(
            applicant.message,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.5.sp),
            color = HuellaColors.TextBody,
        )
        when (applicant.status) {
            RequestStatus.PENDIENTE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    text = "Rechazar",
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    contentColor = HuellaColors.ErrorDark,
                    height = 40.dp,
                )
                PrimaryButton(
                    text = "Aceptar",
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    loading = busy,
                    height = 40.dp,
                )
            }
            RequestStatus.ACEPTADA -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResultTag("Aceptada", R.drawable.icon_estado_aprobada, HuellaColors.PrimarySoft, HuellaColors.Primary)
                Row(
                    Modifier.clickable(onClick = onContact),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HuellaIcon(R.drawable.icon_accion_mensaje, tint = HuellaColors.Primary, size = 23.dp)
                    Text(
                        "Contactar",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                        color = HuellaColors.Primary,
                    )
                }
            }
            RequestStatus.RECHAZADA ->
                ResultTag("Rechazada", R.drawable.icon_estado_rechazada, HuellaColors.ErrorContainer, HuellaColors.ErrorDark)
        }
    }
}

@Composable
private fun ResultTag(text: String, icon: Int, background: Color, content: Color) {
    Row(
        Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HuellaIcon(icon, tint = content, size = 21.dp)
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

/** "Diálogo · Solicitud aceptada". */
@Composable
private fun RequestAcceptedDialog(
    closing: Boolean,
    onClosePublication: () -> Unit,
    onKeepOpen: () -> Unit,
) {
    Dialog(onDismissRequest = onKeepOpen, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(HuellaColors.Background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Outlined.Celebration, contentDescription = null, tint = HuellaColors.Primary, modifier = Modifier.size(30.dp))
            Text(
                "Solicitud aceptada",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
                color = HuellaColors.TextPrimary,
            )
            Text(
                "¿Deseas cerrar la publicación? La mascota se marcará como adoptada y sumarás 50 puntos.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = HuellaColors.TextSecondary,
            )
            Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = "Cerrar publicación",
                    onClick = onClosePublication,
                    modifier = Modifier.fillMaxWidth(),
                    loading = closing,
                    height = 44.dp,
                )
                HuellaTextButton(
                    text = "Mantener abierta",
                    onClick = onKeepOpen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    enabled = !closing,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RequestsScreenPreview() {
    HuellaTheme {
        RequestsScreen(
            initialTab = 0,
            onOpenPublication = {},
            onOpenChat = {},
            onExplore = {},
        )
    }
}
