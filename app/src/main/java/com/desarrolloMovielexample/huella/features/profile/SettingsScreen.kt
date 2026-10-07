package com.desarrolloMovielexample.huella.features.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaSwitch
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.theme.HuellaColors

@Suppress("DEPRECATION") // Icons.Outlined.Logout: the AutoMirrored variant is not needed for an LTR-only app.
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
        topBar = { HuellaTopBar("Ajustes", Modifier.statusBarsPadding(), onBack = onBack) },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupCaption("Notificaciones")
                Column(Modifier.fillMaxWidth().outlinedCard()) {
                    ToggleRow("Notificaciones push", "Solicitudes, moderación y niveles", state.pushEnabled, viewModel::setPush)
                    HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    ToggleRow("Resumen por correo", "Semanal, con mascotas cerca de ti", state.emailDigestEnabled, viewModel::setEmailDigest)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupCaption("Cuenta")
                Column(Modifier.fillMaxWidth().outlinedCard()) {
                    LinkRow("Editar perfil", onEditProfile) {
                        HuellaIcon(R.drawable.icon_nav_perfil, tint = HuellaColors.TextSecondary, size = 29.dp)
                    }
                    HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    LinkRow("Cambiar contraseña", onChangePassword) {
                        HuellaIcon(R.drawable.icon_estado_cerrada, tint = HuellaColors.TextSecondary, size = 29.dp)
                    }
                    HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    LinkRow("Términos y política de adopción", viewModel::onTerms) {
                        Icon(Icons.Outlined.Gavel, contentDescription = null, tint = HuellaColors.TextSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .outlinedCard()
                    .clickable {
                        viewModel.logout()
                        onLoggedOut()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null, tint = HuellaColors.ErrorDark, modifier = Modifier.size(22.dp))
                Text(
                    "Cerrar sesión",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
                    color = HuellaColors.ErrorDark,
                )
            }
            Text(
                "Huella 1.0 · Hecho en Colombia",
                style = MaterialTheme.typography.bodySmall,
                color = HuellaColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
        }
        HuellaSwitch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Text(text, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = HuellaColors.TextTertiary, modifier = Modifier.size(22.dp))
    }
}
