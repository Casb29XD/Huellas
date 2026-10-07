package com.desarrolloMovielexample.huella.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTextField
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    CollectSnackbarMessages(viewModel.messages, snackbarHostState)
    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) {
            delay(700) // ponytail: let the welcome snackbar be seen before the stack is cleared
            onLoginSuccess()
        }
    }

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AuthHeader("Bienvenido de nuevo", "Ingresa para seguir ayudando")

            HuellaTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Correo",
                placeholder = "mariana@correo.com",
                error = state.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HuellaTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Contraseña",
                    placeholder = "••••••••",
                    error = state.passwordError,
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.login()
                    }),
                    trailingIcon = { PasswordVisibilityToggle(state.passwordVisible, viewModel::togglePasswordVisibility) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Olvidé mi contraseña",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = HuellaColors.Primary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onForgotPassword),
                )
            }

            PrimaryButton(
                text = "Ingresar",
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login()
                },
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = HuellaColors.OutlineVariant)
                Text("o", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = HuellaColors.TextTertiary)
                HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = HuellaColors.OutlineVariant)
            }

            GoogleButton(onClick = viewModel::onGoogleClick)

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("¿No tienes cuenta? ", style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextSecondary)
                Text(
                    "Crear cuenta",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = HuellaColors.Primary,
                    modifier = Modifier.clickable(onClick = onRegister),
                )
            }
        }
    }
}

/** White outlined pill with the circled "G" mark, as in the design. */
@Composable
private fun GoogleButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = HuellaShapes.Button,
        border = BorderStroke(1.dp, HuellaColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = HuellaColors.TextPrimary),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .border(1.5.dp, HuellaColors.TextPrimary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("G", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        }
        Spacer(Modifier.width(10.dp))
        Text("Continuar con Google", style = MaterialTheme.typography.labelLarge)
    }
}
