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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.ColombiaLocationFields
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTextField
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.utils.asString
import kotlinx.coroutines.flow.filter

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
    onOpenTerms: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    // Validation problems are shown under each field (and the terms box); the snackbar only carries the rest.
    val messages = remember(viewModel) { viewModel.messages.filter { !viewModel.uiState.value.hasInlineErrors } }
    CollectSnackbarMessages(messages, snackbarHostState)
    LaunchedEffect(state.registered) {
        if (state.registered) onRegistered()
    }
    val passwordTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation()

    Scaffold(
        topBar = { HuellaTopBar(title = stringResource(R.string.register_title), onBack = onBack) },
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
                label = stringResource(R.string.field_name),
                placeholder = stringResource(R.string.field_name_placeholder),
                error = state.nameError?.asString(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            HuellaTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(R.string.field_email),
                placeholder = stringResource(R.string.field_email_placeholder),
                error = state.emailError?.asString(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            ColombiaLocationFields(
                value = state.city,
                onValueChange = viewModel::onCityChange,
                label = stringResource(R.string.field_department),
                municipalityLabel = stringResource(R.string.field_city),
                error = state.cityError?.asString(),
            )
            HuellaTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = stringResource(R.string.field_password),
                placeholder = stringResource(R.string.field_password_min),
                error = state.passwordError?.asString(),
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                trailingIcon = { PasswordVisibilityToggle(state.passwordVisible, viewModel::togglePasswordVisibility) },
                modifier = Modifier.fillMaxWidth(),
            )
            HuellaTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmChange,
                label = stringResource(R.string.field_confirm_password),
                placeholder = stringResource(R.string.field_confirm_placeholder),
                error = state.confirmError?.asString(),
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TermsCheckbox(checked = state.acceptedTerms, isError = state.termsError, onToggle = viewModel::toggleTerms)
                Text(
                    stringResource(R.string.register_read_terms),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = HuellaColors.PrimaryText,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onOpenTerms),
                )
                if (state.termsError) {
                    Text(stringResource(R.string.register_terms_required), style = MaterialTheme.typography.bodySmall, color = HuellaColors.ErrorDark)
                }
            }
            PrimaryButton(
                text = stringResource(R.string.register_title),
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
            .background(HuellaColors.Surface, HuellaShapes.Card)
            .border(1.dp, if (isError) HuellaColors.Error else HuellaColors.OutlineVariant, HuellaShapes.Card)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val boxShape = RoundedCornerShape(4.dp)
        Box(
            Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .background(if (checked) HuellaColors.Primary else HuellaColors.Surface, boxShape)
                .border(2.dp, if (checked) HuellaColors.Primary else if (isError) HuellaColors.Error else HuellaColors.TextTertiary, boxShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        val accept = stringResource(R.string.register_terms_accept)
        val noSale = stringResource(R.string.register_terms_no_sale)
        Text(
            buildAnnotatedString {
                append(accept)
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = HuellaColors.TextPrimary)) {
                    append(noSale)
                }
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.5.sp),
            color = HuellaColors.TextBody,
            modifier = Modifier.weight(1f),
        )
    }
}

private val RegisterUiState.hasInlineErrors: Boolean
    get() = termsError || listOfNotNull(nameError, emailError, cityError, passwordError, confirmError).isNotEmpty()

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    HuellaTheme {
        RegisterScreen(
            onBack = {},
            onRegistered = {},
        )
    }
}
