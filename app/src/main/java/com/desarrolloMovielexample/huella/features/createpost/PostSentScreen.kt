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
import androidx.compose.ui.res.stringResource
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
    val sentMessage = stringResource(R.string.msg_post_sent)
    LaunchedEffect(Unit) { snackbarHostState.showSnackbar(sentMessage) }

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
        bottomBar = {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryButton(stringResource(R.string.post_sent_view_mine), onViewMyPublications, Modifier.fillMaxWidth())
                HuellaTextButton(stringResource(R.string.post_sent_home), onGoHome, Modifier.fillMaxWidth().height(48.dp))
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
                stringResource(R.string.post_sent_title),
                style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.6.sp),
                color = HuellaColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.post_sent_text),
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
