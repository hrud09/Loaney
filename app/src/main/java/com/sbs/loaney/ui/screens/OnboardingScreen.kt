package com.sbs.loaney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import com.sbs.loaney.ui.components.OnboardingIllustration
import com.sbs.loaney.ui.components.OnboardingIllustrationType
import com.sbs.loaney.ui.components.bounce
import com.sbs.loaney.ui.theme.*

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val pages = remember(isDark) {
        listOf(
            OnboardingPage(
                title = "Welcome to Loaney",
                description = "Track all your lent and borrowed money in one secure place.",
                type = OnboardingIllustrationType.WELCOME,
                bgColor = if (isDark) Color(0xFF0D0D12) else AlimCream
            ),
            OnboardingPage(
                title = "Never Forget a Debt",
                description = "Keep a clear history of your transactions so you always know who owes who.",
                type = OnboardingIllustrationType.HISTORY,
                bgColor = if (isDark) Color(0xFF131C30) else SkyBlue.copy(alpha = 0.15f)
            ),
            OnboardingPage(
                title = "Gain Financial Clarity",
                description = "Visualize your balances and maintain healthy financial relationships.",
                type = OnboardingIllustrationType.CLARITY,
                bgColor = if (isDark) Color(0xFF0B1D16) else AlimGreen.copy(alpha = 0.1f)
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Smooth background color interpolation
    val backgroundColor: Color by remember(pagerState.targetPage, pagerState.currentPageOffsetFraction, pages) {
        derivedStateOf {
            val currentPage = pagerState.currentPage
            val offset = pagerState.currentPageOffsetFraction
            val nextPage = if (offset > 0) currentPage + 1 else currentPage - 1
            
            if (nextPage in 0 until pages.size) {
                androidx.compose.ui.graphics.lerp(
                    pages[currentPage].bgColor,
                    pages[nextPage].bgColor,
                    kotlin.math.abs(offset)
                )
            } else {
                pages[currentPage].bgColor
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { position ->
                OnboardingPageContent(
                    page = pages[position],
                    isSelected = pagerState.currentPage == position
                )
            }

            OnboardingBottomBar(
                pagesSize = pages.size,
                currentPage = pagerState.currentPage,
                onNextClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        onFinish()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage, isSelected: Boolean) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(isSelected) {
        if (isSelected) visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .animateContentSize()
                .padding(bottom = 48.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000)) + scaleIn(tween(800, easing = FastOutSlowInEasing))
            ) {
                OnboardingIllustration(type = page.type)
            }
        }
        
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(600, delayMillis = 200)) { it / 2 } + fadeIn(tween(600, delayMillis = 200))
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 32.sp,
                    letterSpacing = (-0.5).sp
                ),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(600, delayMillis = 400)) { it / 2 } + fadeIn(tween(600, delayMillis = 400))
        ) {
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    lineHeight = 28.sp,
                    fontSize = 18.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OnboardingBottomBar(
    pagesSize: Int,
    currentPage: Int,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fluid Page Indicator
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(pagesSize) { iteration ->
                val isSelected = currentPage == iteration
                val width by animateDpAsState(
                    targetValue = if (isSelected) 32.dp else 10.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "indicatorWidth"
                )
                val color by animateColorAsState(
                    targetValue = if (isSelected) AlimGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                    label = "indicatorColor"
                )
                
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // Bouncy Next Button
        val interactionSource = remember { MutableInteractionSource() }
        Button(
            onClick = onNextClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AlimGreen,
                contentColor = Color.White
            ),
            modifier = Modifier
                .height(56.dp)
                .widthIn(min = 120.dp)
                .bounce(interactionSource)
        ) {
            Text(
                text = if (currentPage == pagesSize - 1) "Get Started" else "Next",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val type: OnboardingIllustrationType,
    val bgColor: Color
)
