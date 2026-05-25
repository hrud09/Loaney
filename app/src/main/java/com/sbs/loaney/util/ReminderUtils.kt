package com.sbs.loaney.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utility functions to send payment reminders through various channels.
 */

fun getReminderMessage(contactName: String, amount: Double, dueDate: String, currencySymbol: String): String {
    return "Hey $contactName, just a quick reminder about the $currencySymbol${String.format("%,.0f", amount)} due on $dueDate. Let me know if you need to chat about it! 😊"
}

fun sendWhatsAppReminder(context: Context, phoneNumber: String, message: String) {
    val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
    val whatsappUri = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
    val whatsappIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
        setPackage("com.whatsapp")
    }

    try {
        context.startActivity(whatsappIntent)
    } catch (e: Exception) {
        // Fallback to generic share if WhatsApp not installed
        sendGenericShareReminder(context, message)
    }
}

fun sendSmsReminder(context: Context, phoneNumber: String, message: String) {
    val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
    val smsUri = Uri.parse("smsto:$cleanNumber")
    val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
        putExtra("sms_body", message)
    }
    try {
        context.startActivity(smsIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open SMS app", Toast.LENGTH_SHORT).show()
    }
}

fun sendEmailReminder(context: Context, email: String, contactName: String, message: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$email")
        putExtra(Intent.EXTRA_SUBJECT, "Payment Reminder: Loan with $contactName")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

fun sendMessengerReminder(context: Context, message: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        setPackage("com.facebook.orca")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to generic share
        sendGenericShareReminder(context, message)
    }
}

fun sendGenericShareReminder(context: Context, message: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Send Reminder"))
}

fun sendReminder(
    context: Context,
    contactName: String,
    amount: Double,
    dueDate: String,
    phoneNumber: String?,
    currencySymbol: String = "৳"
) {
    val message = getReminderMessage(contactName, amount, dueDate, currencySymbol)

    if (!phoneNumber.isNullOrBlank()) {
        sendWhatsAppReminder(context, phoneNumber, message)
    } else {
        sendGenericShareReminder(context, message)
    }
}

