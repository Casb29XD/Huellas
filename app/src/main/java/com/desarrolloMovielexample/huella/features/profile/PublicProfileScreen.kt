package com.desarrolloMovielexample.huella.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.components.LevelBadge
import com.desarrolloMovielexample.huella.core.components.PublicationRowCard
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import com.desarrolloMovielexample.huella.core.theme.color

private const val CONTACT_CONVERSATION_ID = "c1" // ponytail: only conversation in FakeRepository.

@Composable
fun PublicProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenPublication: (String) -> Unit,
    onContact: (String) -> Unit,
    viewModel: PublicProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(userId) { viewModel.load(userId) }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }
    val user = state.user

    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás", tint = HuellaColors.TextPrimary)
                }
                if (user != null) {
                    IconButton(onClick = viewModel::report) {
                        HuellaIcon(R.drawable.icon_accion_reportar, tint = HuellaColors.TextSecondary, size = 31.dp)
                    }
                }
            }
        },
        bottomBar = {
            if (user != null) {
                Column(
                    Modifier
                        .background(HuellaColors.Background)
                        .navigationBarsPadding(),
                ) {
                    HorizontalDivider(color = HuellaColors.Divider, thickness = 1.dp)
                    Button(
                        onClick = { onContact(CONTACT_CONVERSATION_ID) },
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = HuellaShapes.Button,
                        colors = ButtonDefaults.buttonColors(containerColor = HuellaColors.Primary, contentColor = Color.White),
                    ) {
                        HuellaIcon(R.drawable.icon_accion_mensaje, tint = Color.White, size = 26.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Contactar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
    ) { inner ->
        if (user == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isLoaded) {
                    Text("No encontramos este perfil", style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextSecondary)
                }
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileAvatar(user.initials, user.level.color)
                Text(user.name, style = MaterialTheme.typography.titleLarge, color = HuellaColors.TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HuellaIcon(R.drawable.icon_attr_ubicacion, tint = HuellaColors.TextSecondary, size = 21.dp)
                    Text(
                        "${user.city.substringBefore(",")} · Miembro desde ${user.memberSince}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = HuellaColors.TextSecondary,
                    )
                }
                LevelBadge(user.level, soft = true)
            }
            StatsRow(
                listOf(
                    user.activeCount.toString() to "Activas",
                    user.adoptionsCount.toString() to "Adopciones",
                    user.points.toString() to "Puntos",
                ),
                Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            )
            Text(
                "Publicaciones activas",
                style = MaterialTheme.typography.titleMedium,
                color = HuellaColors.TextPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
            )
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.publications.isEmpty()) {
                    Text("No tiene publicaciones activas.", style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextSecondary)
                }
                state.publications.forEach { p ->
                    PublicationRowCard(publication = p, onClick = { onOpenPublication(p.id) })
                }
            }
        }
    }
}
