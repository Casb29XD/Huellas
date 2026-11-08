package com.desarrolloMovielexample.huella.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.utils.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Shows every one-shot message from a ViewModel in the screen's snackbar (newest replaces the current one). */
@Composable
internal fun CollectSnackbarMessages(messages: Flow<UiText>, hostState: SnackbarHostState) {
    val resources = LocalContext.current.resources
    LaunchedEffect(messages) {
        messages.collect { message ->
            hostState.currentSnackbarData?.dismiss()
            launch { hostState.showSnackbar(message.resolve(resources)) }
        }
    }
}

/** Logo (120dp) + title 22/500 + subtitle 14/400, as in "Iniciar sesión". */
@Composable
internal fun AuthHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.logo_huella),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(280f / 240f),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = HuellaColors.TextPrimary, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

/** Eye icon (22dp, #5F6A63) toggling password visibility. */
@Composable
internal fun PasswordVisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    Icon(
        imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
        contentDescription = stringResource(if (visible) R.string.password_hide else R.string.password_show),
        tint = HuellaColors.TextSecondary,
        modifier = Modifier
            .size(22.dp)
            .clickable(onClick = onToggle),
    )
}
