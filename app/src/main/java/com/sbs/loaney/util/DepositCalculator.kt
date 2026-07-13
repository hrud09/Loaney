package com.sbs.loaney.util

import java.util.Calendar
import kotlin.math.pow

enum class DepositType { DPS, FDR }

data class DepositProjection(
    /** DPS: monthly deposit x tenure. FDR: the lump sum. */
    val totalDeposited: Double,
    val grossInterest: Double,
    val taxOnInterest: Double,
    val netMaturityAmount: Double,
    /** Net interest as an annualised percentage of what was actually put in. */
    val effectiveAnnualYield: Double
)

object DepositCalculator {

    /** Bangladesh source tax (AIT) on deposit interest. */
    const val TAX_RATE_WITH_TIN = 10.0
    const val TAX_RATE_WITHOUT_TIN = 15.0

    /** Quarterly is the norm for Bangladeshi banks. */
    const val DEFAULT_COMPOUNDINGS_PER_YEAR = 4

    /** Banks typically drop you to roughly the savings rate if you break early. */
    const val DEFAULT_PREMATURE_RATE = 3.0

    /**
     * @param amount DPS: the monthly deposit. FDR: the lump sum principal.
     * @param taxRatePercent applied to interest only, never to principal.
     */
    fun project(
        type: DepositType,
        amount: Double,
        annualRatePercent: Double,
        tenureMonths: Int,
        compoundingsPerYear: Int = DEFAULT_COMPOUNDINGS_PER_YEAR,
        taxRatePercent: Double = TAX_RATE_WITH_TIN
    ): DepositProjection {
        if (amount <= 0.0 || tenureMonths <= 0) return EMPTY

        val totalDeposited = when (type) {
            DepositType.DPS -> amount * tenureMonths
            DepositType.FDR -> amount
        }

        val grossMaturity = when (type) {
            DepositType.DPS -> dpsFutureValue(amount, annualRatePercent, tenureMonths, compoundingsPerYear)
            DepositType.FDR -> fdrFutureValue(amount, annualRatePercent, tenureMonths, compoundingsPerYear)
        }

        val grossInterest = (grossMaturity - totalDeposited).coerceAtLeast(0.0)
        val tax = grossInterest * (taxRatePercent.coerceIn(0.0, 100.0) / 100.0)
        val net = grossMaturity - tax

        val years = tenureMonths / 12.0
        val yield_ = if (totalDeposited > 0.0 && years > 0.0) {
            ((grossInterest - tax) / totalDeposited) / years * 100.0
        } else 0.0

        return DepositProjection(
            totalDeposited = totalDeposited,
            grossInterest = grossInterest,
            taxOnInterest = tax,
            netMaturityAmount = net,
            effectiveAnnualYield = yield_
        )
    }

    /**
     * What you'd actually walk away with by breaking the deposit at [atMonth].
     * Banks recompute the whole term at a penalty rate rather than pro-rating the contracted one.
     */
    fun projectPrematureEncashment(
        type: DepositType,
        amount: Double,
        tenureMonths: Int,
        atMonth: Int,
        prematureAnnualRatePercent: Double = DEFAULT_PREMATURE_RATE,
        compoundingsPerYear: Int = DEFAULT_COMPOUNDINGS_PER_YEAR,
        taxRatePercent: Double = TAX_RATE_WITH_TIN
    ): DepositProjection {
        val heldMonths = atMonth.coerceIn(0, tenureMonths)
        if (heldMonths <= 0) return EMPTY
        return project(
            type = type,
            amount = amount,
            annualRatePercent = prematureAnnualRatePercent,
            tenureMonths = heldMonths,
            compoundingsPerYear = compoundingsPerYear,
            taxRatePercent = taxRatePercent
        )
    }

    /** Compound interest on a lump sum: A = P(1 + r/n)^(nt) */
    private fun fdrFutureValue(
        principal: Double,
        annualRatePercent: Double,
        tenureMonths: Int,
        compoundingsPerYear: Int
    ): Double {
        if (annualRatePercent <= 0.0) return principal
        val n = compoundingsPerYear.coerceAtLeast(1)
        val r = annualRatePercent / 100.0
        val t = tenureMonths / 12.0
        return principal * (1.0 + r / n).pow(n * t)
    }

    /**
     * Future value of a monthly recurring deposit (ordinary annuity, deposit at period end):
     * FV = M * [((1+i)^k - 1) / i]
     */
    private fun dpsFutureValue(
        monthlyDeposit: Double,
        annualRatePercent: Double,
        tenureMonths: Int,
        compoundingsPerYear: Int
    ): Double {
        // No interest: you get back exactly what you put in. Also guards the /i below.
        if (annualRatePercent <= 0.0) return monthlyDeposit * tenureMonths

        val i = effectiveMonthlyRate(annualRatePercent, compoundingsPerYear)
        if (i <= 0.0) return monthlyDeposit * tenureMonths

        return monthlyDeposit * (((1.0 + i).pow(tenureMonths) - 1.0) / i)
    }

    /**
     * Deposits land monthly but the bank may compound quarterly, so convert the nominal
     * annual rate into the monthly rate that yields the same effective annual growth.
     */
    private fun effectiveMonthlyRate(annualRatePercent: Double, compoundingsPerYear: Int): Double {
        val n = compoundingsPerYear.coerceAtLeast(1)
        val r = annualRatePercent / 100.0
        if (n == 12) return r / 12.0
        return (1.0 + r / n).pow(n / 12.0) - 1.0
    }

    /** Calendar-aware, so a 3-month deposit lands on the same day-of-month, not 90 days later. */
    fun maturityDate(startDate: Long, tenureMonths: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startDate
            add(Calendar.MONTH, tenureMonths)
        }
        return cal.timeInMillis
    }

    private val EMPTY = DepositProjection(0.0, 0.0, 0.0, 0.0, 0.0)
}
