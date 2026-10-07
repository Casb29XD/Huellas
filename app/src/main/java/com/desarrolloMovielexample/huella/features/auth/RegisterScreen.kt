package com.desarrolloMovielexample.huella.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.core.components.HuellaDropdownField
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTextField
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    CollectSnackbarMessages(viewModel.messages, snackbarHostState)
    LaunchedEffect(state.registered) {
        if (state.registered) {
            delay(700) // ponytail: let the success snackbar be seen before the stack is cleared
            onRegistered()
        }
    }
    val passwordTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation()

    Scaffold(
        topBar = { HuellaTopBar(title = "Crear cuenta", onBack = onBack) },
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HuellaTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Nombre",
                placeholder = "Mariana López",
                error = state.nameError,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            HuellaTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Correo",
                placeholder = "mariana@correo.com",
                error = state.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            HuellaDropdownField(
                value = state.city,
                options = state.cities,
                onSelect = viewModel::onCityChange,
                label = "Ciudad",
                error = state.cityError,
                modifier = Modifier.fillMaxWidth(),
            )
            HuellaTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Contraseña",
                placeholder = "Mínimo 8 caracteres",
                error = state.passwordError,
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                trailingIcon = { PasswordVisibilityToggle(state.passwordVisible, viewModel::togglePasswordVisibility) },
                modifier = Modifier.fillMaxWidth(),
            )
            HuellaTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmChange,
                label = "Confirmar contraseña",
                placeholder = "Repite tu contraseña",
                error = state.confirmError,
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            TermsCheckbox(checked = state.acceptedTerms, isError = state.termsError, onToggle = viewModel::toggleTerms)
            PrimaryButton(
                text = "Crear cuenta",
                onClick = {
                    focusManager.clearFocus()
                    viewModel.register()
                },
                loading = state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun TermsCheckbox(checked: Boolean, isError: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, HuellaShapes.Card)
            .border(1.dp, if (isError) HuellaColors.Error else HuellaColors.OutlineVariant, HuellaShapes.Card)
            .clickable(onClick = onToggle)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val boxShape = RoundedCornerShape(4.dp)
        Box(
            Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .background(if (checked) HuellaColors.Primary else Color.White, boxShape)
                .border(2.dp, if (checked) HuellaColors.Primary else if (isError) HuellaColors.Error else HuellaColors.TextTertiary, boxShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Text(
            buildAnnotatedString {
                append("Acepto los términos y condiciones. ")
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = HuellaColors.TextPrimary)) {
                    append("Entiendo que está prohibida la venta de animales.")
                }
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.5.sp),
            color = HuellaColors.TextBody,
            modifier = Modifier.weight(1f),
        )
    }
}
