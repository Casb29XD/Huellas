package com.desarrolloMovielexample.huella.features.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.theme.HuellaColors

/** Términos y política de adopción: texto estático versionado en res/raw/terminos.txt. */
@Composable
fun TermsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val text = remember {
        context.resources.openRawResource(R.raw.terminos).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
    Scaffold(
        topBar = { HuellaTopBar(title = stringResource(R.string.terms_title), onBack = onBack) },
        containerColor = HuellaColors.Background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = HuellaColors.TextBody,
            )
        }
    }
}
