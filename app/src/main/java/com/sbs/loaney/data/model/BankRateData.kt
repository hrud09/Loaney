package com.sbs.loaney.data.model

/**
 * Hardcoded DPS and FDR interest rate data for major Bangladeshi banks.
 * Rates are approximate and based on publicly available schedules.
 * Update with app releases as rates change.
 */

data class BankDpsRate(
    val bankName: String,
    val tenureMonths: Int,
    val annualRate: Double
)

data class BankFdrRate(
    val bankName: String,
    val tenureMonths: Int,
    val annualRate: Double
)

object BankRateData {

    val allBankNames: List<String> = listOf(
        "Sonali Bank", "Janata Bank", "Agrani Bank", "Rupali Bank",
        "Dutch-Bangla Bank (DBBL)", "BRAC Bank", "City Bank", "Eastern Bank (EBL)",
        "Islami Bank Bangladesh", "Standard Chartered", "HSBC Bangladesh",
        "Prime Bank", "Mutual Trust Bank", "UCB", "Southeast Bank",
        "Pubali Bank", "NCC Bank", "Mercantile Bank", "Bank Asia", "AB Bank"
    )

    // ── DPS Rates ────────────────────────────────────────────────────────────
    val dpsRates: List<BankDpsRate> = listOf(
        // Sonali Bank
        BankDpsRate("Sonali Bank", 36, 6.00),
        BankDpsRate("Sonali Bank", 60, 6.25),
        BankDpsRate("Sonali Bank", 120, 6.50),

        // Janata Bank
        BankDpsRate("Janata Bank", 36, 6.00),
        BankDpsRate("Janata Bank", 60, 6.25),
        BankDpsRate("Janata Bank", 120, 6.50),

        // Agrani Bank
        BankDpsRate("Agrani Bank", 36, 6.00),
        BankDpsRate("Agrani Bank", 60, 6.25),
        BankDpsRate("Agrani Bank", 120, 6.50),

        // Rupali Bank
        BankDpsRate("Rupali Bank", 36, 6.00),
        BankDpsRate("Rupali Bank", 60, 6.25),
        BankDpsRate("Rupali Bank", 120, 6.50),

        // Dutch-Bangla Bank
        BankDpsRate("Dutch-Bangla Bank (DBBL)", 36, 5.50),
        BankDpsRate("Dutch-Bangla Bank (DBBL)", 60, 6.00),
        BankDpsRate("Dutch-Bangla Bank (DBBL)", 84, 6.25),
        BankDpsRate("Dutch-Bangla Bank (DBBL)", 120, 6.50),

        // BRAC Bank
        BankDpsRate("BRAC Bank", 36, 5.00),
        BankDpsRate("BRAC Bank", 60, 5.50),
        BankDpsRate("BRAC Bank", 84, 5.75),
        BankDpsRate("BRAC Bank", 120, 6.00),

        // City Bank
        BankDpsRate("City Bank", 36, 5.50),
        BankDpsRate("City Bank", 60, 6.00),
        BankDpsRate("City Bank", 120, 6.25),

        // Eastern Bank (EBL)
        BankDpsRate("Eastern Bank (EBL)", 36, 5.50),
        BankDpsRate("Eastern Bank (EBL)", 60, 6.00),
        BankDpsRate("Eastern Bank (EBL)", 84, 6.25),
        BankDpsRate("Eastern Bank (EBL)", 120, 6.50),

        // Islami Bank Bangladesh
        BankDpsRate("Islami Bank Bangladesh", 36, 6.00),
        BankDpsRate("Islami Bank Bangladesh", 60, 6.50),
        BankDpsRate("Islami Bank Bangladesh", 120, 7.00),

        // Standard Chartered
        BankDpsRate("Standard Chartered", 36, 4.00),
        BankDpsRate("Standard Chartered", 60, 4.50),

        // HSBC Bangladesh
        BankDpsRate("HSBC Bangladesh", 36, 3.75),
        BankDpsRate("HSBC Bangladesh", 60, 4.25),

        // Prime Bank
        BankDpsRate("Prime Bank", 36, 5.50),
        BankDpsRate("Prime Bank", 60, 6.00),
        BankDpsRate("Prime Bank", 120, 6.50),

        // Mutual Trust Bank
        BankDpsRate("Mutual Trust Bank", 36, 5.75),
        BankDpsRate("Mutual Trust Bank", 60, 6.25),
        BankDpsRate("Mutual Trust Bank", 120, 6.75),

        // UCB
        BankDpsRate("UCB", 36, 5.50),
        BankDpsRate("UCB", 60, 6.00),
        BankDpsRate("UCB", 120, 6.50),

        // Southeast Bank
        BankDpsRate("Southeast Bank", 36, 5.50),
        BankDpsRate("Southeast Bank", 60, 6.00),
        BankDpsRate("Southeast Bank", 120, 6.25),

        // Pubali Bank
        BankDpsRate("Pubali Bank", 36, 6.00),
        BankDpsRate("Pubali Bank", 60, 6.25),
        BankDpsRate("Pubali Bank", 120, 6.50),

        // NCC Bank
        BankDpsRate("NCC Bank", 36, 5.75),
        BankDpsRate("NCC Bank", 60, 6.25),
        BankDpsRate("NCC Bank", 120, 6.50),

        // Mercantile Bank
        BankDpsRate("Mercantile Bank", 36, 5.50),
        BankDpsRate("Mercantile Bank", 60, 6.00),
        BankDpsRate("Mercantile Bank", 120, 6.50),

        // Bank Asia
        BankDpsRate("Bank Asia", 36, 5.50),
        BankDpsRate("Bank Asia", 60, 6.00),
        BankDpsRate("Bank Asia", 120, 6.50),

        // AB Bank
        BankDpsRate("AB Bank", 36, 5.50),
        BankDpsRate("AB Bank", 60, 6.00),
        BankDpsRate("AB Bank", 120, 6.25)
    )

