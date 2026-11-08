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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
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
import com.desarrolloMovielexample.huella.core.components.labelRes
import com.desarrolloMovielexample.huella.core.components.optionLabel
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.core.theme.toneColor
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.utils.asString
import com.desarrolloMovielexample.huella.data.remote.AiReview
import com.desarrolloMovielexample.huella.domain.model.ModerationItem
import com.desarrolloMovielexample.huella.domain.model.Report
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val DisabledReject = Color(0xFFE6B9B9)

/** 06 · Detalle para moderar (Aprobar / Rechazar) + "Rechazar publicación" sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationDetailScreen(
    itemId: String,
    onBack: () -> Unit,
    /** Recibe la confirmación para mostrarla en la pantalla de destino. */
    onDone: (UiText) -> Unit,
    viewModel: ModerationDetailViewModel = viewModel(),
) {
    LaunchedEffect(itemId) { viewModel.load(itemId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val resources = LocalContext.current.resources
    LaunchedEffect(viewModel) {
        // El Scaffold ancla el snackbar sobre la DecisionBar; los errores del sheet se muestran inline.
        viewModel.messages.collect { message ->
            launch { snackbarHostState.showSnackbar(message.resolve(resources), duration = SnackbarDuration.Short) }
        }
    }
    LaunchedEffect(state.doneMessage) { state.doneMessage?.let(onDone) }

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
                HuellaTopBar(title = stringResource(R.string.moderation_title), onBack = onBack)
                if (state.loaded) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            illustration = R.drawable.illus_tasks,
                            title = stringResource(R.string.detail_unavailable),
                            text = stringResource(R.string.moderation_already_done),
                            actionText = stringResource(R.string.action_go_back),
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
                            contentDescription = item.publication.title,
                            modifier = Modifier.matchParentSize(),
                        )
                        CategoryChip(item.publication.category, Modifier.padding(10.dp))
                    }
                    DetailBody(item, state.resolvingReportId, viewModel::resolveReport) {
                        AiSuggestionCard(state.aiReview, state.aiLoading, state.aiMessage, viewModel::analyzeWithAi)
                    }
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
            RejectSheetContent(
                selected = state.rejectReason,
                reasonError = state.reasonError,
                comment = state.rejectComment,
                commentError = state.commentError,
                rejectError = state.rejectError,
                loading = state.isRejecting,
                onSelect = viewModel::selectReason,
                onCommentChange = viewModel::updateComment,
                onClose = viewModel::closeRejectSheet,
                onConfirm = viewModel::confirmReject,
            )
        }
    }
}

/** Yellow "En revisión · Publicada hace 8 h" header (una sola métrica de tiempo). */
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
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = HuellaColors.TextPrimary)
        }
        HuellaIcon(R.drawable.icon_estado_en_revision, tint = HuellaColors.WarningDark, size = 26.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                stringResource(if (item.isReported) R.string.moderation_reported_post else R.string.post_status_review),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = HuellaColors.WarningDark,
            )
            Text(
                stringResource(R.string.moderation_published, item.publication.timeAgo.asString()),
                style = MaterialTheme.typography.bodySmall,
                color = HuellaColors.WarningDark,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailBody(
    item: ModerationItem,
    resolvingReportId: String?,
    onResolve: (Report) -> Unit,
    aiCard: @Composable () -> Unit,
) {
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
            listOf(optionLabel(pub.species), pub.breed, optionLabel(pub.size), pub.city).forEachIndexed { i, text ->
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

        aiCard()

        Section(stringResource(R.string.moderation_author_history)) {
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
                        Text(stringResource(pub.author.level.labelRes), style = MaterialTheme.typography.labelMedium, color = HuellaColors.TextTertiary)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallTag(pluralStringResource(R.plurals.moderation_approved_n, item.authorApproved, item.authorApproved), HuellaColors.PrimarySoft, HuellaColors.Primary)
                    SmallTag(pluralStringResource(R.plurals.moderation_rejected_n, item.authorRejected, item.authorRejected), HuellaColors.ErrorContainer, HuellaColors.ErrorDark)
                }
            }
        }

        if (item.isReported) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.moderation_reports_received), style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary)
                    Text(item.reports.size.toString(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HuellaColors.Error)
                }
                Column(Modifier.fillMaxWidth().outlinedWhite()) {
                    item.reports.forEachIndexed { i, report ->
                        if (i > 0) HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                        ReportRow(report, resolving = resolvingReportId == report.id, onResolve = { onResolve(report) })
                    }
                }
            }
        }

        Section(stringResource(R.string.detail_location)) {
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
                        .background(HuellaColors.Surface, HuellaShapes.Tag)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Tarjeta "Sugerencia de IA": solo informa; la decisión sigue siendo del moderador. */
@Composable
private fun AiSuggestionCard(review: AiReview?, loading: Boolean, message: UiText?, onAnalyze: () -> Unit) {
    Section(stringResource(R.string.moderation_ai_title)) {
        Column(
            Modifier
                .fillMaxWidth()
                .outlinedWhite()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (review != null) {
                val (label, color) = when (review.verdict) {
                    "APROBAR" -> stringResource(R.string.moderation_ai_approve) to HuellaColors.Primary
                    "RECHAZAR" -> stringResource(R.string.moderation_ai_reject) to HuellaColors.ErrorDark
                    else -> stringResource(R.string.moderation_ai_review) to HuellaColors.WarningDark
                }
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
                Text(
                    stringResource(R.string.moderation_ai_risk, (review.risk * 100).roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = HuellaColors.TextSecondary,
                )
                review.reasons.forEach { reason ->
                    Text(stringResource(R.string.moderation_ai_bullet, reason), style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextBody)
                }
                if (review.verdict == "RECHAZAR" && !review.suggestedReason.isNullOrBlank()) {
                    Text(
                        stringResource(R.string.moderation_ai_reason, optionLabel(review.suggestedReason.orEmpty())),
                        style = MaterialTheme.typography.bodySmall,
                        color = HuellaColors.TextSecondary,
                    )
                }
                Text(
                    stringResource(R.string.moderation_ai_only_suggestion),
                    style = MaterialTheme.typography.labelSmall,
                    color = HuellaColors.TextTertiary,
                )
            } else {
                Text(
                    stringResource(R.string.moderation_ai_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = HuellaColors.TextSecondary,
                )
            }
            if (message != null) {
                Text(message.asString(), style = MaterialTheme.typography.bodySmall, color = HuellaColors.ErrorDark)
            }
            TextButton(onClick = onAnalyze, enabled = !loading, contentPadding = PaddingValues(horizontal = 0.dp)) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = HuellaColors.Primary, strokeWidth = 2.dp)
                } else {
                    Text(
                        stringResource(if (review == null) R.string.moderation_ai_analyze else R.string.moderation_ai_again),
                        style = MaterialTheme.typography.labelLarge,
                        color = HuellaColors.PrimaryText,
                    )
                }
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
private fun ReportRow(report: Report, resolving: Boolean, onResolve: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HuellaIcon(R.drawable.icon_accion_reportar, tint = HuellaColors.Error, size = 23.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(optionLabel(report.reason), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = HuellaColors.TextPrimary)
                Text(report.who, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
            }
            if (report.detail.isNotBlank()) {
                Text(
                    report.detail,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = HuellaColors.TextSecondary,
                )
            }
            if (report.id.isNotBlank()) {
                TextButton(onClick = onResolve, enabled = !resolving, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    if (resolving) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = HuellaColors.Primary, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.moderation_resolve_report), style = MaterialTheme.typography.labelLarge, color = HuellaColors.PrimaryText)
                    }
                }
            }
        }
    }
}

