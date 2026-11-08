package com.desarrolloMovielexample.huella.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTextField
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import kotlinx.coroutines.delay

/** Mismo lenguaje visual que "Recupera tu contraseña": logo, campos de 52dp y botón verde. */
@Composable
fun SetPasswordScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: SetPasswordViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    CollectSnackbarMessages(viewModel.messages, snackbarHostState)
    LaunchedEffect(state.saved) {
        if (state.saved) {
            delay(800) // deja ver "Contraseña actualizada"
            onDone()
        }
    }
    val transformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation()

    Scaffold(
        topBar = { HuellaTopBar(title = "", onBack = onBack) },
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AuthHeader("Crea una nueva contraseña", "Úsala la próxima vez que inicies sesión en Huella.")
            HuellaTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Nueva contraseña",
                placeholder = "Mínimo 8 caracteres",
                error = state.passwordError,
                visualTransformation = transformation,
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
                visualTransformation = transformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.save()
                }),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = "Guardar contraseña",
                onClick = {
                    focusManager.clearFocus()
                    viewModel.save()
                },
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
