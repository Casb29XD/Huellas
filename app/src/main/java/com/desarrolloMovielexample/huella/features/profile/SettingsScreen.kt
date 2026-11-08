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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.Avatar
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaSwitch
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.utils.AppLanguage
import com.desarrolloMovielexample.huella.domain.model.User

@Suppress("DEPRECATION") // Icons.Outlined.Logout: the AutoMirrored variant is not needed for an LTR-only app.
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    user: User? = null,
    onOpenTerms: () -> Unit = {},
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf(AppLanguage.selected(context)) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(resources)) } }

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
        topBar = { HuellaTopBar(stringResource(R.string.settings_title), Modifier.statusBarsPadding(), onBack = onBack) },
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
                GroupCaption(stringResource(R.string.settings_notifications))
                Column(Modifier.fillMaxWidth().outlinedCard()) {
                    ToggleRow(stringResource(R.string.settings_push), stringResource(R.string.settings_push_summary), state.pushEnabled, viewModel::setPush)
                    HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    ToggleRow(stringResource(R.string.settings_email_digest), stringResource(R.string.settings_email_digest_summary), state.emailDigestEnabled, viewModel::setEmailDigest)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupCaption(stringResource(R.string.settings_language_section))
                Column(Modifier.fillMaxWidth().outlinedCard()) {
                    val languageLabel = when (selectedLanguage) {
                        AppLanguage.SPANISH -> stringResource(R.string.language_spanish)
                        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                        else -> stringResource(R.string.language_system)
                    }
                    LinkRow(
                        text = stringResource(R.string.settings_language),
                        onClick = { showLanguageDialog = true },
                        subtitle = languageLabel,
                    ) {
                        Icon(Icons.Outlined.Language, contentDescription = null, tint = HuellaColors.TextSecondary, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupCaption(stringResource(R.string.settings_account))
                Column(Modifier.fillMaxWidth().outlinedCard()) {
                    if (user != null) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Avatar(user.initials, user.level, size = 40.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(user.name, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary)
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
                            }
                        }
                        HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    }
                    LinkRow(stringResource(R.string.settings_edit_profile), onEditProfile) {
                        HuellaIcon(R.drawable.icon_nav_perfil, tint = HuellaColors.TextSecondary, size = 29.dp)
                    }
                    HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    LinkRow(stringResource(R.string.settings_change_password), onChangePassword) {
                        HuellaIcon(R.drawable.icon_estado_cerrada, tint = HuellaColors.TextSecondary, size = 29.dp)
                    }
                    HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    LinkRow(stringResource(R.string.settings_terms), onOpenTerms) {
                        Icon(Icons.Outlined.Gavel, contentDescription = null, tint = HuellaColors.TextSecondary, modifier = Modifier.size(22.dp))
                    }
                    HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteDialog = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = HuellaColors.ErrorDark, modifier = Modifier.size(22.dp))
                        Text(
                            stringResource(R.string.settings_delete_account),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
                            color = HuellaColors.ErrorDark,
                        )
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
                    stringResource(R.string.settings_logout),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
                    color = HuellaColors.ErrorDark,
                )
            }


            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(R.string.settings_delete_title)) },
                    text = { Text(stringResource(R.string.settings_delete_text)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                viewModel.deleteAccount(onSuccess = onLoggedOut)
                            }
                        ) {
                            Text(stringResource(R.string.settings_delete_confirm), color = HuellaColors.ErrorDark)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
            Text(
                stringResource(R.string.settings_footer),
                style = MaterialTheme.typography.bodySmall,
                color = HuellaColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language_dialog_title)) },
            text = {
                Column {
                    LanguageOption(
                        title = stringResource(R.string.language_system),
                        selected = selectedLanguage == AppLanguage.SYSTEM,
                        onClick = {
                            AppLanguage.set(context, AppLanguage.SYSTEM)
                            selectedLanguage = AppLanguage.SYSTEM
                            showLanguageDialog = false
                            AppLanguage.findActivity(context)?.recreate()
                        },
                    )
                    LanguageOption(
                        title = stringResource(R.string.language_spanish),
                        selected = selectedLanguage == AppLanguage.SPANISH,
                        onClick = {
                            AppLanguage.set(context, AppLanguage.SPANISH)
                            selectedLanguage = AppLanguage.SPANISH
                            showLanguageDialog = false
                            AppLanguage.findActivity(context)?.recreate()
                        },
                    )
                    LanguageOption(
                        title = stringResource(R.string.language_english),
                        selected = selectedLanguage == AppLanguage.ENGLISH,
                        onClick = {
                            AppLanguage.set(context, AppLanguage.ENGLISH)
                            selectedLanguage = AppLanguage.ENGLISH
                            showLanguageDialog = false
                            AppLanguage.findActivity(context)?.recreate()
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            containerColor = HuellaColors.Background,
        )
    }
}

@Composable
private fun LanguageOption(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary)
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    HuellaTheme {
        SettingsScreen(
            onBack = {},
            onEditProfile = {},
            onChangePassword = {},
            onLoggedOut = {},
        )
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
private fun LinkRow(text: String, onClick: () -> Unit, subtitle: String? = null, icon: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = HuellaColors.TextTertiary, modifier = Modifier.size(22.dp))
    }
}
