package com.desarrolloMovielexample.huella.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.EmptyState
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTextButton
import com.desarrolloMovielexample.huella.core.components.HuellaTextField
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.theme.HuellaColors

/** Not in the design: same visual language as "Iniciar sesión" (logo header, 52dp fields, green pill). */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    CollectSnackbarMessages(viewModel.messages, snackbarHostState)

    Scaffold(
        topBar = { HuellaTopBar(title = "", onBack = onBack) },
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
    ) { innerPadding ->
        if (state.emailSent) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    illustration = R.drawable.illus_message_sent,
                    title = "Revisa tu correo",
                    text = "Enviamos un enlace a ${state.email.trim()} para que crees una nueva contraseña. Si no lo ves, revisa la carpeta de spam.",
                    illustrationSize = 220.dp,
                    actionText = "Volver a iniciar sesión",
                    onAction = onBack,
                    secondaryActionText = "Usar otro correo",
                    onSecondaryAction = viewModel::editEmail,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                AuthHeader(
                    "Recupera tu contraseña",
                    "Escribe el correo de tu cuenta y te enviaremos un enlace para crear una nueva.",
                )
                HuellaTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Correo",
                    placeholder = "mariana@correo.com",
                    error = state.emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        focusManager.clearFocus()
                        viewModel.sendLink()
                    }),
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton(
                    text = "Enviar enlace",
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.sendLink()
                    },
                    loading = state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                HuellaTextButton(
                    text = "Volver a iniciar sesión",
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
