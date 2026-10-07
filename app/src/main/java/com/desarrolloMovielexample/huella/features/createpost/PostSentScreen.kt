package com.desarrolloMovielexample.huella.features.createpost

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.HuellaTextButton
import com.desarrolloMovielexample.huella.core.components.ModerationStatusTag
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.domain.model.PostStatus

@Composable
fun PostSentScreen(
    onViewMyPublications: () -> Unit,
    onGoHome: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Success feedback for the create-post form (the form itself is popped on success).
    LaunchedEffect(Unit) { snackbarHostState.showSnackbar("Publicación enviada") }

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
        bottomBar = {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryButton("Ver mis publicaciones", onViewMyPublications, Modifier.fillMaxWidth())
                HuellaTextButton("Volver al inicio", onGoHome, Modifier.fillMaxWidth().height(48.dp))
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Image(painterResource(R.drawable.illus_message_sent), contentDescription = null, modifier = Modifier.size(260.dp))
            Text(
                "Tu publicación está en revisión",
                style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.6.sp),
                color = HuellaColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                "Un moderador la revisará en las próximas horas. Te avisaremos cuando esté aprobada y sumarás 10 puntos.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.4.sp),
                color = HuellaColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            ModerationStatusTag(PostStatus.EN_REVISION)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PostSentScreenPreview() {
    HuellaTheme {
        PostSentScreen(
            onViewMyPublications = {},
            onGoHome = {},
        )
    }
}
