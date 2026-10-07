package com.desarrolloMovielexample.huella.features.feed

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
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
import com.desarrolloMovielexample.huella.core.components.categoryIcon
import com.desarrolloMovielexample.huella.core.components.sexIcon
import com.desarrolloMovielexample.huella.core.components.sizeIcon
import com.desarrolloMovielexample.huella.core.components.speciesIcon
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.core.theme.toneColor
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.repository.randomPhotoUrl

private val VetSoft = Color(0xFFE1F2EE)

@Composable
fun PublicationDetailScreen(
    publicationId: String,
    onBack: () -> Unit,
    onOpenAuthor: (String) -> Unit,
    onOpenMap: () -> Unit,
    viewModel: PublicationDetailViewModel = viewModel(key = "detail_$publicationId") { PublicationDetailViewModel(publicationId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val screenHost = remember { SnackbarHostState() }
    val sheetHost = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            val sheetOpen = viewModel.uiState.value.sheet.let { it != null && it != DetailSheet.LLAMAR }
            (if (sheetOpen) sheetHost else screenHost).showSnackbar(msg)
        }
    }

    val pub = state.publication
    if (pub == null) {
        Scaffold(containerColor = HuellaColors.Background, topBar = { HuellaTopBar("Publicación", onBack = onBack) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    illustration = R.drawable.illus_searching,
                    title = "Publicación no disponible",
                    text = "Es posible que haya sido eliminada o cerrada por su autor.",
                    actionText = "Volver",
                    onAction = onBack,
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val share = {
        val text = "${pub.title} · ${pub.category.label} en ${pub.city}. Míralo en Huella."
        runCatching {
            context.startActivity(
                Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }, null),
            )
        }
        Unit
    }

    Scaffold(
        containerColor = HuellaColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { HuellaSnackbarHost(screenHost) },
        bottomBar = {
            ActionBar(
                pub = pub,
                requestSent = state.requestSent,
                onCta = {
                    when {
                        pub.category.isAdoption -> viewModel.openSheet(DetailSheet.INTERES)
                        pub.category.isLostFound -> viewModel.openSheet(DetailSheet.VISTO)
                        else -> onOpenMap()
                    }
                },
                onCall = { viewModel.openSheet(DetailSheet.LLAMAR) },
                onReport = { viewModel.openSheet(DetailSheet.REPORTAR) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Gallery(pub, onBack = onBack, onShare = share)
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CategoryChip(pub.category)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = HuellaColors.TextTertiary, modifier = Modifier.size(15.dp))
                    Text(pub.timeAgo, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary, modifier = Modifier.padding(start = 4.dp))
                }
                Text(
                    pub.title,
                    style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.6.sp),
                    color = HuellaColors.TextPrimary,
                )
                when {
                    pub.category.isLostFound -> LostFoundInfo(pub)
                    pub.category == Category.VETERINARIA -> VetInfo(pub)
                    else -> AdoptionInfo(pub)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Descripción", style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary)
                    Text(pub.description, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.4.sp), color = HuellaColors.TextBody)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ubicación", style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenMap),
                        contentAlignment = Alignment.Center,
                    ) {
                        MapBackground(Modifier.fillMaxSize())
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            HuellaIcon(categoryIcon(pub.category, filled = true), tint = pub.category.color, size = 36.dp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        HuellaIcon(R.drawable.icon_attr_ubicacion, tint = HuellaColors.TextSecondary, size = 21.dp)
                        Text(pub.neighborhood, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = HuellaColors.TextSecondary)
                    }
                }
                AuthorCard(pub, onOpenAuthor)
            }
        }
    }

    when (state.sheet) {
        DetailSheet.INTERES -> HuellaBottomSheet(onDismiss = viewModel::closeSheet) {
            SheetBody(sheetHost) {
                SheetTitle("Me interesa adoptar", viewModel::closeSheet)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .softCard()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StripedPlaceholder(toneColor(pub.tone), Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(pub.title, style = Medium14, color = HuellaColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Publicado por ${pub.author.name}", style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
                    }
                }
                HuellaTextField(
                    value = state.interestMessage,
                    onValueChange = viewModel::onInterestMessageChange,
                    label = "Mensaje al autor (opcional)",
                    placeholder = "Cuéntale por qué serías un buen hogar",
                    singleLine = false,
                    minHeight = 110.dp,
                )
                PrimaryButton(
                    "Enviar solicitud",
                    onClick = viewModel::sendInterest,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.isSending,
                    icon = Icons.AutoMirrored.Outlined.Send,
                )
            }
        }

        DetailSheet.REPORTAR -> HuellaBottomSheet(onDismiss = viewModel::closeSheet) {
            SheetBody(sheetHost, gap = 14.dp) {
                SheetTitle("Reportar publicación", viewModel::closeSheet)
                Text("¿Cuál es el motivo?", style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextSecondary)
                Column(Modifier.fillMaxWidth().softCard()) {
                    REPORT_REASONS.forEachIndexed { i, reason ->
                        val selected = state.reportReason == reason
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onReportReasonChange(reason) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .border(2.dp, if (selected) HuellaColors.Primary else HuellaColors.TextTertiary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(HuellaColors.Primary))
                            }
                            Text(reason, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary)
                        }
                        if (i < REPORT_REASONS.lastIndex) HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    }
                }
                HuellaTextField(
                    value = state.reportDetail,
                    onValueChange = viewModel::onReportDetailChange,
                    placeholder = "Detalle (opcional)",
                    singleLine = false,
                    minHeight = 72.dp,
                )
                PillButton(
                    "Enviar reporte",
                    onClick = viewModel::sendReport,
                    modifier = Modifier.fillMaxWidth(),
                    container = HuellaColors.Error,
                    iconRes = R.drawable.icon_accion_reportar,
                    loading = state.isSending,
                )
            }
        }

        DetailSheet.VISTO -> HuellaBottomSheet(onDismiss = viewModel::closeSheet) {
            SheetBody(sheetHost) {
                SheetTitle("Lo he visto", viewModel::closeSheet)
                HuellaTextField(
                    value = state.sightingText,
                    onValueChange = viewModel::onSightingChange,
                    label = "¿Dónde y cuándo?",
                    placeholder = "Ej: hoy a las 8 a. m. cerca de la Circular 73",
                    singleLine = false,
                    minHeight = 90.dp,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.MilitaryTech, contentDescription = null, tint = HuellaColors.Primary, modifier = Modifier.size(18.dp))
                    Text("Este aviso suma 5 puntos a tu nivel.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = HuellaColors.TextSecondary)
                }
                PrimaryButton(
                    "Enviar aviso",
                    onClick = viewModel::sendSighting,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.isSending,
                    icon = Icons.AutoMirrored.Outlined.Send,
                    containerColor = pub.category.color,
                )
            }
        }

        DetailSheet.LLAMAR -> CallDialog(
            name = pub.author.name,
            phone = pub.phone ?: "Sin teléfono registrado",
            onDismiss = viewModel::closeSheet,
            onCall = {
                viewModel.closeSheet()
                pub.phone?.let { phone ->
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${phone.replace(" ", "")}".toUri())) }
                }
            },
        )

        null -> Unit
    }
}

