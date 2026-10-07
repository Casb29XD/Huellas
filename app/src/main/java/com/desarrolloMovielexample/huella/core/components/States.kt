package com.desarrolloMovielexample.huella.core.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes

/**
 * "Estado vacío": illustration, title 16/500, text 14/400 #5F6A63, optional 44dp green action
 * and optional text action below (e.g. "Limpiar filtros" + "Crear alerta").
 * Illustrations: R.drawable.illus_searching, illus_relationship, illus_empty_inbox, illus_tasks,
 * illus_connection_lost, illus_message_sent...
 */
@Composable
fun EmptyState(
    @DrawableRes illustration: Int,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    illustrationSize: Dp = 200.dp,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    actionOutlined: Boolean = false,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Column(
        modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painterResource(illustration), contentDescription = null, modifier = Modifier.size(illustrationSize))
        Text(title, style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = HuellaColors.TextSecondary, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) {
            if (actionOutlined) {
                SecondaryButton(actionText, onAction, height = 44.dp, icon = actionIcon)
            } else {
                PrimaryButton(actionText, onAction, height = 44.dp, icon = actionIcon)
            }
        }
        if (secondaryActionText != null && onSecondaryAction != null) {
            HuellaTextButton(secondaryActionText, onSecondaryAction)
        }
    }
}

/** Red "Sin conexión a internet" banner. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier, text: String = "Sin conexión a internet") {
    Row(
        modifier
            .fillMaxWidth()
            .background(HuellaColors.ErrorContainer, HuellaShapes.Card)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = HuellaColors.ErrorDark, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = HuellaColors.ErrorDark)
    }
}

/** "Error de conexión": offline banner + connection-lost illustration + "Reintentar". */
@Composable
fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "No pudimos cargar las publicaciones",
    text: String = "Revisa tu conexión e inténtalo de nuevo. Las publicaciones guardadas siguen disponibles sin internet.",
    showBanner: Boolean = true,
) {
    Column(modifier.fillMaxSize()) {
        if (showBanner) OfflineBanner(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            EmptyState(
                illustration = R.drawable.illus_connection_lost,
                title = title,
                text = text,
                actionText = "Reintentar",
                onAction = onRetry,
                actionIcon = Icons.Outlined.Refresh,
            )
        }
    }
}

/** Pulsing grey block used to build skeletons. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = HuellaShapes.Card) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .background(HuellaColors.Divider, shape),
    )
}

/** Card-sized skeleton block (height ≈ a feed card). */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    SkeletonBox(
        modifier
            .fillMaxWidth()
            .height(200.dp),
    )
}

/** "Estado de carga (skeleton)" of the feed: header lines + avatar, search pill, chips, two cards. */
@Composable
fun FeedSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(Modifier.width(140.dp).height(18.dp), RoundedCornerShape(9.dp))
                SkeletonBox(Modifier.width(84.dp).height(14.dp), RoundedCornerShape(7.dp))
            }
            SkeletonBox(Modifier.size(40.dp), CircleShape)
        }
        SkeletonBox(Modifier.fillMaxWidth().height(44.dp), HuellaShapes.Button)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60.dp, 80.dp, 76.dp, 90.dp).forEach { w ->
                SkeletonBox(Modifier.width(w).height(32.dp), RoundedCornerShape(10.dp))
            }
        }
        SkeletonCard(Modifier.height(250.dp))
        SkeletonCard(Modifier.height(250.dp))
    }
}
