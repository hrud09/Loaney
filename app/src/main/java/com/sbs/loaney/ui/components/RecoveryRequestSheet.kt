package com.sbs.loaney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sbs.loaney.R
import com.sbs.loaney.data.model.RecoveryConfig
import com.sbs.loaney.ui.theme.AlimGreen
import com.sbs.loaney.ui.theme.AlimWhite
import com.sbs.loaney.ui.theme.AmberWarn
import com.sbs.loaney.ui.theme.CoralRose
import java.util.Locale

/**
 * Bottom sheet where a lender requests assisted recovery of an overdue loan.
 *
 * This UI only *collects and confirms* the request. It never contacts the borrower — while
 * [RecoveryConfig.IS_LIVE] is `false` an "early access" banner makes that explicit, and the
 * submit path (LoanTrackerViewModel → RecoveryRepository) records the request without any outreach.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryRequestSheet(
    borrowerName: String,
    outstandingAmount: Double,
    daysOverdue: Int,
    currency: String,
    borrowerConfirmed: Boolean,
    hasProof: Boolean,
    hasWitness: Boolean,
    paymentCount: Int,
    onDismiss: () -> Unit,
    onSubmit: (note: String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }

    val money = { amount: Double -> "$currency${String.format(Locale.getDefault(), "%,.0f", amount)}" }
    val estimatedFee = RecoveryConfig.estimatedFee(outstandingAmount)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.recovery_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.recovery_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Early-access notice — only shown while the live recovery pipeline is off.
            if (!RecoveryConfig.IS_LIVE) {
                InfoBanner(
                    icon = Icons.Filled.Info,
                    tint = AmberWarn,
                    text = stringResource(R.string.recovery_early_access, borrowerName)
                )
            }

            // Fee card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlimGreen.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.recovery_fee_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(
                            R.string.recovery_fee_value,
                            String.format(Locale.getDefault(), "%.0f", RecoveryConfig.FEE_PERCENT)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AlimGreen
                    )
                }
                Text(
                    stringResource(R.string.recovery_fee_estimate, money(estimatedFee), money(outstandingAmount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Case-strength checklist
            Text(
                stringResource(R.string.recovery_evidence_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            EvidenceRow(borrowerConfirmed, stringResource(R.string.recovery_ev_confirmed))
            EvidenceRow(hasProof, stringResource(R.string.recovery_ev_proof))
            EvidenceRow(hasWitness, stringResource(R.string.recovery_ev_witness))
            EvidenceRow(paymentCount > 0, stringResource(R.string.recovery_ev_payments, paymentCount))

            if (!borrowerConfirmed || !hasProof) {
                Text(
                    stringResource(R.string.recovery_ev_missing_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberWarn
                )
            }

            // Optional lender note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.recovery_note_hint)) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AlimGreen,
                    cursorColor = AlimGreen
                )
            )

            // Consent (required)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = consent,
                    onCheckedChange = { consent = it },
                    colors = CheckboxDefaults.colors(checkedColor = AlimGreen)
                )
                Text(
                    stringResource(R.string.recovery_consent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 14.dp)
                )
            }

            // Jurisdiction / partner footnote
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.recovery_jurisdiction),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { onSubmit(note) },
                enabled = consent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlimGreen, contentColor = AlimWhite)
            ) {
                Text(stringResource(R.string.recovery_submit), fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text(stringResource(R.string.recovery_cancel))
            }
        }
    }
}

@Composable
private fun EvidenceRow(present: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (present) Icons.Filled.CheckCircle else Icons.Outlined.Cancel,
            contentDescription = null,
            tint = if (present) AlimGreen else CoralRose.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InfoBanner(icon: ImageVector, tint: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