    // ── FDR Rates ────────────────────────────────────────────────────────────
    val fdrRates: List<BankFdrRate> = listOf(
        // Sonali Bank
        BankFdrRate("Sonali Bank", 3, 5.00),
        BankFdrRate("Sonali Bank", 6, 5.50),
        BankFdrRate("Sonali Bank", 12, 6.00),
        BankFdrRate("Sonali Bank", 24, 6.25),
        BankFdrRate("Sonali Bank", 36, 6.50),

        // Janata Bank
        BankFdrRate("Janata Bank", 3, 5.00),
        BankFdrRate("Janata Bank", 6, 5.50),
        BankFdrRate("Janata Bank", 12, 6.00),
        BankFdrRate("Janata Bank", 24, 6.25),
        BankFdrRate("Janata Bank", 36, 6.50),

        // Agrani Bank
        BankFdrRate("Agrani Bank", 3, 5.00),
        BankFdrRate("Agrani Bank", 6, 5.50),
        BankFdrRate("Agrani Bank", 12, 6.00),
        BankFdrRate("Agrani Bank", 24, 6.25),
        BankFdrRate("Agrani Bank", 36, 6.50),

        // Rupali Bank
        BankFdrRate("Rupali Bank", 3, 5.00),
        BankFdrRate("Rupali Bank", 6, 5.50),
        BankFdrRate("Rupali Bank", 12, 6.00),
        BankFdrRate("Rupali Bank", 24, 6.25),
        BankFdrRate("Rupali Bank", 36, 6.50),

        // Dutch-Bangla Bank
        BankFdrRate("Dutch-Bangla Bank (DBBL)", 1, 3.50),
        BankFdrRate("Dutch-Bangla Bank (DBBL)", 3, 4.50),
        BankFdrRate("Dutch-Bangla Bank (DBBL)", 6, 5.00),
        BankFdrRate("Dutch-Bangla Bank (DBBL)", 12, 5.50),
        BankFdrRate("Dutch-Bangla Bank (DBBL)", 24, 5.75),
        BankFdrRate("Dutch-Bangla Bank (DBBL)", 36, 6.00),

        // BRAC Bank
        BankFdrRate("BRAC Bank", 1, 3.00),
        BankFdrRate("BRAC Bank", 3, 4.00),
        BankFdrRate("BRAC Bank", 6, 4.50),
        BankFdrRate("BRAC Bank", 12, 5.00),
        BankFdrRate("BRAC Bank", 24, 5.25),
        BankFdrRate("BRAC Bank", 36, 5.50),

        // City Bank
        BankFdrRate("City Bank", 3, 4.50),
        BankFdrRate("City Bank", 6, 5.00),
        BankFdrRate("City Bank", 12, 5.50),
        BankFdrRate("City Bank", 24, 5.75),
        BankFdrRate("City Bank", 36, 6.00),

        // Eastern Bank (EBL)
        BankFdrRate("Eastern Bank (EBL)", 1, 3.50),
        BankFdrRate("Eastern Bank (EBL)", 3, 4.50),
        BankFdrRate("Eastern Bank (EBL)", 6, 5.00),
        BankFdrRate("Eastern Bank (EBL)", 12, 5.50),
        BankFdrRate("Eastern Bank (EBL)", 24, 5.75),
        BankFdrRate("Eastern Bank (EBL)", 36, 6.00),

        // Islami Bank Bangladesh
        BankFdrRate("Islami Bank Bangladesh", 3, 5.50),
        BankFdrRate("Islami Bank Bangladesh", 6, 6.00),
        BankFdrRate("Islami Bank Bangladesh", 12, 6.50),
        BankFdrRate("Islami Bank Bangladesh", 24, 6.75),
        BankFdrRate("Islami Bank Bangladesh", 36, 7.00),

        // Standard Chartered
        BankFdrRate("Standard Chartered", 1, 2.50),
        BankFdrRate("Standard Chartered", 3, 3.50),
        BankFdrRate("Standard Chartered", 6, 4.00),
        BankFdrRate("Standard Chartered", 12, 4.50),

        // HSBC Bangladesh
        BankFdrRate("HSBC Bangladesh", 1, 2.25),
        BankFdrRate("HSBC Bangladesh", 3, 3.25),
        BankFdrRate("HSBC Bangladesh", 6, 3.75),
        BankFdrRate("HSBC Bangladesh", 12, 4.25),

        // Prime Bank
        BankFdrRate("Prime Bank", 3, 5.00),
        BankFdrRate("Prime Bank", 6, 5.50),
        BankFdrRate("Prime Bank", 12, 6.00),
        BankFdrRate("Prime Bank", 24, 6.25),
        BankFdrRate("Prime Bank", 36, 6.50),

        // Mutual Trust Bank
        BankFdrRate("Mutual Trust Bank", 3, 5.25),
        BankFdrRate("Mutual Trust Bank", 6, 5.75),
        BankFdrRate("Mutual Trust Bank", 12, 6.25),
        BankFdrRate("Mutual Trust Bank", 24, 6.50),

        // UCB
        BankFdrRate("UCB", 3, 5.00),
        BankFdrRate("UCB", 6, 5.50),
        BankFdrRate("UCB", 12, 6.00),
        BankFdrRate("UCB", 24, 6.25),
        BankFdrRate("UCB", 36, 6.50),

        // Southeast Bank
        BankFdrRate("Southeast Bank", 3, 5.00),
        BankFdrRate("Southeast Bank", 6, 5.50),
        BankFdrRate("Southeast Bank", 12, 5.75),
        BankFdrRate("Southeast Bank", 24, 6.00),

        // Pubali Bank
        BankFdrRate("Pubali Bank", 3, 5.00),
        BankFdrRate("Pubali Bank", 6, 5.50),
        BankFdrRate("Pubali Bank", 12, 6.00),
        BankFdrRate("Pubali Bank", 24, 6.25),
        BankFdrRate("Pubali Bank", 36, 6.50),

        // NCC Bank
        BankFdrRate("NCC Bank", 3, 5.25),
        BankFdrRate("NCC Bank", 6, 5.75),
        BankFdrRate("NCC Bank", 12, 6.00),
        BankFdrRate("NCC Bank", 24, 6.25),

        // Mercantile Bank
        BankFdrRate("Mercantile Bank", 3, 5.00),
        BankFdrRate("Mercantile Bank", 6, 5.50),
        BankFdrRate("Mercantile Bank", 12, 6.00),
        BankFdrRate("Mercantile Bank", 24, 6.25),
        BankFdrRate("Mercantile Bank", 36, 6.50),

        // Bank Asia
        BankFdrRate("Bank Asia", 3, 5.00),
        BankFdrRate("Bank Asia", 6, 5.50),
        BankFdrRate("Bank Asia", 12, 5.75),
        BankFdrRate("Bank Asia", 24, 6.00),
        BankFdrRate("Bank Asia", 36, 6.25),

        // AB Bank
        BankFdrRate("AB Bank", 3, 5.00),
        BankFdrRate("AB Bank", 6, 5.50),
        BankFdrRate("AB Bank", 12, 5.75),
        BankFdrRate("AB Bank", 24, 6.00)
    )

