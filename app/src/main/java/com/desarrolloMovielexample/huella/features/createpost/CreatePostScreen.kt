package com.desarrolloMovielexample.huella.features.createpost

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AddLocation
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Publish
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.FieldLabel
import com.desarrolloMovielexample.huella.core.components.HuellaDropdownField
import com.desarrolloMovielexample.huella.core.components.HuellaFilterChip
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaSwitch
import com.desarrolloMovielexample.huella.core.components.HuellaTextField
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.components.PetImage
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.components.categoryIcon
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.repository.randomPhotoUrl

/** Photo tile tones from the design (#D8E8DD, #E8DAD6, #D8E0EC, #E4DCEC, #EEE3D2). */
private val PhotoTones = listOf(Color(0xFFD8E8DD), Color(0xFFE8DAD6), Color(0xFFD8E0EC), Color(0xFFE4DCEC), Color(0xFFEEE3D2))

@Composable
fun CreatePostScreen(
    onBack: () -> Unit,
    onPublished: () -> Unit,
    viewModel: CreatePostViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(state.published) { if (state.published) onPublished() }
    LaunchedEffect(state.step) { scroll.scrollTo(0) }

    val goBack = { if (!viewModel.previousStep()) onBack() }
    BackHandler(enabled = state.step > 1) { viewModel.previousStep() }

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
        bottomBar = {
            Column(Modifier.background(HuellaColors.Background).navigationBarsPadding().imePadding()) {
                HorizontalDivider(color = HuellaColors.Divider, thickness = 1.dp)
                if (state.step < 3) {
                    ContinueButton(
                        text = "Continuar",
                        onClick = viewModel::nextStep,
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                        trailingArrow = true,
                    )
                } else {
                    PrimaryButton(
                        text = "Publicar",
                        onClick = viewModel::publish,
                        loading = state.isLoading,
                        icon = Icons.Outlined.Publish,
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            HuellaTopBar(
                title = state.stepTitle,
                overline = "Nueva publicación · ${state.step}/3",
                onBack = goBack,
            )
            val progress by animateFloatAsState(state.step / 3f, label = "progress")
            Box(
                Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 6.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HuellaColors.OutlineVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(HuellaColors.Primary),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (state.step == 3) 18.dp else 20.dp),
            ) {
                when (state.step) {
                    1 -> StepCategoryAndPhotos(state, viewModel)
                    2 -> StepPetData(state, viewModel)
                    else -> StepDescriptionAndLocation(state, viewModel)
                }
            }
        }
    }
}

// core PrimaryButton has a leading icon only; design "Continuar →" has a trailing arrow.
@Composable
private fun ContinueButton(text: String, onClick: () -> Unit, modifier: Modifier, trailingArrow: Boolean) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = HuellaShapes.Button,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = HuellaColors.Primary, contentColor = Color.White),
        elevation = null,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (trailingArrow) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

// ---------------- Paso 1 · Categoría y fotos ----------------

