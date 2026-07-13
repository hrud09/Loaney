package com.sbs.loaney.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DepositCalculatorTest {

    private val delta = 0.01

    // ── FDR ──────────────────────────────────────────────────────────────────

    /**
     * 100,000 at 6% for 12 months, compounded quarterly.
     * A = 100000 * (1 + 0.06/4)^4 = 100000 * 1.015^4 = 106,136.36
     */
    @Test
    fun `FDR compounds quarterly`() {
        val p = DepositCalculator.project(
            type = DepositType.FDR,
            amount = 100_000.0,
            annualRatePercent = 6.0,
            tenureMonths = 12,
            compoundingsPerYear = 4,
            taxRatePercent = 0.0
        )

        assertEquals(100_000.0, p.totalDeposited, delta)
        assertEquals(6_136.36, p.grossInterest, delta)
        assertEquals(106_136.36, p.netMaturityAmount, delta)
    }

    @Test
    fun `FDR at zero rate returns the principal untouched`() {
        val p = DepositCalculator.project(
            type = DepositType.FDR,
            amount = 50_000.0,
            annualRatePercent = 0.0,
            tenureMonths = 36
        )

        assertEquals(0.0, p.grossInterest, delta)
        assertEquals(0.0, p.taxOnInterest, delta)
        assertEquals(50_000.0, p.netMaturityAmount, delta)
    }

    // ── DPS ──────────────────────────────────────────────────────────────────

    /**
     * 5,000/month at 6% for 12 months, compounded monthly (i = 0.005).
     * FV = 5000 * ((1.005^12 - 1) / 0.005) = 61,677.81
     */
    @Test
    fun `DPS accrues as a monthly annuity`() {
        val p = DepositCalculator.project(
            type = DepositType.DPS,
            amount = 5_000.0,
            annualRatePercent = 6.0,
            tenureMonths = 12,
            compoundingsPerYear = 12,
            taxRatePercent = 0.0
        )

        assertEquals(60_000.0, p.totalDeposited, delta)
        assertEquals(1_677.81, p.grossInterest, delta)
        assertEquals(61_677.81, p.netMaturityAmount, delta)
    }

    /** The /i in the annuity formula divides by zero at 0%; it must fall back to plain contributions. */
    @Test
    fun `DPS at zero rate does not divide by zero`() {
        val p = DepositCalculator.project(
            type = DepositType.DPS,
            amount = 1_000.0,
            annualRatePercent = 0.0,
            tenureMonths = 24
        )

        assertEquals(24_000.0, p.totalDeposited, delta)
        assertEquals(0.0, p.grossInterest, delta)
        assertEquals(24_000.0, p.netMaturityAmount, delta)
        assertTrue(p.netMaturityAmount.isFinite())
    }

    /** Quarterly compounding earns slightly less than monthly at the same nominal rate. */
    @Test
    fun `DPS compounding frequency changes the outcome`() {
        fun at(n: Int) = DepositCalculator.project(
            type = DepositType.DPS,
            amount = 5_000.0,
            annualRatePercent = 6.0,
            tenureMonths = 60,
            compoundingsPerYear = n,
            taxRatePercent = 0.0
        ).netMaturityAmount

        assertTrue("quarterly should trail monthly", at(4) < at(12))
    }

    // ── Tax ──────────────────────────────────────────────────────────────────

    @Test
    fun `tax applies to interest only, never to principal`() {
        val taxed = DepositCalculator.project(
            type = DepositType.FDR,
            amount = 100_000.0,
            annualRatePercent = 6.0,
            tenureMonths = 12,
            compoundingsPerYear = 4,
            taxRatePercent = DepositCalculator.TAX_RATE_WITH_TIN
        )

        // Interest 6,136.36; 10% of that is 613.64.
        assertEquals(613.64, taxed.taxOnInterest, delta)
        assertEquals(105_522.72, taxed.netMaturityAmount, delta)
        assertTrue("principal must survive taxation", taxed.netMaturityAmount > 100_000.0)
    }

    @Test
    fun `no TIN is taxed harder than TIN`() {
        fun at(rate: Double) = DepositCalculator.project(
            type = DepositType.FDR,
            amount = 100_000.0,
            annualRatePercent = 6.0,
            tenureMonths = 12,
            compoundingsPerYear = 4,
            taxRatePercent = rate
        )

        val withTin = at(DepositCalculator.TAX_RATE_WITH_TIN)
        val withoutTin = at(DepositCalculator.TAX_RATE_WITHOUT_TIN)

        assertEquals(613.64, withTin.taxOnInterest, delta)
        assertEquals(920.45, withoutTin.taxOnInterest, delta)
        assertTrue(withoutTin.netMaturityAmount < withTin.netMaturityAmount)
        // Gross interest is identical; only the tax differs.
        assertEquals(withTin.grossInterest, withoutTin.grossInterest, delta)
    }

    // ── Premature encashment ─────────────────────────────────────────────────

    @Test
    fun `breaking early yields less than holding to maturity`() {
        val full = DepositCalculator.project(
            type = DepositType.FDR,
            amount = 100_000.0,
            annualRatePercent = 8.0,
            tenureMonths = 36
        )
        val broken = DepositCalculator.projectPrematureEncashment(
            type = DepositType.FDR,
            amount = 100_000.0,
            tenureMonths = 36,
            atMonth = 12,
            prematureAnnualRatePercent = DepositCalculator.DEFAULT_PREMATURE_RATE
        )

        assertTrue(broken.netMaturityAmount < full.netMaturityAmount)
        assertTrue("should still beat losing the principal", broken.netMaturityAmount > 100_000.0)
    }

    @Test
    fun `encashing at month zero yields nothing`() {
        val p = DepositCalculator.projectPrematureEncashment(
            type = DepositType.DPS,
            amount = 5_000.0,
            tenureMonths = 36,
            atMonth = 0
        )
        assertEquals(0.0, p.netMaturityAmount, delta)
    }

    // ── Guards & dates ───────────────────────────────────────────────────────

    @Test
    fun `non-positive inputs produce an empty projection`() {
        val zeroAmount = DepositCalculator.project(DepositType.FDR, 0.0, 6.0, 12)
        val zeroTenure = DepositCalculator.project(DepositType.FDR, 100_000.0, 6.0, 0)

        assertEquals(0.0, zeroAmount.netMaturityAmount, delta)
        assertEquals(0.0, zeroTenure.netMaturityAmount, delta)
    }

    /** Adding months must be calendar-aware, not startDate + 30*n days. */
    @Test
    fun `maturity date advances by calendar months`() {
        val start = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 15, 0, 0, 0) }
        val maturity = Calendar.getInstance().apply {
            timeInMillis = DepositCalculator.maturityDate(start.timeInMillis, 3)
        }

        assertEquals(2026, maturity.get(Calendar.YEAR))
        assertEquals(Calendar.APRIL, maturity.get(Calendar.MONTH))
        assertEquals(15, maturity.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `maturity date rolls over the year boundary`() {
        val start = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 10, 0, 0, 0) }
        val maturity = Calendar.getInstance().apply {
            timeInMillis = DepositCalculator.maturityDate(start.timeInMillis, 4)
        }

        assertEquals(2027, maturity.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, maturity.get(Calendar.MONTH))
    }
}
