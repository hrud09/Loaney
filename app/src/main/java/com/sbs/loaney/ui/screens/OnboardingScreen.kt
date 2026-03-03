package com.sbs.loaney.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.R
import com.sbs.loaney.ui.components.OnboardingIllustration
import com.sbs.loaney.ui.components.OnboardingIllustrationType
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to Loaney",
            description = "Track all your lent and borrowed money in one secure place.",
            type = OnboardingIllustrationType.WELCOME
        ),
        OnboardingPage(
            title = "Never Forget a Debt",
            description = "Keep a clear history of your transactions so you always know who owes who.",
            type = OnboardingIllustrationType.HISTORY
        ),
        OnboardingPage(
            title = "Gain Financial Clarity",
            description = "Visualize your balances and maintain healthy financial relationships.",
            type = OnboardingIllustrationType.CLARITY
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    // State: show profile setup instead of pager
    var showProfileSetup by remember { mutableStateOf(false) }

    // Profile state
    var userName by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("৳") }
    val currencies = listOf(
        "৳" to "BDT",
        "$" to "USD",
        "€" to "EUR",
        "£" to "GBP",
        "₹" to "INR"
    )

    val completeOnboarding = {
        if (userName.isNotBlank()) {
            settingsViewModel.setUserName(userName)
        }
        settingsViewModel.setCurrencySymbol(selectedCurrency)
        settingsViewModel.setOnboardingCompleted(true)
        onFinish()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = DeepDarkBg
    ) { paddingValues ->
        if (showProfileSetup) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ProfileSetupPage(
                        userName = userName,
                        onNameChange = { userName = it },
                        selectedCurrency = selectedCurrency,
                        currencies = currencies,
                        onCurrencySelect = { selectedCurrency = it },
                        onSkip = completeOnboarding
                    )
                }
                
                Button(
                    onClick = completeOnboarding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = userName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftViolet,
                        contentColor = Color.White
                    )
                ) {
                    Text("Get Started", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        } else {
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
                    OnboardingPageContent(page = pages[position])
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page Indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(pages.size) { iteration ->
                            val isSelected = pagerState.currentPage == iteration
                            Box(
                                modifier = Modifier
                                    .size(width = if (isSelected) 24.dp else 8.dp, height = 8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) SoftViolet else MutedText.copy(alpha = 0.3f))
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage == pages.size - 1) {
                                showProfileSetup = true
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftViolet)
                    ) {
                        Text(if (pagerState.currentPage == pages.size - 1) "Finish" else "Next", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSetupPage(
    userName: String,
    onNameChange: (String) -> Unit,
    selectedCurrency: String,
    currencies: List<Pair<String, String>>,
    onCurrencySelect: (String) -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip) {
                Text("Skip for now", color = MutedText)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Large icon container
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(DarkSurface, CircleShape)
                .border(2.dp, SoftViolet.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = SoftViolet,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Nearly There!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tell us your name and preferred currency to personalize your experience.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MutedText,
                lineHeight = 24.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Name Input
        OutlinedTextField(
            value = userName,
            onValueChange = onNameChange,
            label = { Text("Your Name", color = MutedText) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SoftViolet) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = SoftViolet,
                unfocusedBorderColor = DarkOutline,
                cursorColor = SoftViolet
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Currency Selection
        Text(
            text = "Local Currency",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            currencies.forEach { (symbol, code) ->
                val isSelected = selectedCurrency == symbol
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) SoftViolet.copy(alpha = 0.15f) else DarkSurface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCurrencySelect(symbol) }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) SoftViolet else DarkOutline,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SoftViolet else Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = code,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) SoftViolet else MutedText,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingIllustration(type = page.type)
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 32.sp
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MutedText,
                lineHeight = 26.sp,
                fontSize = 18.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val type: OnboardingIllustrationType
)