@Composable
private fun StepCategoryAndPhotos(state: CreatePostUiState, vm: CreatePostViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("¿Qué quieres publicar?", style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary)
        Category.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { cat ->
                    CategoryCard(cat, selected = cat == state.category, onClick = { vm.onCategory(cat) }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fotos", style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("Hasta $MAX_PHOTOS · ${MAX_PHOTOS - state.photos.size} restantes", style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
        }
        val tiles: List<Int?> = state.photos + if (state.photos.size < MAX_PHOTOS) listOf(null) else emptyList()
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { seed ->
                    val m = Modifier.weight(1f).aspectRatio(1f)
                    if (seed == null) {
                        AddPhotoTile(onClick = vm::addPhoto, modifier = m)
                    } else {
                        val index = state.photos.indexOf(seed)
                        PhotoTile(seed, index, onRemove = { vm.removePhoto(seed) }, modifier = m)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = HuellaShapes.Card
    Column(
        modifier
            .defaultMinSize(minHeight = 96.dp)
            .clip(shape)
            .background(if (selected) category.color.copy(alpha = 0.08f) else Color.White)
            .border(1.5.dp, if (selected) category.color else HuellaColors.OutlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HuellaIcon(categoryIcon(category, filled = selected), tint = category.color, size = 39.dp)
        Text(category.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = HuellaColors.TextPrimary)
    }
}

@Composable
private fun PhotoTile(seed: Int, index: Int, onRemove: () -> Unit, modifier: Modifier) {
    Box(modifier.clip(HuellaShapes.Card)) {
        // ponytail: random picsum photo instead of a real picker (image upload is out of scope this phase).
        PetImage(
            imageUrl = randomPhotoUrl("post$seed"),
            tone = PhotoTones[index.coerceIn(0, PhotoTones.lastIndex)],
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Foto ${index + 1}",
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(HuellaColors.TextPrimary.copy(alpha = 0.7f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Text(
            "foto ${index + 1}",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = HuellaColors.TextSecondary),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .clip(HuellaShapes.Card)
            .background(Color.White)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = HuellaColors.Primary,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = HuellaColors.Primary, modifier = Modifier.size(28.dp))
        Text("Agregar", style = MaterialTheme.typography.labelMedium, color = HuellaColors.Primary)
    }
}

// ---------------- Paso 2 · Datos de la mascota ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepPetData(state: CreatePostUiState, vm: CreatePostViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FieldLabel("Tipo de animal")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.species.forEach { s -> HuellaFilterChip(s, selected = s == state.species, onClick = { vm.onSpecies(s) }) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HuellaTextField(
            value = state.breed,
            onValueChange = vm::onBreed,
            label = "Raza aproximada",
            placeholder = "Ej: Criollo",
            error = state.breedError,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.breedSuggestions.forEach { b ->
                Text(
                    b,
                    style = MaterialTheme.typography.bodySmall,
                    color = HuellaColors.TextBody,
                    modifier = Modifier
                        .clip(HuellaShapes.Chip)
                        .background(HuellaColors.SurfaceMuted)
                        .clickable { vm.onBreed(b) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FieldLabel("Tamaño")
        PlainSegmented(listOf("Pequeño", "Mediano", "Grande"), state.size, vm::onSize)
    }
    ToggleRow("Tiene vacunas", "Se mostrará el badge “Vacunado”", state.vaccinated, vm::onVaccinated)
    ToggleRow("Está castrado", "Se mostrará el badge “Castrado”", state.sterilized, vm::onSterilized)
    HuellaTextField(
        value = state.age,
        onValueChange = vm::onAge,
        label = "Edad aproximada",
        placeholder = "Ej: 2 años",
        error = state.ageError,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FieldLabel("Sexo")
        PlainSegmented(listOf("Macho", "Hembra", "Desconocido"), state.sex, vm::onSex)
    }
}

/** Design's form segmented control: like core SegmentedSelector but without the check mark. */
@Composable
private fun PlainSegmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(HuellaShapes.Button)
            .border(1.dp, HuellaColors.Outline, HuellaShapes.Button),
    ) {
        options.forEachIndexed { i, option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (on) HuellaColors.PrimaryContainer else Color.Transparent)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = if (on) HuellaColors.TextPrimary else HuellaColors.TextSecondary,
                )
            }
            if (i < options.lastIndex) Box(Modifier.width(1.dp).fillMaxHeight().background(HuellaColors.OutlineVariant))
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(HuellaShapes.Card)
            .background(Color.White)
            .border(1.dp, HuellaColors.Divider, HuellaShapes.Card)
            .clickable { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
        }
        HuellaSwitch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------------- Paso 3 · Descripción y ubicación ----------------

@Composable
private fun StepDescriptionAndLocation(state: CreatePostUiState, vm: CreatePostViewModel) {
    HuellaTextField(
        value = state.title,
        onValueChange = vm::onTitle,
        label = "Título",
        placeholder = "Ej: Luna busca un hogar tranquilo",
        error = state.titleError,
    )
    HuellaTextField(
        value = state.description,
        onValueChange = vm::onDescription,
        label = "Descripción",
        placeholder = "Carácter, salud, historia, qué tipo de hogar necesita…",
        error = state.descriptionError,
        singleLine = false,
        minHeight = 120.dp,
    )
    HuellaDropdownField(value = state.city, options = vm.cities, onSelect = vm::onCity, label = "Ciudad")
    // ponytail: no map this phase — plain text location field in place of the mini-map.
    HuellaTextField(
        value = state.location,
        onValueChange = vm::onLocation,
        label = "Ubicación en el mapa",
        labelTrailing = "Opcional",
        placeholder = "Marcar ubicación (barrio o referencia)",
        leadingIcon = { Icon(Icons.Outlined.AddLocation, contentDescription = null, tint = HuellaColors.Primary, modifier = Modifier.size(18.dp)) },
    )
    if (state.needsDate) {
        HuellaTextField(
            value = state.eventDate,
            onValueChange = vm::onEventDate,
            label = state.dateLabel,
            placeholder = "Sáb 5 sep 2026 · 6:30 p. m.",
            error = state.eventDateError,
            trailingIcon = { HuellaIcon(R.drawable.icon_attr_fecha, tint = HuellaColors.TextSecondary, size = 29.dp) },
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(HuellaColors.WarningContainer, HuellaShapes.Card)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Gavel, contentDescription = null, tint = HuellaColors.WarningDark, modifier = Modifier.size(22.dp))
        Text(
            buildAnnotatedString {
                append("Las publicaciones son revisadas por moderadores. ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("Está prohibida la venta de animales.") }
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.5.sp),
            color = Color(0xFF5C3D00),
        )
    }
}
