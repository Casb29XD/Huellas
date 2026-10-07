package com.desarrolloMovielexample.huella.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.core.components.HuellaDropdownField
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTextField
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.color
import kotlinx.coroutines.delay

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }
    // Let the "Cambios guardados" snackbar be seen before leaving.
    LaunchedEffect(state.saved) {
        if (state.saved) {
            delay(1_200)
            onSaved()
        }
    }

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
        topBar = { HuellaTopBar("Editar perfil", Modifier.statusBarsPadding(), onBack = onBack) },
        bottomBar = {
            Column(
                Modifier
                    .background(HuellaColors.Background)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                HorizontalDivider(color = HuellaColors.Divider, thickness = 1.dp)
                PrimaryButton(
                    text = "Guardar cambios",
                    onClick = viewModel::save,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                    loading = state.isLoading,
                    enabled = !state.saved,
                )
            }
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                ProfileAvatar(state.initials, state.level.color, size = 96.dp, ringWidth = 0.dp)
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(HuellaColors.Primary)
                        .border(3.dp, HuellaColors.Background, CircleShape)
                        .clickable(onClick = viewModel::onPhotoClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Cambiar foto", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            HuellaTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Nombre",
                error = state.nameError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            HuellaTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Correo",
                error = state.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            HuellaDropdownField(
                value = state.city,
                options = state.cities,
                onSelect = viewModel::onCityChange,
                label = "Ciudad",
                error = state.cityError,
            )
            HuellaTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                label = "Teléfono de contacto",
                error = state.phoneError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            )
            HuellaTextField(
                value = state.bio,
                onValueChange = viewModel::onBioChange,
                label = "Sobre ti",
                singleLine = false,
                minHeight = 90.dp,
            )
        }
    }
}
