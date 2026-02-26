package com.sbs.loaney.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbs.loaney.data.local.entity.BankAccountEntity

@Composable
fun WalletCardHolder(
    accounts: List<BankAccountEntity>,
    totalBalance: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    var showBalance by remember { mutableStateOf(false) }

    // Colors roughly matching the exact palette from the user's reference image
    val cardColors = listOf(
        Color(0xFFB4B0FF), // Stripe Purple (Back)
        Color(0xFFA5E083), // Wise Green (Middle)
        Color(0xFFF0F2F6)  // PayPal White (Front)
    )

    // Take up to 3 accounts. If there's 3, we want them rendered: back, middle, front.
    val displayAccounts = accounts.take(3)
    val totalCards = displayAccounts.size
    
    val cardHeight = 168.dp // 140 * 1.2
    val pocketHeight = 132.dp // 110 * 1.2

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(pocketHeight + (totalCards * 48).dp), // Height accommodates the pocket and sticking out cards (scaled offset)
        contentAlignment = Alignment.TopCenter
    ) {
        // Wallet Background / Back cover (drawn first, behind all cards)
        // Similar shape to the foreground pocket but larger
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF1D3320).copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.96f) // Slightly wider than the foreground pocket (0.92f)
                .height(pocketHeight + 36.dp) // Taller than the pocket so it peaks out behind the cards (scaled by 1.2)
        ) {}

        // Stacked Cards (index 0 drawn first -> Back. index N drawn last -> Front)
        displayAccounts.forEachIndexed { index, account ->
            val cardColor = cardColors[index % cardColors.size]
            val isDarkText = cardColor == Color(0xFFF0F2F6) || cardColor == Color(0xFFA5E083)

            // Front card (index == totalCards -1) sticks out least -> highest Y offset value (closest to the pocket)
            // Back card (index == 0) sticks out most -> lowest Y offset value (furthest from pocket)
            val stickOutOffset = ((totalCards - 1 - index) * 42) // 35 * 1.2

            // Width scales down slightly for cards in the back to give overlapping depth
            val widthFraction = 0.85f - ((totalCards - 1 - index) * 0.05f)

            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                color = cardColor,
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(cardHeight)
                    .offset(y = stickOutOffset.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = account.bankName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = if (isDarkText) Color(0xFF1E1E22) else Color.White
                    )
                    Text(
                        text = "* * * * * *",
                        color = if (isDarkText) Color.Gray else Color.White.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Wallet Pocket / Cover (drawn last to cover the bottom of all cards)
        // Ensure it aligns strictly to the bottom of the bounding Box
        Surface(
            shape = RoundedCornerShape(32.dp), // Very round pocket bottom
            color = Color(0xFF1D3320).copy(alpha = 0.6f), // 60% transparent dark green foreground
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.92f)
                .height(pocketHeight)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "* * * * * *", // Emulating the mock design exactly
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Balance",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = "Hidden Wallet",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
