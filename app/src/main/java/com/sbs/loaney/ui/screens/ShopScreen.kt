package com.sbs.loaney.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.model.Coupon
import com.sbs.loaney.data.model.CouponCategory
import com.sbs.loaney.ui.theme.CurrencyTypography
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.ShopViewModel

// Premium bKash style doesn't use these dark gamified colors anymore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onNavigateBack: () -> Unit,
    viewModel: ShopViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    var selectedCoupon by remember { mutableStateOf<Coupon?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Shop Rewards", 
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineSmall,
                        letterSpacing = (-0.5).sp
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        },
        containerColor = SoftSlate
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- Pie Balance Header ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .glassCard(
                        shape = RoundedCornerShape(32.dp),
                        backgroundColor = CyberIndigo.copy(alpha = 0.9f),
                        borderColor = Color.White.copy(alpha = 0.2f)
                    )
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Glow
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(VibrantTeal.copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.2f)
                        ),
                        radius = size.width * 0.6f
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CURRENT BALANCE",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${uiState.totalPies}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 36.sp
                                )
                            )
                            Text(
                                text = " PIES",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }
                    }
                }
            }

            // --- Categories ---
            LazyRow(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                item {
                    CategoryChip(
                        label = "All",
                        isSelected = uiState.selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) }
                    )
                }
                items(CouponCategory.entries) { category ->
                    CategoryChip(
                        label = category.name.lowercase().capitalize(),
                        isSelected = uiState.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }

            // --- Coupons Grid ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.filteredCoupons) { coupon ->
                    CouponCard(
                        coupon = coupon,
                        canAfford = uiState.totalPies >= coupon.costInPies,
                        onClick = { selectedCoupon = coupon }
                    )
                }
            }
        }

        // --- Purchase Confirmation Dialog ---
        if (selectedCoupon != null) {
            val coupon = selectedCoupon!!
            AlertDialog(
                onDismissRequest = { selectedCoupon = null },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Confirm Redemption", color = MaterialTheme.colorScheme.onBackground) },
                text = {
                    Text(
                        "Spend ${coupon.costInPies} pies for ${coupon.brandName}'s ${coupon.discountTitle}?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.purchaseCoupon(coupon)
                            selectedCoupon = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text("Redeem", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedCoupon = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // --- Success Overlay ---
        AnimatedVisibility(
            visible = uiState.purchaseSuccess,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00C896),
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Coupon Redeemed!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Check your email for the code.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier
                        .background(CyberIndigo, CircleShape)
                        .shadow(8.dp, CircleShape, spotColor = CyberIndigo.copy(alpha = 0.4f))
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                        .border(0.5.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
                }
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isSelected) Color.White else Color(0xFF475569)
        )
    }
}

@Composable
fun CouponCard(coupon: Coupon, canAfford: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (canAfford) 1f else 0.6f)
            .glassCard(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color.White,
                borderColor = Color.Black.copy(alpha = 0.03f)
            )
            .clickable(enabled = canAfford) { onClick() }
    ) {
        // Brand Accent (Modern Gradient)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(coupon.brandColor.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo Placeholder - Modern Style
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(coupon.brandColor.copy(alpha = 0.1f), CircleShape)
                    .border(1.5.dp, coupon.brandColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = coupon.brandName.firstOrNull()?.toString()?.uppercase() ?: "",
                    color = coupon.brandColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coupon.brandName.uppercase(),
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp
                )
                Text(
                    text = coupon.discountTitle,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(
                    text = coupon.description,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (canAfford) CyberIndigo.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = if (canAfford) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${coupon.costInPies}",
                        color = if (canAfford) CyberIndigo else Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
                if (!canAfford) {
                    Text(
                        "INSUFFICIENT",
                        color = CoralRose,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 4.dp, end = 2.dp)
                    )
                }
            }
        }
    }
}

private fun String.capitalize() = replaceFirstChar { it.uppercase() }
