package com.desarrolloMovielexample.huella.core.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.theme.HuellaColors

enum class BottomBarItem(val label: String, @DrawableRes val icon: Int) {
    INICIO("Inicio", R.drawable.icon_nav_inicio),
    BUSCAR("Buscar", R.drawable.icon_nav_buscar),
    PUBLICAR("Publicar", R.drawable.icon_nav_publicar),
    SOLICITUDES("Solicitudes", R.drawable.icon_nav_solicitudes),
    PERFIL("Perfil", R.drawable.icon_nav_perfil),
}

/**
 * "Barra de navegación inferior": white 80dp bar, top hairline, 5 items.
 * Selected item: green icon inside a #DCEBE2 pill. Center "Publicar" = raised green circle with "+".
 */
@Composable
fun HuellaBottomBar(
    selected: BottomBarItem?,
    onItemClick: (BottomBarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = HuellaColors.Divider, thickness = 1.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomBarItem.entries.forEach { item ->
                val isSelected = item == selected
                val color = if (isSelected) HuellaColors.Primary else HuellaColors.TextSecondary
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onItemClick(item) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (item == BottomBarItem.PUBLICAR) {
                        // The PNG is a circle with a transparent "+": white disc behind, icon tinted green on top.
                        Box(
                            Modifier
                                .offset(y = (-10).dp)
                                .size(56.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(49.dp)
                                    .shadow(10.dp, CircleShape, ambientColor = HuellaColors.Primary, spotColor = HuellaColors.Primary)
                                    .background(Color.White, CircleShape),
                            )
                            Icon(painterResource(item.icon), contentDescription = item.label, tint = HuellaColors.Primary, modifier = Modifier.requiredSize(73.dp))
                        }
                        Text(item.label, style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.offset(y = (-10).dp))
                    } else {
                        Box(
                            Modifier
                                .background(if (isSelected) HuellaColors.PrimaryContainer else Color.Transparent, RoundedCornerShape(16.dp))
                                .padding(horizontal = 18.dp, vertical = 4.dp),
                        ) {
                            Icon(painterResource(item.icon), contentDescription = item.label, tint = color, modifier = Modifier.size(31.dp))
                        }
                        Text(item.label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
                    }
                }
            }
        }
    }
}
