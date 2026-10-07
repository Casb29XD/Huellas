package com.desarrolloMovielexample.huella.features.feed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.Avatar
import com.desarrolloMovielexample.huella.core.components.EmptyState
import com.desarrolloMovielexample.huella.core.components.FeedSkeleton
import com.desarrolloMovielexample.huella.core.components.HuellaDropdownField
import com.desarrolloMovielexample.huella.core.components.HuellaFilterChip
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaSwitch
import com.desarrolloMovielexample.huella.core.components.LevelBadge
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.components.PublicationCard
import com.desarrolloMovielexample.huella.core.components.SecondaryButton
import com.desarrolloMovielexample.huella.core.components.SegmentedSelector
import com.desarrolloMovielexample.huella.core.components.categoryIcon
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.domain.model.Category

@Composable
fun HomeScreen(
    onOpenPublication: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onCreatePost: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            when {
                state.isLoading -> FeedSkeleton()
                state.publications.isEmpty() -> NoResults(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onUpdateFilters = viewModel::updateFilters,
                    onCategory = viewModel::onCategorySelected,
                    onClear = viewModel::clearAll,
                    onCreateAlert = viewModel::createAlert,
                )
                else -> FeedList(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onOpenFilters = { showFilters = true },
                    onCategory = viewModel::onCategorySelected,
                    onUpdateFilters = viewModel::updateFilters,
                    onOpenPublication = onOpenPublication,
                    onOpenMap = onOpenMap,
                    onOpenNotifications = onOpenNotifications,
                    onOpenProfile = onOpenProfile,
                )
            }
        }
    }

    if (showFilters) {
        FiltersSheet(
            initial = state.filters,
            species = state.species,
            cities = state.cities,
            onDismiss = { showFilters = false },
            onApply = {
                showFilters = false
                viewModel.applyFilters(it)
            },
            onClear = {
                showFilters = false
                viewModel.clearAll()
            },
        )
    }
}

@Composable
private fun FeedList(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onCategory: (Category?) -> Unit,
    onUpdateFilters: (FeedFilters) -> Unit,
    onOpenPublication: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Hola, ${state.userFirstName}", style = MaterialTheme.typography.titleLarge, color = HuellaColors.TextPrimary)
                    LevelBadge(state.userLevel, soft = true)
                }
                Box {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = HuellaColors.TextPrimary)
                    }
                    if (state.hasUnreadNotifications) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 9.dp, end = 9.dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(HuellaColors.Background)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(HuellaColors.Error),
                        )
                    }
                }
                Spacer(Modifier.size(4.dp))
                Avatar(state.userInitials, state.userLevel, Modifier.clickable(onClick = onOpenProfile), size = 40.dp)
            }
        }
        item {
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    onOpenFilters = onOpenFilters,
                    active = false,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, HuellaColors.OutlineVariant, CircleShape)
                        .clickable(onClick = onOpenMap),
                    contentAlignment = Alignment.Center,
                ) {
                    HuellaIcon(R.drawable.icon_accion_mapa, tint = HuellaColors.Primary, size = 29.dp)
                }
            }
        }
        item {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryFilterChip("Todas", R.drawable.icon_animal_otro, HuellaColors.TextPrimary, state.category == null) { onCategory(null) }
                Category.entries.forEach { c ->
                    CategoryFilterChip(c.label, categoryIcon(c), c.color, state.category == c) { onCategory(c) }
                }
            }
        }
        if (state.filters.isActive) {
            item { ActiveFilterChips(state.filters, onUpdateFilters, Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) }
        }
        item {
            val n = state.publications.size
            Text(
                "$n ${if (n == 1) "publicación" else "publicaciones"} cerca de Medellín",
                style = MaterialTheme.typography.bodySmall,
                color = HuellaColors.TextTertiary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
            )
        }
        items(state.publications, key = { it.id }) { p ->
            PublicationCard(
                publication = p,
                onClick = { onOpenPublication(p.id) },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            )
        }
    }
}

/** "Feed sin resultados": active search field + removable filter chips + empty state. */
@Composable
private fun NoResults(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onUpdateFilters: (FeedFilters) -> Unit,
    onCategory: (Category?) -> Unit,
    onClear: () -> Unit,
    onCreateAlert: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            onOpenFilters = null,
            active = true,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        )
        if (state.filters.isActive || state.category != null) {
            ActiveFilterChips(
                state.filters,
                onUpdateFilters,
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                category = state.category,
                onRemoveCategory = { onCategory(null) },
            )
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                illustration = R.drawable.illus_searching,
                title = "No encontramos mascotas",
                text = "Prueba con otra ciudad o quita algún filtro. También puedes activar alertas para esta búsqueda.",
                illustrationSize = 230.dp,
                actionText = "Limpiar filtros",
                onAction = onClear,
                secondaryActionText = "Crear alerta",
                onSecondaryAction = onCreateAlert,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenFilters: (() -> Unit)?,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, if (active) HuellaColors.Primary else HuellaColors.OutlineVariant, RoundedCornerShape(24.dp))
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HuellaIcon(R.drawable.icon_nav_buscar, tint = HuellaColors.TextSecondary, size = 29.dp)
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = HuellaColors.TextPrimary),
            cursorBrush = SolidColor(HuellaColors.Primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Buscar mascotas, razas, ciudades",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HuellaColors.TextTertiary,
                            maxLines = 2,
                        )
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Borrar búsqueda",
                tint = HuellaColors.TextSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onQueryChange("") }
                    .padding(6.dp)
                    .size(22.dp),
            )
        }
        if (onOpenFilters != null) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onOpenFilters)
                    .padding(6.dp),
            ) {
                HuellaIcon(R.drawable.icon_accion_filtros, tint = HuellaColors.Primary, size = 29.dp)
            }
        }
    }
}

