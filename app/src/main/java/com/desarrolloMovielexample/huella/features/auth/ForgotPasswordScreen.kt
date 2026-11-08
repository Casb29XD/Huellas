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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
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
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.utils.asString

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
                    title = stringResource(R.string.forgot_sent_title),
                    text = stringResource(R.string.forgot_sent_text, state.email.trim()),
                    illustrationSize = 220.dp,
                    actionText = stringResource(R.string.forgot_back_to_login),
                    onAction = onBack,
                    secondaryActionText = stringResource(R.string.forgot_other_email),
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
                    stringResource(R.string.forgot_title),
                    stringResource(R.string.forgot_subtitle),
                )
                HuellaTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = stringResource(R.string.field_email),
                    placeholder = stringResource(R.string.field_email_placeholder),
                    error = state.emailError?.asString(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        focusManager.clearFocus()
                        viewModel.sendLink()
                    }),
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton(
                    text = stringResource(R.string.forgot_send),
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.sendLink()
                    },
                    loading = state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                HuellaTextButton(
                    text = stringResource(R.string.forgot_back_to_login),
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    HuellaTheme {
        ForgotPasswordScreen(onBack = {})
    }
}
