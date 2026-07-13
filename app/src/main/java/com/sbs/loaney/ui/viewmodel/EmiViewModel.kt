package com.sbs.loaney.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.data.local.entity.EmiEntity
import com.sbs.loaney.data.repository.ILoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

data class BrandOffer(
    val id: Int,
    val brandName: String,
    val brandColorHex: String,
    val offerTitle: String,
    val description: String,
    val cardPartners: String,
    val products: String,
    val actionUrl: String,
    val category: String
)

data class EmiUiState(
    val emis: List<EmiEntity> = emptyList(),
    val bankAccounts: List<BankAccountEntity> = emptyList(),
    val calculatorAmount: Double = 50000.0,
    val calculatorDownPayment: Double = 10000.0,
    val calculatorRate: Double = 9.0,
    val calculatorTenure: Int = 12,
    val selectedBrandCategory: String = "All",
    val brandOffers: List<BrandOffer> = emptyList()
) {
    val activeEmis: List<EmiEntity> get() = emis.filter { !it.isCompleted }
    val completedEmis: List<EmiEntity> get() = emis.filter { it.isCompleted }
    
    val totalOutstanding: Double get() = activeEmis.sumOf { 
        val principal = it.totalAmount - it.downPayment
        val totalPaidInstallments = it.monthlyInstallment * it.paymentsPaid
        val totalFutureInstallments = it.monthlyInstallment * (it.tenureMonths - it.paymentsPaid)
        totalFutureInstallments
    }

    val monthlyLiability: Double get() = activeEmis.sumOf { it.monthlyInstallment }

    // Calculator results
    val calcPrincipal: Double get() = (calculatorAmount - calculatorDownPayment).coerceAtLeast(0.0)
    val calcMonthlyRate: Double get() = (calculatorRate / 12.0) / 100.0
    val calcMonthlyInstallment: Double get() = when {
        calcPrincipal <= 0.0 -> 0.0
        calculatorRate <= 0.0 -> calcPrincipal / calculatorTenure
        else -> {
            val r = calcMonthlyRate
            val n = calculatorTenure
            val emi = (calcPrincipal * r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1)
            emi
        }
    }
    val calcTotalPayable: Double get() = (calcMonthlyInstallment * calculatorTenure) + calculatorDownPayment
    val calcTotalInterest: Double get() = (calcTotalPayable - calculatorAmount).coerceAtLeast(0.0)
}

