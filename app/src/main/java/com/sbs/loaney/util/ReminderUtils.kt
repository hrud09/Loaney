package com.sbs.loaney.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Sends a friendly payment reminder via WhatsApp (preferred) or SMS (fallback).
 */
fun sendReminder(
    context: Context,
    contactName: String,
    amount: Double,
    dueDate: String,
    phoneNumber: String?,
    currencySymbol: String = "৳"
) {
    val message = "Hey $contactName, just a quick reminder about the $currencySymbol${String.format("%,.0f", amount)} due on $dueDate. Let me know if you need to chat about it! 😊"

    if (!phoneNumber.isNullOrBlank()) {
        // Try WhatsApp first
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val whatsappUri = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
        val whatsappIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
            setPackage("com.whatsapp")
        }

        try {
            if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(whatsappIntent)
                return
            }
        } catch (_: Exception) {
            // WhatsApp not installed, fall through to SMS
        }

        // Fallback to SMS
        val smsUri = Uri.parse("smsto:$cleanNumber")
        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
        }
        try {
            context.startActivity(smsIntent)
        } catch (_: Exception) {
            // Last resort: plain share
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Send Reminder"))
        }
    } else {
        // No phone number — use generic share
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Send Reminder"))
    }
}
