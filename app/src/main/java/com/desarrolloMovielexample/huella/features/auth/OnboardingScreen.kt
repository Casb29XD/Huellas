package com.desarrolloMovielexample.huella.features.auth

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import kotlinx.coroutines.launch

private data class OnboardingPage(@DrawableRes val illustration: Int, val title: String, val text: String)

private val pages = listOf(
    OnboardingPage(
        R.drawable.illus_animal,
        "Adopta, no compres",
        "Miles de perros y gatos en Colombia esperan una familia. Encuéntralos cerca de ti.",
    ),
    OnboardingPage(
        R.drawable.illus_location,
        "Ayuda a mascotas perdidas a volver a casa",
        "Publica avistamientos, revisa el mapa y contacta a la familia en minutos.",
    ),
    OnboardingPage(
        R.drawable.illus_cool_guy,
        "Sube de nivel ayudando",
        "Cada adopción, hogar temporal o reporte suma puntos. De Amigo Animal a Héroe de las Mascotas.",
    ),
)

private val DotInactive = Color(0xFFD3DBD5)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(HuellaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "Omitir",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                color = HuellaColors.Primary,
                modifier = Modifier
                    .clickable(onClick = onFinish)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(painterResource(page.illustration), contentDescription = null, modifier = Modifier.size(300.dp))
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        page.title,
                        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 28.6.sp),
                        color = HuellaColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        page.text,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                        color = HuellaColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { i ->
                    val selected = i == pagerState.currentPage
                    val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dotWidth")
                    val color by animateColorAsState(if (selected) HuellaColors.Primary else DotInactive, label = "dotColor")
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(width)
                            .background(color, RoundedCornerShape(4.dp)),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            PrimaryButton(
                text = if (isLast) "Comenzar" else "Siguiente",
                onClick = {
                    if (isLast) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    HuellaTheme {
        OnboardingScreen(onFinish = {})
    }
}