/** Feed category chip: selected = solid category color (Todas = #1F2A24) + white text. */
@Composable
private fun CategoryFilterChip(
    text: String,
    @DrawableRes icon: Int,
    selectedColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val fg = if (selected) Color.White else HuellaColors.TextBody
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .height(34.dp)
            .clip(shape)
            .background(if (selected) selectedColor else Color.White)
            .border(1.dp, if (selected) Color.Transparent else ChipBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HuellaIcon(icon, tint = fg, size = 21.dp)
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp))
    }
}

private val ChipBorder = Color(0xFFD9DED9)

/** Removable chips for the applied filters ("Conejo ×", "Cartagena ×"). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterChips(
    filters: FeedFilters,
    onUpdate: (FeedFilters) -> Unit,
    modifier: Modifier = Modifier,
    category: Category? = null,
    onRemoveCategory: () -> Unit = {},
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (category != null) HuellaFilterChip(category.label, selected = true, onClick = onRemoveCategory, onRemove = onRemoveCategory)
        filters.species?.let { HuellaFilterChip(it, true, { onUpdate(filters.copy(species = null)) }, onRemove = { onUpdate(filters.copy(species = null)) }) }
        filters.size?.let { HuellaFilterChip(it, true, { onUpdate(filters.copy(size = null)) }, onRemove = { onUpdate(filters.copy(size = null)) }) }
        if (filters.onlyVaccinated) {
            HuellaFilterChip("Vacunados", true, { onUpdate(filters.copy(onlyVaccinated = false)) }, onRemove = { onUpdate(filters.copy(onlyVaccinated = false)) })
        }
        if (filters.onlySterilized) {
            HuellaFilterChip("Castrados", true, { onUpdate(filters.copy(onlySterilized = false)) }, onRemove = { onUpdate(filters.copy(onlySterilized = false)) })
        }
        filters.city?.let {
            HuellaFilterChip(it.substringBefore(","), true, { onUpdate(filters.copy(city = null)) }, onRemove = { onUpdate(filters.copy(city = null)) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltersSheet(
    initial: FeedFilters,
    species: List<String>,
    cities: List<String>,
    onDismiss: () -> Unit,
    onApply: (FeedFilters) -> Unit,
    onClear: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    HuellaBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SheetTitle("Filtros", onDismiss)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetLabel("Tipo de animal")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    species.forEach { s ->
                        HuellaFilterChip(s, selected = draft.species == s, onClick = {
                            draft = draft.copy(species = if (draft.species == s) null else s)
                        })
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetLabel("Tamaño")
                SegmentedSelector(
                    options = listOf("Pequeño", "Mediano", "Grande"),
                    selected = draft.size.orEmpty(),
                    onSelect = { draft = draft.copy(size = if (draft.size == it) null else it) },
                )
            }
            SwitchRow("Solo con vacunas", draft.onlyVaccinated) { draft = draft.copy(onlyVaccinated = it) }
            SwitchRow("Solo castrados", draft.onlySterilized) { draft = draft.copy(onlySterilized = it) }
            HuellaDropdownField(
                value = draft.city?.substringBefore(",").orEmpty(),
                options = listOf(ALL_CITIES) + cities,
                onSelect = { draft = draft.copy(city = if (it == ALL_CITIES) null else it) },
                label = "Ciudad",
                placeholder = ALL_CITIES,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SheetLabel("Distancia")
                    Text("${draft.distanceKm.toInt()} km", style = MaterialTheme.typography.labelMedium, color = HuellaColors.Primary)
                }
                Slider(
                    value = draft.distanceKm,
                    onValueChange = { draft = draft.copy(distanceKm = it) },
                    valueRange = 1f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = HuellaColors.Primary,
                        activeTrackColor = HuellaColors.Primary,
                        inactiveTrackColor = HuellaColors.OutlineVariant,
                    ),
                )
            }
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Limpiar", onClick = onClear, modifier = Modifier.weight(1f))
                PrimaryButton("Aplicar", onClick = { onApply(draft) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

private const val ALL_CITIES = "Todas las ciudades"

@Composable
private fun SwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary, modifier = Modifier.weight(1f))
        HuellaSwitch(checked, onChange)
    }
}

// ---------- Bottom-sheet scaffolding shared by the feed feature ----------

/** Design sheet: #FAFAF7, top radius 28, 32x4 handle, scrim rgba(31,42,36,.45). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HuellaBottomSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = HuellaColors.Background,
        scrimColor = Color(0x731F2A24),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HuellaColors.Outline),
            )
        },
    ) {
        Box(Modifier.imePadding()) { content() }
    }
}

@Composable
internal fun SheetTitle(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = HuellaColors.TextPrimary, modifier = Modifier.weight(1f))
        Icon(
            Icons.Outlined.Close,
            contentDescription = "Cerrar",
            tint = HuellaColors.TextPrimary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClose)
                .padding(6.dp)
                .size(24.dp),
        )
    }
}

@Composable
internal fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = HuellaColors.TextBody)
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HuellaTheme {
        HomeScreen(
            onOpenPublication = {},
            onOpenMap = {},
            onOpenNotifications = {},
            onOpenProfile = {},
            onCreatePost = {},
        )
    }
}