private val Medium14: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)

private fun Modifier.softCard(background: Color = Color.White): Modifier = this
    .clip(RoundedCornerShape(12.dp))
    .background(background)
    .border(1.dp, HuellaColors.Divider, RoundedCornerShape(12.dp))

@Composable
private fun SheetBody(host: SnackbarHostState, gap: Dp = 16.dp, content: @Composable () -> Unit) {
    Box {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) { content() }
        HuellaSnackbarHost(host, Modifier.align(Alignment.BottomCenter))
    }
}

/** 4:3 pager with 3 photos, back + share buttons, "foto n/3" label and page dots. */
@Composable
private fun Gallery(pub: Publication, onBack: () -> Unit, onShare: () -> Unit) {
    val urls = listOf(pub.imageUrl, randomPhotoUrl("${pub.id}b"), randomPhotoUrl("${pub.id}c"))
    val pager = rememberPagerState(pageCount = { urls.size })
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
    ) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            PetImage(urls[page], toneColor(pub.tone), Modifier.fillMaxSize(), contentDescription = pub.title)
        }
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás", tint = HuellaColors.TextPrimary)
            }
            Text(
                "foto ${pager.currentPage + 1}/${urls.size}: ${pub.photoLabel}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                color = HuellaColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(start = 56.dp, top = 2.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center,
            ) {
                HuellaIcon(R.drawable.icon_accion_compartir, tint = HuellaColors.TextPrimary, size = 29.dp)
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(urls.size) { i ->
                val active = i == pager.currentPage
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (active) 20.dp else 8.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        .background(if (active) Color.White else Color.White.copy(alpha = 0.5f)),
                )
            }
        }
    }
}

