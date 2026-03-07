package com.sbs.loaney.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.ui.theme.*

@Composable
fun WalletCardHolder(
    accounts: List<BankAccountEntity>,
    totalBalance: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    var showBalance by remember { mutableStateOf(false) }

    // Neubrutulism colors for cards
    val cardSolidColors = listOf(
        NbCoral,
        NbSkyBlue,
        NbYellow,
        NbGreen
    )

    val displayAccounts = accounts.take(3)
    val totalCards = displayAccounts.size
    
    val cardHeight = 168.dp
    val pocketHeight = 132.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(pocketHeight + (totalCards * 48).dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Wallet Background
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.96f)
                .height(pocketHeight + 36.dp)
                .neubrutalistCard(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = NbPureBlack.copy(alpha = 0.9f)
                )
        )

        // Stacked Cards
        displayAccounts.forEachIndexed { index, account ->
            val cardColor = cardSolidColors[index % cardSolidColors.size]

            val stickOutOffset = ((totalCards - 1 - index) * 42)
            val widthFraction = 0.85f - ((totalCards - 1 - index) * 0.05f)

            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(cardHeight)
                    .offset(y = stickOutOffset.dp)
                    .neubrutalistCard(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                        backgroundColor = cardColor
                    )
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
                        color = NbPureBlack
                    )
                    Text(
                        text = "* * * * * *",
                        color = NbPureBlack.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Wallet Pocket / Cover
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.92f)
                .height(pocketHeight)
                .neubrutalistCard(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = NbPureBlack
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "* * * * * *",
                    color = NbPureWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Balance",
                    color = NbPureWhite.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = "Hidden Wallet",
                    tint = NbPureWhite.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
