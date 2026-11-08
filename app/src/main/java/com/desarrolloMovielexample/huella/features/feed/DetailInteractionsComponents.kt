package com.desarrolloMovielexample.huella.features.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.desarrolloMovielexample.huella.core.components.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desarrolloMovielexample.huella.core.components.Avatar
import com.desarrolloMovielexample.huella.core.components.HuellaTextField
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.domain.model.Comment
import com.desarrolloMovielexample.huella.domain.model.Publication

@Composable
fun VotesAndCommentsRow(
    publication: Publication,
    onVote: (Int) -> Unit,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HuellaColors.Surface)
            .border(1.dp, HuellaColors.Divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Votos (Upvote / Downvote)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val upColor = if (publication.userVote == 1) HuellaColors.Primary else HuellaColors.TextSecondary
            IconButton(onClick = { onVote(1) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.ThumbUp, contentDescription = "Me gusta", tint = upColor, modifier = Modifier.size(18.dp))
            }
            Text(
                publication.votesCount.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = HuellaColors.TextPrimary
            )
            val downColor = if (publication.userVote == -1) HuellaColors.ErrorDark else HuellaColors.TextSecondary
            IconButton(onClick = { onVote(-1) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.ThumbDown, contentDescription = "No me gusta", tint = downColor, modifier = Modifier.size(18.dp))
            }
        }

        // Comentarios
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenComments)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comentarios", tint = HuellaColors.TextSecondary, modifier = Modifier.size(18.dp))
            Text(
                "${publication.commentsCount} comentarios",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = HuellaColors.TextSecondary
            )
        }
    }
}

@Composable
fun CommentsSection(
    comments: List<Comment>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onDeleteComment: (String) -> Unit,
    canDeleteAny: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Comentarios (${comments.size})", style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary)

        // Entrada de nuevo comentario
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HuellaTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = "Escribe un comentario o pregunta...",
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (draft.isNotBlank()) HuellaColors.Primary else HuellaColors.SurfaceMuted)
                    .clickable(enabled = draft.isNotBlank(), onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Enviar",
                    tint = if (draft.isNotBlank()) Color.White else HuellaColors.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Lista de comentarios
        if (comments.isEmpty()) {
            Text(
                "Aún no hay comentarios. ¡Sé el primero en preguntar!",
                style = MaterialTheme.typography.bodyMedium,
                color = HuellaColors.TextTertiary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HuellaColors.Surface)
                    .border(1.dp, HuellaColors.Divider, RoundedCornerShape(12.dp))
            ) {
                comments.forEachIndexed { index, comment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Avatar(initials = comment.author.initials, level = comment.author.level, size = 34.dp)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(comment.author.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp), color = HuellaColors.TextPrimary)
                                Text(comment.timeAgo, style = MaterialTheme.typography.labelSmall, color = HuellaColors.TextTertiary)
                            }
                            Text(comment.text, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = HuellaColors.TextBody)
                        }
                        if (comment.isMine || canDeleteAny) {
                            IconButton(onClick = { onDeleteComment(comment.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar", tint = HuellaColors.TextTertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (index < comments.lastIndex) {
                        HorizontalDivider(color = HuellaColors.SurfaceMuted, thickness = 1.dp)
                    }
                }
            }
        }
    }
}
