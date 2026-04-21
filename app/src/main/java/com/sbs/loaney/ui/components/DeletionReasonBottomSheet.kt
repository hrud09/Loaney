package com.sbs.loaney.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbs.loaney.R
import com.sbs.loaney.ui.theme.AlimDark
import com.sbs.loaney.ui.theme.AlimGreen
import com.sbs.loaney.ui.theme.AlimWhite
import com.sbs.loaney.ui.theme.CoralRose
import com.sbs.loaney.ui.viewmodel.DeletionReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletionReasonBottomSheet(
    personName: String,
    onDismiss: () -> Unit,
    onConfirm: (DeletionReason, String?) -> Unit
) {
    var selectedReason by remember { mutableStateOf<DeletionReason?>(null) }
    var otherReasonText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AlimWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AlimDark.copy(alpha = 0.1f)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CoralRose.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = CoralRose,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title & Subtitle
            Text(
                text = "Delete Loan for $personName?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AlimDark,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(id = R.string.delete_reason_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AlimDark.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Reason Options
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReasonItem(
                    title = stringResource(id = R.string.delete_reason_paid),
                    subtitle = "The loan has been settled in full",
                    isSelected = selectedReason == DeletionReason.PAID_FULLY,
                    onClick = { selectedReason = DeletionReason.PAID_FULLY }
                )
                ReasonItem(
                    title = stringResource(id = R.string.delete_reason_forgiven),
                    subtitle = "This debt is written off / forgiven",
                    isSelected = selectedReason == DeletionReason.FORGIVEN,
                    onClick = { selectedReason = DeletionReason.FORGIVEN }
                )
                ReasonItem(
                    title = stringResource(id = R.string.delete_reason_mistake),
                    subtitle = "Logged by error, remove completely",
                    isSelected = selectedReason == DeletionReason.MISTAKE,
                    onClick = { selectedReason = DeletionReason.MISTAKE }
                )
                ReasonItem(
                    title = stringResource(id = R.string.delete_reason_other),
                    subtitle = "None of the above, provide your own",
                    isSelected = selectedReason == DeletionReason.OTHER,
                    onClick = { selectedReason = DeletionReason.OTHER }
                )

                AnimatedVisibility(
                    visible = selectedReason == DeletionReason.OTHER,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = otherReasonText,
                        onValueChange = { otherReasonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        placeholder = { Text(stringResource(id = R.string.delete_reason_other_hint)) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AlimGreen,
                            unfocusedBorderColor = AlimDark.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AlimDark.copy(alpha = 0.1f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlimDark)
                ) {
                    Text(stringResource(id = R.string.cancel), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { selectedReason?.let { onConfirm(it, otherReasonText) } },
                    enabled = selectedReason != null,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralRose,
                        contentColor = Color.White,
                        disabledContainerColor = CoralRose.copy(alpha = 0.3f)
                    )
                ) {
                    Text(stringResource(id = R.string.delete), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReasonItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) AlimGreen.copy(alpha = 0.05f) else Color.Transparent,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AlimGreen else AlimDark.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) AlimGreen else AlimDark
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AlimDark.copy(alpha = 0.5f)
                )
            }
            
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AlimGreen,
                    unselectedColor = AlimDark.copy(alpha = 0.2f)
                )
            )
        }
    }
}