@HiltViewModel
class EmiViewModel @Inject constructor(
    private val repository: ILoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmiUiState())
    val uiState: StateFlow<EmiUiState> = _uiState.asStateFlow()

    init {
        // Collect EMIs
        repository.getAllEmis()
            .onEach { list ->
                _uiState.update { it.copy(emis = list) }
            }
            .launchIn(viewModelScope)

        // Collect Bank Accounts
        repository.getAllBankAccounts()
            .onEach { accounts ->
                _uiState.update { it.copy(bankAccounts = accounts) }
            }
            .launchIn(viewModelScope)

        // Load static brand offers
        _uiState.update { it.copy(brandOffers = getMockBrandOffers()) }
    }

    fun updateCalculatorAmount(amount: Double) {
        _uiState.update { it.copy(calculatorAmount = amount) }
    }

    fun updateCalculatorDownPayment(downPayment: Double) {
        _uiState.update { it.copy(calculatorDownPayment = downPayment) }
    }

    fun updateCalculatorRate(rate: Double) {
        _uiState.update { it.copy(calculatorRate = rate) }
    }

    fun updateCalculatorTenure(tenure: Int) {
        _uiState.update { it.copy(calculatorTenure = tenure) }
    }

    fun updateBrandCategory(category: String) {
        _uiState.update { it.copy(selectedBrandCategory = category) }
    }

    fun addEmi(
        itemName: String,
        totalAmount: Double,
        downPayment: Double,
        tenureMonths: Int,
        interestRate: Double,
        monthlyInstallment: Double,
        startDate: Long,
        dueDateDay: Int,
        bankAccountId: Long?
    ) {
        viewModelScope.launch {
            val emi = EmiEntity(
                itemName = itemName,
                totalAmount = totalAmount,
                downPayment = downPayment,
                tenureMonths = tenureMonths,
                interestRate = interestRate,
                monthlyInstallment = monthlyInstallment,
                startDate = startDate,
                dueDateDay = dueDateDay,
                associatedBankAccountId = bankAccountId,
                paymentsPaid = 0,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
            repository.insertEmi(emi)
        }
    }

    fun payInstallment(emi: EmiEntity) {
        viewModelScope.launch {
            val nextPaymentsPaid = emi.paymentsPaid + 1
            val completed = nextPaymentsPaid >= emi.tenureMonths
            val updatedEmi = emi.copy(
                paymentsPaid = nextPaymentsPaid.coerceAtMost(emi.tenureMonths),
                isCompleted = completed
            )
            repository.updateEmi(updatedEmi)
        }
    }

    fun deleteEmi(emi: EmiEntity) {
        viewModelScope.launch {
            repository.deleteEmi(emi)
        }
    }

    private fun getMockBrandOffers(): List<BrandOffer> {
        return listOf(
            BrandOffer(
                id = 1,
                brandName = "Apple - Executive Machines",
                brandColorHex = "#000000",
                offerTitle = "0% Interest EMI up to 24 Months",
                description = "Get any iPhone 15 series, iPad, or Macbook at 0% EMI for up to 24 months with zero downpayment.",
                cardPartners = "City Bank, SCB, EBL, BRAC Bank, HSBC",
                products = "iPhones, iPads, Macbooks, Apple Watches",
                actionUrl = "https://www.apple.com",
                category = "Electronics"
            ),
            BrandOffer(
                id = 2,
                brandName = "Samsung Plaza",
                brandColorHex = "#0A0937",
                offerTitle = "Up to 18 Months EMI & Free Gift",
                description = "Purchase Samsung Galaxy S24 Ultra or any flagship Fold series with 18 months 0% EMI and premium gifts.",
                cardPartners = "Amex, Standard Chartered, BRAC Bank, Mutual Trust Bank",
                products = "Samsung Galaxy Flagships, Neo QLED TVs, Smart Fridges",
                actionUrl = "https://www.samsung.com",
                category = "Electronics"
            ),
            BrandOffer(
                id = 3,
                brandName = "Star Tech",
                brandColorHex = "#EF4444",
                offerTitle = "12 Months 0% Interest Tech Deals",
                description = "Build your custom gaming rig or buy laptops with flat 0% interest rate EMI for up to 12 months.",
                cardPartners = "EBL, City Bank, SCB, DBBL, Prime Bank, UCB",
                products = "Laptops, PC Components, Desktops, Monitors",
                actionUrl = "https://www.startech.com.bd",
                category = "Electronics"
            ),
            BrandOffer(
                id = 4,
                brandName = "Singer Bangladesh",
                brandColorHex = "#1D4ED8",
                offerTitle = "Singer Summer Sale - 0% EMI 12 Months",
                description = "Keep your home cool with direct inverter Air Conditioners. Save up to 15,000 BDT with 0% EMI options.",
                cardPartners = "All major credit cards supported, plus local in-house financing",
                products = "Air Conditioners, Refrigerators, Washing Machines",
                actionUrl = "https://www.singerbd.com",
                category = "Home Appliances"
            ),
            BrandOffer(
                id = 5,
                brandName = "Apex",
                brandColorHex = "#047857",
                offerTitle = "Apex Festive Walk - 6 Months 0% EMI",
                description = "Walk in style! Avail 0% interest EMI on purchase above 10,000 BDT at any official Apex outlet.",
                cardPartners = "City Bank, EBL, BRAC Bank, Mutual Trust Bank",
                products = "Footwear, Leather Bags, Fashion Accessories",
                actionUrl = "https://www.apex4u.com",
                category = "Lifestyle"
            )
        )
    }
}
