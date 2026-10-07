package com.desarrolloMovielexample.huella.features.moderation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.Avatar
import com.desarrolloMovielexample.huella.core.components.CategoryChip
import com.desarrolloMovielexample.huella.core.components.EmptyState
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTextField
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.components.PetImage
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.components.StripedPlaceholder
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.core.theme.toneColor
import com.desarrolloMovielexample.huella.domain.model.ModerationItem
import com.desarrolloMovielexample.huella.domain.model.Report
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DisabledReject = Color(0xFFE6B9B9)

/** 06 · Detalle para moderar (Aprobar / Rechazar) + "Rechazar publicación" sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationDetailScreen(
    itemId: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ModerationDetailViewModel = viewModel(),
) {
    LaunchedEffect(itemId) { viewModel.load(itemId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetSnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            // Feedback goes to the sheet's own host while it is open (the sheet window covers the screen).
            val host = if (viewModel.uiState.value.showRejectSheet) sheetSnackbarHostState else snackbarHostState
            launch { host.showSnackbar(message, duration = SnackbarDuration.Short) }
        }
    }
    LaunchedEffect(state.finished) {
        if (state.finished) {
            delay(1_500) // let the confirmation snackbar be read before leaving
            onDone()
        }
    }

    val item = state.item
    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        bottomBar = {
            if (item != null) {
                DecisionBar(
                    approving = state.isApproving,
                    enabled = !state.busy,
                    onReject = viewModel::openRejectSheet,
                    onApprove = viewModel::approve,
                )
            }
        },
        containerColor = HuellaColors.Background,
    ) { padding ->
        if (item == null) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                HuellaTopBar(title = "Moderación", onBack = onBack)
                if (state.loaded) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            illustration = R.drawable.illus_tasks,
                            title = "Publicación no disponible",
                            text = "Esta publicación ya fue moderada o no existe.",
                            actionText = "Volver",
                            onAction = onBack,
                            actionOutlined = true,
                        )
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                ReviewHeader(item, onBack)
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                    ) {
                        PetImage(
                            imageUrl = item.publication.imageUrl,
                            tone = toneColor(item.publication.tone),
                            label = "foto 1/3: ${item.publication.title}",
                            contentDescription = item.publication.title,
                            modifier = Modifier.matchParentSize(),
                        )
                        CategoryChip(item.publication.category, Modifier.padding(10.dp))
                    }
                    DetailBody(item)
                }
            }
        }
    }

    if (state.showRejectSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeRejectSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = HuellaColors.Background,
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .background(HuellaColors.Outline, RoundedCornerShape(2.dp)),
                )
            },
        ) {
            Box {
                RejectSheetContent(
                    selected = state.rejectReason,
                    reasonError = state.reasonError,
                    comment = state.rejectComment,
                    commentError = state.commentError,
                    loading = state.isRejecting,
                    onSelect = viewModel::selectReason,
                    onCommentChange = viewModel::updateComment,
                    onClose = viewModel::closeRejectSheet,
                    onConfirm = viewModel::confirmReject,
                )
                HuellaSnackbarHost(sheetSnackbarHostState, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

/** Yellow "En revisión · 8 h en espera · publicado hace 8 h" header with back arrow. */
@Composable
private fun ReviewHeader(item: ModerationItem, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(HuellaColors.WarningContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás", tint = HuellaColors.TextPrimary)
        }
        HuellaIcon(R.drawable.icon_estado_en_revision, tint = HuellaColors.WarningDark, size = 26.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("En revisión", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = HuellaColors.WarningDark)
            Text(
                "${item.waitTime} · publicado ${item.publication.timeAgo}",
                style = MaterialTheme.typography.bodySmall,
                color = HuellaColors.WarningDark,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailBody(item: ModerationItem) {
    val pub = item.publication
    Column(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            pub.title,
            style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
            color = HuellaColors.TextPrimary,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(pub.species, pub.breed, pub.size, pub.city).forEachIndexed { i, text ->
                if (i > 0) Text("·", fontSize = 13.sp, color = HuellaColors.Outline)
                Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = HuellaColors.TextSecondary)
            }
        }
        Text(
            pub.description,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = HuellaColors.TextBody,
            modifier = Modifier
                .fillMaxWidth()
                .outlinedWhite()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )

        Section("Historial del autor") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .outlinedWhite()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(pub.author.initials, pub.author.level, size = 44.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(pub.author.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = HuellaColors.TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        HuellaIcon(R.drawable.icon_animal_otro, tint = pub.author.level.color, size = 18.dp)
                        Text(pub.author.level.label, style = MaterialTheme.typography.labelMedium, color = HuellaColors.TextTertiary)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallTag("${item.authorApproved} aprobadas", HuellaColors.PrimarySoft, HuellaColors.Primary)
                    SmallTag("${item.authorRejected} rechazadas", HuellaColors.ErrorContainer, HuellaColors.ErrorDark)
                }
            }
        }

        if (item.isReported) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("Reportes recibidos", style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary)
                    Text(item.reports.size.toString(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HuellaColors.Error)
                }
                Column(Modifier.fillMaxWidth().outlinedWhite()) {
                    item.reports.forEachIndexed { i, report ->
                        if (i > 0) HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                        ReportRow(report)
                    }
                }
            }
        }

        Section("Ubicación") {
            // ponytail: maps are out of scope this phase -> static striped placeholder with a pin.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(HuellaShapes.Card),
            ) {
                StripedPlaceholder(tone = Color(0xFFE8EFE9), modifier = Modifier.matchParentSize())
                HuellaIcon(
                    R.drawable.icon_attr_ubicacion,
                    tint = pub.category.color,
                    size = 40.dp,
                    modifier = Modifier.align(Alignment.Center),
                )
                Text(
                    pub.neighborhood ?: pub.city,
                    style = MaterialTheme.typography.labelMedium,
                    color = HuellaColors.TextPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(Color.White, HuellaShapes.Tag)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary)
        content()
    }
}

