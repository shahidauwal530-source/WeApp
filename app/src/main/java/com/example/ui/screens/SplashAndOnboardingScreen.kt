package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GlobalLanguages
import com.example.localization.AppStrings
import com.example.ui.components.WeAppGradientButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    strings: AppStrings,
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
        delay(1100)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0718), Color(0xFF1A103C), Color(0xFF0B1538))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_weapp_foreground),
                contentDescription = strings.appName,
                modifier = Modifier
                    .size(116.dp)
                    .scale(scale.value)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = strings.appName,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.slogan,
                style = MaterialTheme.typography.titleMedium,
                color = WePurpleLight,
                textAlign = TextAlign.Center
            )
            Surface(
                color = WeBlue.copy(alpha = 0.22f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = strings.tagline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}

private data class OnboardingPage(
    val imageRes: Int,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(
    strings: AppStrings,
    currentLanguage: String,
    onToggleLanguage: (String) -> Unit,
    onFinishOnboarding: () -> Unit
) {
    val pages = remember(strings) {
        listOf(
            OnboardingPage(R.drawable.img_onboard_social, strings.onboardTitle1, strings.onboardDesc1),
            OnboardingPage(R.drawable.img_onboard_video_game, strings.onboardTitle2, strings.onboardDesc2),
            OnboardingPage(R.drawable.img_onboard_earn, strings.onboardTitle3, strings.onboardDesc3)
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(GlobalLanguages.supported) { (code, label) ->
                        FilterChip(
                            selected = currentLanguage == code,
                            onClick = { onToggleLanguage(code) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("onboard_lang_$code")
                        )
                    }
                }

                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(strings.skip, color = WePurpleLight)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = page.imageRes),
                        contentDescription = page.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(28.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { idx ->
                        val active = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (active) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) WeBrandGradient
                                    else Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.outline,
                                            MaterialTheme.colorScheme.outline
                                        )
                                    )
                                )
                        )
                    }
                }

                val isLast = pagerState.currentPage == pages.lastIndex
                WeAppGradientButton(
                    text = if (isLast) strings.getStarted else strings.next,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "onboarding_next_button",
                    onClick = {
                        if (isLast) {
                            onFinishOnboarding()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                )
            }
        }
    }
}
