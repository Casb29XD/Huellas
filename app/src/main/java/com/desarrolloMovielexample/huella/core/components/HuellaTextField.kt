package com.desarrolloMovielexample.huella.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes

/** Field label above the box: 12/500 #3F4A44 (red when in error). */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, isError: Boolean = false, trailing: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) HuellaColors.ErrorDark else HuellaColors.TextBody,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
        }
    }
}

/**
 * "Campo de texto" from the components sheet: label above, 52dp white box, 12dp radius,
 * 1dp #C7CBC6 border (green when focused, 2dp #D64545 + message on error).
 */
@Composable
fun HuellaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    error: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minHeight: Dp = 52.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    labelTrailing: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val isError = error != null
    val borderColor = when {
        isError -> HuellaColors.Error
        focused -> HuellaColors.Primary
        else -> HuellaColors.Outline
    }
    val borderWidth = if (isError) 2.dp else 1.dp
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) FieldLabel(label, isError = isError, trailing = labelTrailing)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = 1,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = HuellaColors.TextPrimary),
            cursorBrush = SolidColor(HuellaColors.Primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = minHeight)
                        .background(Color.White, HuellaShapes.Field)
                        .border(borderWidth, borderColor, HuellaShapes.Field)
                        .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 14.dp),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                ) {
                    if (leadingIcon != null) {
                        leadingIcon()
                        Spacer(Modifier.width(10.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextTertiary)
                        }
                        inner()
                    }
                    if (trailingIcon != null) {
                        Spacer(Modifier.width(8.dp))
                        trailingIcon()
                    }
                }
            },
        )
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = HuellaColors.ErrorDark)
        }
    }
}

/** Select box (Ciudad, etc.): same look as [HuellaTextField] with chevron and a dropdown menu. */
@Composable
fun HuellaDropdownField(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "Selecciona",
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val isError = error != null
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) FieldLabel(label, isError = isError)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .background(Color.White, HuellaShapes.Field)
                    .border(if (isError) 2.dp else 1.dp, if (isError) HuellaColors.Error else HuellaColors.Outline, HuellaShapes.Field)
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    value.ifEmpty { placeholder },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isEmpty()) HuellaColors.TextTertiary else HuellaColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = HuellaColors.TextSecondary)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Color.White) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = HuellaColors.ErrorDark)
        }
    }
}