/** Two-column grid of equal-height cells; [cell] renders cell [index] with the given modifier. */
@Composable
private fun InfoGrid(count: Int, cell: @Composable (index: Int, modifier: Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        (0 until count).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { i -> cell(i, Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class InfoItem(@DrawableRes val icon: Int, val label: String, val value: String)

@Composable
private fun IconCell(item: InfoItem, tint: Color, modifier: Modifier) {
    Row(
        modifier
            .softCard()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HuellaIcon(item.icon, tint = tint, size = 31.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.label, style = MaterialTheme.typography.labelSmall, color = HuellaColors.TextTertiary)
            Text(item.value, style = Medium14, color = HuellaColors.TextPrimary)
        }
    }
}

@Composable
private fun AdoptionInfo(pub: Publication) {
    val items = buildList {
        add(InfoItem(speciesIcon(pub.species), "Tipo", pub.species))
        add(InfoItem(R.drawable.icon_attr_raza_aproximada, "Raza aproximada", pub.breed))
        add(InfoItem(sizeIcon(pub.size), "Tamaño", pub.size))
        add(
            InfoItem(
                if (pub.vaccinated) R.drawable.icon_attr_vacunado else R.drawable.icon_attr_sin_vacunas,
                "Vacunas",
                if (pub.vaccinated) "Al día" else "Pendientes",
            ),
        )
        if (pub.sterilized != null) add(InfoItem(R.drawable.icon_attr_vacunado, "Castración", if (pub.sterilized == true) "Sí" else "No"))
    }
    InfoGrid(items.size) { i, m -> IconCell(items[i], HuellaColors.Primary, m) }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        pub.age?.let { InlineAttr(R.drawable.icon_attr_edad, it) }
        pub.sex?.let { InlineAttr(sexIcon(it), it) }
    }
}

@Composable
private fun InlineAttr(@DrawableRes icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HuellaIcon(icon, tint = HuellaColors.TextSecondary, size = 23.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextBody)
    }
}

@Composable
private fun LostFoundInfo(pub: Publication) {
    val found = pub.category == Category.ENCONTRADOS
    val bg = if (found) HuellaColors.InfoContainer else HuellaColors.ErrorContainer
    val fg = if (found) HuellaColors.InfoDark else HuellaColors.ErrorDark
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HuellaIcon(R.drawable.icon_attr_fecha, tint = fg, size = 31.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (found) "Fecha y lugar del hallazgo" else "Fecha y lugar de la pérdida",
                style = MaterialTheme.typography.labelMedium,
                color = fg.copy(alpha = 0.8f),
            )
            Text(pub.eventDate.orEmpty(), style = MaterialTheme.typography.titleSmall, color = fg)
            Text(pub.place ?: pub.neighborhood, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = fg)
        }
    }
    val items = listOf(
        InfoItem(speciesIcon(pub.species), "Tipo", pub.species),
        InfoItem(R.drawable.icon_attr_raza_aproximada, "Raza aproximada", pub.breed),
        InfoItem(sizeIcon(pub.size), "Tamaño", pub.size),
        InfoItem(sexIcon(pub.sex), "Sexo", pub.sex ?: "Desconocido"),
    )
    InfoGrid(items.size) { i, m -> IconCell(items[i], pub.category.color, m) }
}

