package com.desarrolloMovielexample.huella.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes

/** "Selector segmentado": 44dp pill, outlined, selected segment #DCEBE2 with check. */
@Composable
fun SegmentedSelector(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(HuellaShapes.Button)
            .border(1.dp, HuellaColors.Outline, HuellaShapes.Button),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelected) HuellaColors.PrimaryContainer else Color.Transparent)
                    .clickable { onSelect(option) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelected) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = HuellaColors.TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    option,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = if (isSelected) HuellaColors.TextPrimary else HuellaColors.TextSecondary,
                )
            }
            if (index < options.lastIndex) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(HuellaColors.OutlineVariant),
                )
            }
        }
    }
}

/** Underlined tabs (Enviadas/Recibidas, Mis publicaciones/Historial, Pendientes/Reportes). */
@Composable
fun HuellaTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        tab,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
                        color = if (selected) HuellaColors.Primary else HuellaColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (selected) HuellaColors.Primary else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = HuellaColors.Divider, thickness = 1.dp)
    }
}

/** Green Material switch matching the design toggles. */
@Composable
fun HuellaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = HuellaColors.Primary,
            checkedBorderColor = HuellaColors.Primary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = HuellaColors.OutlineVariant,
            uncheckedBorderColor = HuellaColors.Outline,
        ),
    )
}

/** Screen header: back arrow + 22/500 title (+ optional small overline above the title, e.g. "Nueva publicación · 1/3"). */
@Composable
fun HuellaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    overline: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = if (onBack != null) 4.dp else 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás", tint = HuellaColors.TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(overline, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextSecondary)
            }
            Text(
                title,
                style = if (overline != null) MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp) else MaterialTheme.typography.titleLarge,
                color = HuellaColors.TextPrimary,
                maxLines = 1,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Snackbar host used by every form for validation / success feedback. */
@Composable
fun HuellaSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = HuellaShapes.Card,
            containerColor = HuellaColors.TextPrimary,
            contentColor = Color.White,
            actionColor = HuellaColors.PrimaryContainer,
        )
    }
}

/** Uppercase section caption (e.g. "NOTIFICACIONES", "CUENTA" in Ajustes). */
@Composable
fun SectionCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.66.sp),
        color = HuellaColors.TextTertiary,
        modifier = modifier,
    )
}
