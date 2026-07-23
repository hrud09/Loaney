package com.sbs.loaney.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sbs.loaney.data.model.BanksData
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Result of OCR extraction from a bank document image.
 * All fields are nullable — only fields that could be confidently extracted are set.
 */
data class OcrResult(
    val accountName: String? = null,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val branchName: String? = null,
    val swiftCode: String? = null
)

/**
 * Uses Google ML Kit (bundled, on-device) to extract bank account details
 * from a photo or screenshot.
 */
object BankAccountOcrExtractor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // All known bank names flattened from BanksData for fuzzy matching
    private val allKnownBanks: List<String> by lazy {
        BanksData.countriesWithBanks.values.flatten().distinct()
    }

    /**
     * Run OCR on the image at [imageUri] and extract bank account fields.
     */
    suspend fun extractFromImage(context: Context, imageUri: Uri): OcrResult {
        val inputImage = InputImage.fromFilePath(context, imageUri)
        val text = recognizeText(inputImage)
        return parseExtractedText(text)
    }

    private suspend fun recognizeText(image: InputImage): String {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener {
                    continuation.resume("")
                }
        }
    }

    /**
     * Parse the raw OCR text and extract structured bank account fields.
     */
    internal fun parseExtractedText(rawText: String): OcrResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        return OcrResult(
            accountNumber = extractAccountNumber(rawText),
            accountName = extractAccountName(lines),
            bankName = extractBankName(rawText),
            branchName = extractBranch(lines),
            swiftCode = extractSwiftCode(rawText)
        )
    }

    // ── Account Number ──────────────────────────────────────────────────────
    // Look for sequences of 8–20 digits, possibly separated by dashes or spaces.
    // Prefer the longest match. Skip phone-like numbers (starting with +).
    private fun extractAccountNumber(text: String): String? {
        // Pattern: sequences of digits optionally separated by dashes/spaces, 8-20 digits total
        val pattern = Regex("""(?<!\+)\b(\d[\d\s\-]{6,24}\d)\b""")
        val candidates = pattern.findAll(text)
            .map { it.value.replace(Regex("[\\s\\-]"), "") }
            .filter { it.length in 8..20 }
            .toList()

        // Return the longest candidate (most likely full account number)
        return candidates.maxByOrNull { it.length }
    }

    // ── Account Name ────────────────────────────────────────────────────────
    // Look for text near labels like "Account Holder", "Name", "A/C Name", etc.
    private fun extractAccountName(lines: List<String>): String? {
        val nameLabels = listOf(
            "account holder", "a/c holder", "account name", "a/c name",
            "cardholder", "card holder", "holder name", "customer name",
            "name of account", "beneficiary name", "beneficiary",
            "name", "holder"
        )

        for ((index, line) in lines.withIndex()) {
            val lower = line.lowercase()
            for (label in nameLabels) {
                if (lower.contains(label)) {
                    // Check if the value is on the same line after a colon/separator
                    val afterSep = extractValueAfterSeparator(line, label)
                    if (afterSep != null && looksLikeName(afterSep)) {
                        return afterSep
                    }
                    // Check the next line
                    if (index + 1 < lines.size) {
                        val nextLine = lines[index + 1]
                        if (looksLikeName(nextLine)) {
                            return nextLine
                        }
                    }
                }
            }
        }
        return null
    }

    // ── Bank Name ───────────────────────────────────────────────────────────
    // Fuzzy match against the known banks list
    private fun extractBankName(text: String): String? {
        val lowerText = text.lowercase()

        // Direct match against known bank names (case-insensitive, longest match first)
        val match = allKnownBanks
            .sortedByDescending { it.length }
            .firstOrNull { lowerText.contains(it.lowercase()) }

        if (match != null) return match

        // Try partial/fuzzy: check if any bank name's main words appear in the text
        for (bank in allKnownBanks) {
            val words = bank.lowercase().split(" ").filter { it.length > 3 }
            if (words.size >= 2 && words.all { lowerText.contains(it) }) {
                return bank
            }
        }

        // Check for common bank keywords and try to extract
        val bankKeywords = listOf("bank", "banking", "ltd", "limited", "plc")
        val lines = text.lines()
        for (line in lines) {
            val lower = line.lowercase().trim()
            if (bankKeywords.any { lower.contains(it) } && lower.length < 60) {
                return line.trim()
            }
        }

        return null
    }

    // ── Branch ──────────────────────────────────────────────────────────────
    private fun extractBranch(lines: List<String>): String? {
        val branchLabels = listOf("branch", "branch name", "br.")

        for ((index, line) in lines.withIndex()) {
            val lower = line.lowercase()
            for (label in branchLabels) {
                if (lower.contains(label)) {
                    val afterSep = extractValueAfterSeparator(line, label)
                    if (afterSep != null && afterSep.length in 3..50) {
                        return afterSep
                    }
                    if (index + 1 < lines.size) {
                        val nextLine = lines[index + 1].trim()
                        if (nextLine.length in 3..50 && !nextLine.all { it.isDigit() }) {
                            return nextLine
                        }
                    }
                }
            }
        }
        return null
    }

    // ── SWIFT / BIC Code ────────────────────────────────────────────────────
    // Standard format: 4 letters (bank) + 2 letters (country) + 2 alphanumeric (location) + optional 3 alphanumeric (branch)
    private fun extractSwiftCode(text: String): String? {
        val swiftPattern = Regex("""\b([A-Z]{4}[A-Z]{2}[A-Z0-9]{2}(?:[A-Z0-9]{3})?)\b""")
        return swiftPattern.find(text.uppercase())?.value
    }

    // ── Helpers ─────────────────────────────────────────────────────────────
    /**
     * Extract the value portion after a label and separator (: or -)
     * e.g. "Account Name: John Doe" → "John Doe"
     */
    private fun extractValueAfterSeparator(line: String, label: String): String? {
        val lower = line.lowercase()
        val labelIndex = lower.indexOf(label.lowercase())
        if (labelIndex < 0) return null

        val afterLabel = line.substring(labelIndex + label.length).trim()
        // Remove leading separators
        val cleaned = afterLabel.trimStart(':', '-', '–', '—', ' ', '\t').trim()
        return cleaned.ifBlank { null }
    }

    /**
     * Heuristic: does this string look like a person's name?
     * - Contains at least 2 characters
     * - Is mostly letters and spaces
     * - Not all digits
     * - Not a label keyword
     */
    private fun looksLikeName(text: String): Boolean {
        val cleaned = text.trim()
        if (cleaned.length < 2) return false
        if (cleaned.all { it.isDigit() }) return false

        val labelKeywords = listOf(
            "account", "number", "branch", "swift", "bank", "date",
            "balance", "routing", "ifsc", "code", "type", "status"
        )
        if (labelKeywords.any { cleaned.lowercase() == it }) return false

        val letterRatio = cleaned.count { it.isLetter() || it == ' ' || it == '.' }.toFloat() / cleaned.length
        return letterRatio > 0.7f
    }
}
