package com.desarrolloMovielexample.huella.features.messages

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.Avatar
import com.desarrolloMovielexample.huella.core.components.EmptyState
import com.desarrolloMovielexample.huella.core.components.HuellaTopBar
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.domain.model.Conversation
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository

/** Bandeja de conversaciones. El repositorio las carga al iniciar sesión y al aceptar una solicitud. */
// ponytail: sin ViewModel, la pantalla solo lee una lista que el repositorio ya mantiene.
@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    repo: HuellaRepository = SupabaseRepository,
) {
    val conversations by repo.conversationList.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = HuellaColors.Background,
        topBar = { HuellaTopBar(stringResource(R.string.messages_title), Modifier.statusBarsPadding(), onBack = onBack) },
    ) { inner ->
        if (conversations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner).padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                EmptyState(
                    illustration = R.drawable.illus_empty_inbox,
                    title = stringResource(R.string.messages_empty_title),
                    text = stringResource(R.string.messages_empty_body),
                    illustrationSize = 230.dp,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(conversations, key = { it.id }) { ConversationRow(it, onClick = { onOpenChat(it.id) }) }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HuellaColors.Surface)
            .border(1.dp, HuellaColors.Divider, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(conversation.contact.initials, conversation.contact.level, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(conversation.contact.name, style = MaterialTheme.typography.titleSmall, color = HuellaColors.TextPrimary, maxLines = 1)
            if (conversation.about.isNotBlank()) {
                Text(
                    conversation.about,
                    style = MaterialTheme.typography.bodySmall,
                    color = HuellaColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
