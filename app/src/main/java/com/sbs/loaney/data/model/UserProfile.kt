package com.sbs.loaney.data.model

/**
 * Holds gamification state for the local user.
 */
data class UserProfile(
    val name: String = "You",
    val totalLoaneyPies: Int = 0,
    val xpLevel: Int = 1,
    val currentXp: Int = 0
) {
    /** XP required to reach the next level (scales with level). */
    val xpToNextLevel: Int get() = xpLevel * 100

    /** 0f..1f progress toward the next level. */
    val xpProgress: Float get() = (currentXp.coerceIn(0, xpToNextLevel)).toFloat() / xpToNextLevel
}

/**
 * Award Loaney Pie points when a user makes a loan payment.
 *
 * Scoring rules
 * ─────────────
 * • Base  : 1 point per 100 BDT paid (min 1 pt)
 * • Early : +50 % bonus when [daysBeforeDue] > 0
 * • XP    : points × (1 + 0.1 × (xpLevel − 1))  → every level adds 10 %
 */
fun calculateLoaneyPiePoints(
    amountPaid: Double,
    daysBeforeDue: Int,
    userXpLevel: Int
): Int {
    if (amountPaid <= 0) return 0

    val basePoints = (amountPaid / 100.0).coerceAtLeast(1.0)
    val earlyBonus = if (daysBeforeDue > 0) 1.5 else 1.0
    val xpMultiplier = 1.0 + 0.1 * (userXpLevel - 1).coerceAtLeast(0)

    return (basePoints * earlyBonus * xpMultiplier).toInt().coerceAtLeast(1)
}