    /** Get available DPS tenures for a specific bank */
    fun getDpsTenuresForBank(bankName: String): List<Int> =
        dpsRates.filter { it.bankName == bankName }.map { it.tenureMonths }.distinct().sorted()

    /** Get available FDR tenures for a specific bank */
    fun getFdrTenuresForBank(bankName: String): List<Int> =
        fdrRates.filter { it.bankName == bankName }.map { it.tenureMonths }.distinct().sorted()

    /** Look up DPS rate for a bank and tenure */
    fun getDpsRate(bankName: String, tenureMonths: Int): Double? =
        dpsRates.firstOrNull { it.bankName == bankName && it.tenureMonths == tenureMonths }?.annualRate

    /** Look up FDR rate for a bank and tenure */
    fun getFdrRate(bankName: String, tenureMonths: Int): Double? =
        fdrRates.firstOrNull { it.bankName == bankName && it.tenureMonths == tenureMonths }?.annualRate

    /** Get all banks that offer DPS */
    fun getDpsBanks(): List<String> = dpsRates.map { it.bankName }.distinct().sorted()

    /** Get all banks that offer FDR */
    fun getFdrBanks(): List<String> = fdrRates.map { it.bankName }.distinct().sorted()

    /** Human-readable tenure label */
    fun tenureLabel(months: Int): String = when {
        months < 12 -> "${months}m"
        months % 12 == 0 -> "${months / 12}y"
        else -> "${months / 12}y ${months % 12}m"
    }
}