@Composable
private fun SmallTag(text: String, background: Color, content: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = Modifier
            .background(background, HuellaShapes.Tag)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun ReportRow(report: Report) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HuellaIcon(R.drawable.icon_accion_reportar, tint = HuellaColors.Error, size = 23.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(report.reason, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = HuellaColors.TextPrimary)
                Text(report.who, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
            }
            if (report.detail.isNotBlank()) {
                Text(
                    report.detail,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = HuellaColors.TextSecondary,
                )
            }
        }
    }
}

/** Sticky "Rechazar" (red) + "Aprobar" (green) bar. */
@Composable
private fun DecisionBar(approving: Boolean, enabled: Boolean, onReject: () -> Unit, onApprove: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(HuellaColors.Background)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = HuellaColors.Divider, thickness = 1.dp)
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DecisionButton("Rechazar", R.drawable.icon_accion_rechazar, HuellaColors.Error, false, enabled, onReject, Modifier.weight(1f))
            DecisionButton("Aprobar", R.drawable.icon_accion_aprobar, HuellaColors.Primary, approving, enabled, onApprove, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DecisionButton(
    text: String,
    @DrawableRes icon: Int,
    color: Color,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = HuellaShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = color.copy(alpha = 0.5f),
            disabledContentColor = Color.White,
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            HuellaIcon(icon, tint = Color.White, size = 29.dp)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RejectSheetContent(
    selected: String?,
    reasonError: Boolean,
    comment: String,
    commentError: String?,
    loading: Boolean,
    onSelect: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Rechazar publicación", style = MaterialTheme.typography.titleLarge, color = HuellaColors.TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = HuellaColors.TextPrimary)
            }
        }
        Text(
            buildAnnotatedString {
                append("Motivo ")
                withStyle(SpanStyle(color = HuellaColors.Error)) { append("*") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (reasonError) HuellaColors.Error else HuellaColors.TextSecondary,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(HuellaShapes.Card)
                .background(Color.White)
                .border(1.dp, if (reasonError) HuellaColors.Error else HuellaColors.Divider, HuellaShapes.Card),
        ) {
            REJECT_REASONS.forEachIndexed { i, reason ->
                if (i > 0) HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(reason) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RadioDot(selected == reason)
                    Text(reason, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary)
                }
            }
        }
        HuellaTextField(
            value = comment,
            onValueChange = onCommentChange,
            placeholder = "Comentario para el autor (se envía con la notificación)",
            error = commentError,
            singleLine = false,
            minHeight = 72.dp,
        )
        PrimaryButton(
            text = "Confirmar rechazo",
            onClick = onConfirm,
            loading = loading,
            containerColor = if (selected == null) DisabledReject else HuellaColors.Error,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Design radio: 20dp ring (#8A8F88), red ring + dot when selected. */
@Composable
private fun RadioDot(selected: Boolean) {
    val color = if (selected) HuellaColors.Error else HuellaColors.TextTertiary
    Box(
        Modifier
            .size(20.dp)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).background(color, CircleShape))
    }
}

private fun Modifier.outlinedWhite(): Modifier = this
    .clip(HuellaShapes.Card)
    .background(Color.White)
    .border(1.dp, HuellaColors.Divider, HuellaShapes.Card)
