package com.desarrolloMovielexample.huella.features.requests

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddPhotoAlternate
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.Avatar
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.domain.model.ChatMessage

@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    LaunchedEffect(conversationId) { viewModel.load(conversationId) }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }
    // +1 for the "Hoy" chip at index 0.
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size) }

    val conversation = state.conversation
    Scaffold(
        snackbarHost = { HuellaSnackbarHost(snackbarHostState) },
        containerColor = HuellaColors.Background,
        topBar = {
            Column(
                Modifier
                    .background(Color.White)
                    .statusBarsPadding(),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás", tint = HuellaColors.TextPrimary)
                    }
                    if (conversation != null) {
                        Avatar(conversation.contact.initials, conversation.contact.level, size = 40.dp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            conversation?.contact?.name ?: "Conversación",
                            style = MaterialTheme.typography.titleSmall,
                            color = HuellaColors.TextPrimary,
                            maxLines = 1,
                        )
                        if (conversation != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                HuellaIcon(R.drawable.icon_animal_otro, tint = HuellaColors.TextTertiary, size = 18.dp)
                                Text(conversation.about, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary, maxLines = 1)
                            }
                        }
                    }
                    if (conversation != null && conversation.phone.isNotBlank()) {
                        IconButton(onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${conversation.phone}".toUri()))
                            } catch (e: ActivityNotFoundException) {
                                // ponytail: device without dialer, nothing to open.
                            }
                        }) {
                            HuellaIcon(R.drawable.icon_accion_llamar, tint = HuellaColors.Primary, size = 31.dp)
                        }
                    }
                }
                HorizontalDivider(color = HuellaColors.Divider, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                HorizontalDivider(color = HuellaColors.Divider, thickness = 1.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Adjuntar foto",
                        tint = HuellaColors.TextSecondary,
                        modifier = Modifier.padding(8.dp).size(24.dp),
                    )
                    BasicTextField(
                        value = state.text,
                        onValueChange = viewModel::onTextChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = HuellaColors.TextPrimary),
                        cursorBrush = SolidColor(HuellaColors.Primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.send() }),
                        decorationBox = { inner ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(HuellaColors.Background)
                                    .border(1.dp, HuellaColors.OutlineVariant, RoundedCornerShape(22.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (state.text.isEmpty()) {
                                    Text("Escribe un mensaje", style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextTertiary)
                                }
                                inner()
                            }
                        },
                    )
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(HuellaColors.Primary)
                            .clickable { viewModel.send() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        },
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().padding(bottom = 6.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Hoy",
                        style = MaterialTheme.typography.labelSmall,
                        color = HuellaColors.TextTertiary,
                        modifier = Modifier
                            .background(HuellaColors.SurfaceMuted, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            items(state.messages, key = { it.id }) { MessageBubble(it) }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val align = if (message.fromMe) Alignment.End else Alignment.Start
    val shape = if (message.fromMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 4.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Column(Modifier.fillMaxWidth(0.78f), horizontalAlignment = align, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.3.sp),
                color = if (message.fromMe) Color.White else HuellaColors.TextPrimary,
                modifier = Modifier
                    .shadow(1.dp, shape)
                    .background(if (message.fromMe) HuellaColors.Primary else Color.White, shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Text(message.time, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = HuellaColors.TextTertiary)
        }
    }
}
