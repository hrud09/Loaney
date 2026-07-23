package com.sbs.loaney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sbs.loaney.ui.theme.AlimDark
import com.sbs.loaney.ui.theme.AlimGreen
import com.sbs.loaney.ui.theme.AlimWhite
import com.sbs.loaney.ui.theme.CyberIndigo

enum class ToolInfoType {
    EMI_HUB,
    DPS_FDR
}

data class FieldGuideItem(
    val fieldName: String,
    val description: String
)

data class InfoSection(
    val title: String,
    val description: String,
    val fields: List<FieldGuideItem> = emptyList()
)

@Composable
fun ToolInfoDialog(
    toolType: ToolInfoType,
    onDismiss: () -> Unit
) {
    val (title, icon, accentColor, sections, tipText) = when (toolType) {
        ToolInfoType.EMI_HUB -> ToolInfoData(
            title = "EMI Hub Guide",
            icon = Icons.Default.Calculate,
            accentColor = CyberIndigo,
            sections = listOf(
                InfoSection(
                    title = "What is EMI?",
                    description = "An Equated Monthly Installment (EMI) is a fixed payment amount made to a bank or lender every month. It repays both the principal loan amount and accrued interest over a specified period."
                ),
                InfoSection(
                    title = "Field-by-Field Guide",
                    description = "Understand what each input field in the calculator and tracker means:",
                    fields = listOf(
                        FieldGuideItem("Purchase Price / Total Amount", "The full cost of the item or the total principal borrowed."),
                        FieldGuideItem("Down Payment", "The initial cash payment made upfront. Financing is only calculated on the remaining balance (Price - Down Payment)."),
                        FieldGuideItem("Annual Interest Rate (%)", "The annual percentage rate (APR) charged by the financial institution. Enter 0% for no-cost EMI deals."),
                        FieldGuideItem("Tenure (Months)", "The total duration of the installment plan (e.g., 3, 6, 12, 24, 36 months)."),
                        FieldGuideItem("Monthly Due Day", "The calendar day of the month (1-31) when your installment is due for reminder notifications.")
                    )
                ),
                InfoSection(
                    title = "Hub Features",
                    description = "• Tracker: Keep track of active EMI liabilities, log paid installments, and link them to your bank or card.\n• Calculator: Interactively simulate monthly installments and total interest with real-time donut chart visualization.\n• Offers: Discover zero-interest and special bank EMI deals."
                )
            ),
            tipText = "Tip: Use the Calculator to test different down payment amounts to find a monthly installment that comfortably fits your budget."
        )

        ToolInfoType.DPS_FDR -> ToolInfoData(
            title = "DPS & FDR Guide",
            icon = Icons.Default.Savings,
            accentColor = AlimGreen,
            sections = listOf(
                InfoSection(
                    title = "DPS vs. FDR Explained",
                    description = "• DPS (Deposit Pension Scheme): Regular monthly recurring deposits built over time. Ideal for systematic monthly savings.\n• FDR (Fixed Deposit Receipt): A one-time lump-sum deposit locked for a fixed term to earn higher fixed interest."
                ),
                InfoSection(
                    title = "Field-by-Field Guide",
                    description = "Key parameters used to calculate maturity returns:",
                    fields = listOf(
                        FieldGuideItem("Monthly / Deposit Amount", "For DPS, enter your monthly contribution. For FDR, enter the total one-time lump sum."),
                        FieldGuideItem("Interest Rate (%)", "The annual interest rate offered by the bank."),
                        FieldGuideItem("Tenure (Months)", "The total duration of the deposit in months (e.g. 12 months for 1 year, 60 months for 5 years)."),
                        FieldGuideItem("Compounding Frequency", "How often interest is calculated and added to the principal (Monthly, Quarterly, Half-Yearly, or Yearly). More frequent compounding yields higher final returns."),
                        FieldGuideItem("TIN (Tax Identification Number)", "In Bangladesh, having a TIN reduces the Source Tax (AIT) on interest from 15% down to 10%.")
                    )
                ),
                InfoSection(
                    title = "Premature Cashout Simulation",
                    description = "Our engine estimates how much you would receive if you break your deposit early at a premature penalty rate."
                )
            ),
            tipText = "Tip: Toggle the 'I have a TIN' switch to see how your net maturity payout increases with reduced source tax!"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = accentColor
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = AlimWhite,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Info Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    sections.forEach { section ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )

                            Text(
                                text = section.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )

                            if (section.fields.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    section.fields.forEach { item ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = item.fieldName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = item.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tip Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = tipText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = AlimWhite)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

private data class ToolInfoData(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val sections: List<InfoSection>,
    val tipText: String
)