/** Sticky "Aprobar" (verde) + "Rechazar" (rojo, a la derecha) bar. */
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
            DecisionButton(stringResource(R.string.moderation_approve), R.drawable.icon_accion_aprobar, HuellaColors.Primary, approving, enabled, onApprove, Modifier.weight(1f))
            DecisionButton(stringResource(R.string.action_reject), R.drawable.icon_accion_rechazar, HuellaColors.Error, false, enabled, onReject, Modifier.weight(1f))
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
    commentError: UiText?,
    rejectError: UiText?,
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
            Text(stringResource(R.string.moderation_reject_title), style = MaterialTheme.typography.titleLarge, color = HuellaColors.TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = HuellaColors.TextPrimary)
            }
        }
        val reasonLabel = stringResource(R.string.moderation_reason)
        Text(
            buildAnnotatedString {
                append(reasonLabel)
                withStyle(SpanStyle(color = HuellaColors.Error)) { append("*") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (reasonError) HuellaColors.Error else HuellaColors.TextSecondary,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(HuellaShapes.Card)
                .background(HuellaColors.Surface)
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
                    Text(optionLabel(reason), style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary)
                }
            }
        }
        if (reasonError) {
            // Texto además del color (accesibilidad, ítem 58).
            Text(stringResource(R.string.moderation_reason_required), style = MaterialTheme.typography.bodySmall, color = HuellaColors.ErrorDark)
        }
        HuellaTextField(
            value = comment,
            onValueChange = onCommentChange,
            placeholder = stringResource(R.string.moderation_comment_placeholder),
            error = commentError?.asString(),
            singleLine = false,
            minHeight = 72.dp,
        )
        if (rejectError != null) {
            Text(rejectError.asString(), style = MaterialTheme.typography.bodySmall, color = HuellaColors.ErrorDark)
        }
        PrimaryButton(
            text = stringResource(R.string.moderation_confirm_reject),
            onClick = onConfirm,
            loading = loading,
            containerColor = if (selected == null) DisabledReject else HuellaColors.Error,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Design radio: 20dp ring (#8A8F88), primary ring + dot when selected (el rojo queda para errores). */
@Composable
private fun RadioDot(selected: Boolean) {
    val color = if (selected) HuellaColors.Primary else HuellaColors.TextTertiary
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
    .background(HuellaColors.Surface)
    .border(1.dp, HuellaColors.Divider, HuellaShapes.Card)

@Preview(showBackground = true)
@Composable
fun ModerationDetailScreenPreview() {
    HuellaTheme {
        ModerationDetailScreen(
            itemId = "mod_1",
            onBack = {},
            onDone = {},
        )
    }
}
