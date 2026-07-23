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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sbs.loaney.R
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
            title = stringResource(id = R.string.toolinfo_emi_title),
            icon = Icons.Default.Calculate,
            accentColor = CyberIndigo,
            sections = listOf(
                InfoSection(
                    title = stringResource(id = R.string.toolinfo_what_is_emi_title),
                    description = stringResource(id = R.string.toolinfo_what_is_emi_desc)
                ),
                InfoSection(
                    title = stringResource(id = R.string.toolinfo_field_guide_title),
                    description = stringResource(id = R.string.toolinfo_emi_field_guide_desc),
                    fields = listOf(
                        FieldGuideItem(stringResource(id = R.string.toolinfo_field_purchase_price), stringResource(id = R.string.toolinfo_field_purchase_price_desc)),
                        FieldGuideItem(stringResource(id = R.string.toolinfo_field_down_payment), stringResource(id = R.string.toolinfo_field_down_payment_desc)),
                        FieldGuideItem(stringResource(id = R.string.toolinfo_field_annual_interest), stringResource(id = R.string.toolinfo_field_annual_interest_desc)),
                        FieldGuideItem(stringResource(id = R.string.toolinfo_tenure_months), stringResource(id = R.string.toolinfo_field_emi_tenure_desc)),
                        FieldGuideItem(stringResource(id = R.string.toolinfo_field_monthly_due_day), stringResource(id = R.string.toolinfo_field_monthly_due_day_desc))
                    )
                ),
                InfoSection(
                    title = stringResource(id = R.string.toolinfo_hub_features_title),
                    description = stringResource(id = R.string.toolinfo_hub_features_desc)
                )
            ),
            tipText = stringResource(id = R.string.toolinfo_emi_tip)
        )

        ToolInfoType.DPS_FDR -> ToolInfoData(
            title = stringResource(id = R.string.toolinfo_dps_title),
            icon = Icons.Default.Savings,
            accentColor = AlimGreen,
            sections = listOf(
                InfoSection(
                    title = stringResource(id = R.string.toolinfo_dps_explained_title),
                    description = stringResource(id = R.string.toolinfo_dps_explained_desc)
                ),
                InfoSection(
                    title = stringResource(id = R.string.toolinfo_field_guide_title),
                    description = stringResource(id = R.string.toolinfo_dps_field_guide_desc),
                    fields = listOf(
                        FieldGuideItem(stringResource(id = R.string.toolinfo_field_deposit_amount), stringResource(id = R.string.toolinfo_field_deposit_amount_desc)),
                        FieldGuideItem(stringResource(id = R.string.toolinfo_field_interest_rate), stringResource(id = R.string.toolinfo_field_interest_rate_desc)),
                        FieldGuideItem(stringResource(id = R.string.toolinfo_tenure_months), stringResource(id = R.string.toolinfo_field_dps_tenure_desc)),
                        FieldGuideItem(stringResource(id = R.string.toolinfo_field_compounding), stringResource(id = R.string.toolinfo_field_compounding_desc)),
                        FieldGuideItem(stringResource(id = R.string.toolinfo_field_tin), stringResource(id = R.string.toolinfo_field_tin_desc))
                    )
                ),
                InfoSection(
                    title = stringResource(id = R.string.toolinfo_premature_title),
                    description = stringResource(id = R.string.toolinfo_premature_desc)
                )
            ),
            tipText = stringResource(id = R.string.toolinfo_dps_tip)
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
                    Text(stringResource(id = R.string.toolinfo_got_it), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