@Composable
private fun TextCell(label: String, value: String, modifier: Modifier) {
    Column(modifier.softCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = HuellaColors.TextTertiary)
        Text(value, style = Medium14, color = HuellaColors.TextPrimary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VetInfo(pub: Publication) {
    InfoGrid(4) { i, m ->
        when (i) {
            0 -> TextCell("Fecha", pub.eventDate.orEmpty(), m)
            1 -> TextCell("Horario", pub.schedule.orEmpty(), m)
            2 -> TextCell("Lugar", pub.place ?: pub.neighborhood, m)
            else -> Column(
                m.clip(RoundedCornerShape(12.dp)).background(VetSoft).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Cupo disponible", style = MaterialTheme.typography.labelSmall, color = HuellaColors.CatVeterinaria)
                Text(
                    "${pub.capacity ?: 0} lugares",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                    color = HuellaColors.CatVeterinaria,
                )
            }
        }
    }
    if (pub.services.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetLabel("Servicios")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pub.services.forEach { s ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(VetSoft)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = HuellaColors.CatVeterinaria, modifier = Modifier.size(16.dp))
                        Text(s, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), color = HuellaColors.CatVeterinaria)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorCard(pub: Publication, onOpenAuthor: (String) -> Unit) {
    val author = pub.author
    Row(
        Modifier
            .fillMaxWidth()
            .softCard()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(author.initials, author.level, size = 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(author.name, style = Medium14, color = HuellaColors.TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HuellaIcon(R.drawable.icon_animal_otro, tint = author.level.color, size = 18.dp)
                Text(author.level.label, style = MaterialTheme.typography.labelMedium, color = author.level.color)
            }
        }
        Text(
            "Ver perfil",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            color = HuellaColors.Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenAuthor(author.userId) }
                .padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ActionBar(
    pub: Publication,
    requestSent: Boolean,
    onCta: () -> Unit,
    onCall: () -> Unit,
    onReport: () -> Unit,
) {
    Column(Modifier.background(HuellaColors.Background)) {
        HorizontalDivider(color = HuellaColors.Divider, thickness = 1.dp)
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val cat = pub.category
            when {
                cat.isAdoption && requestSent -> PillButton(
                    "Solicitud enviada ✓", onClick = {}, modifier = Modifier.weight(1f), enabled = false,
                    container = HuellaColors.Disabled, content = HuellaColors.TextTertiary,
                    iconRes = R.drawable.icon_accion_solicitud_enviada, iconSize = 29.dp,
                )
                cat.isAdoption -> PillButton(
                    "Me interesa adoptar", onClick = onCta, modifier = Modifier.weight(1f),
                    iconRes = R.drawable.icon_accion_me_interesa_adoptar,
                )
                cat.isLostFound -> {
                    PillButton("Lo he visto", onClick = onCta, modifier = Modifier.weight(1f), container = cat.color, icon = Icons.Outlined.Visibility)
                    PillButton(
                        "Llamar", onClick = onCall, modifier = Modifier.weight(1f),
                        container = Color.Transparent, content = cat.color, border = cat.color,
                        iconRes = R.drawable.icon_accion_llamar,
                    )
                }
                else -> PillButton("Cómo llegar", onClick = onCta, modifier = Modifier.weight(1f), container = HuellaColors.CatVeterinaria, icon = Icons.Outlined.Directions)
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, HuellaColors.Outline, CircleShape)
                    .clickable(onClick = onReport),
                contentAlignment = Alignment.Center,
            ) {
                HuellaIcon(R.drawable.icon_accion_reportar, tint = HuellaColors.TextSecondary, size = 29.dp)
            }
        }
    }
}

/** 48dp pill with either a design PNG icon ([iconRes]) or a Material icon. */
@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = HuellaColors.Primary,
    content: Color = Color.White,
    border: Color? = null,
    @DrawableRes iconRes: Int? = null,
    iconSize: Dp = 26.dp,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier
            .height(48.dp)
            .clip(shape)
            .background(container)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = content, strokeWidth = 2.dp)
        } else {
            if (iconRes != null) HuellaIcon(iconRes, tint = content, size = iconSize)
            if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            if (iconRes != null || icon != null) Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
        }
    }
}

@Composable
private fun CallDialog(name: String, phone: String, onDismiss: () -> Unit, onCall: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(HuellaColors.Background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HuellaIcon(R.drawable.icon_accion_llamar, tint = HuellaColors.Primary, size = 36.dp)
            Text("Llamar a $name", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = HuellaColors.TextPrimary)
            Text(phone, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextSecondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Cancelar",
                    style = Medium14,
                    color = HuellaColors.Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
                Text(
                    "Llamar",
                    style = Medium14,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(HuellaColors.Primary)
                        .clickable(onClick = onCall)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}
