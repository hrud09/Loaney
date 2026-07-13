package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.entity.DepositEntity
import com.sbs.loaney.data.repository.ILoanRepository
import com.sbs.loaney.data.repository.SettingsRepository
import com.sbs.loaney.util.DepositCalculator
import com.sbs.loaney.util.DepositProjection
import com.sbs.loaney.util.DepositType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DepositUiState(
    // Calculator inputs. Kept as text so a half-typed field doesn't collapse to 0.
    val type: DepositType = DepositType.DPS,
    val bankName: String = "",
    val amountInput: String = "",
    val rateInput: String = "",
    val tenureInput: String = "",
    val compoundingsPerYear: Int = DepositCalculator.DEFAULT_COMPOUNDINGS_PER_YEAR,
    val hasTin: Boolean = true,

    val deposits: List<DepositEntity> = emptyList(),
    val currencySymbol: String = "৳"
) {
    val amount: Double get() = amountInput.toDoubleOrNull() ?: 0.0
    val annualRate: Double get() = rateInput.toDoubleOrNull() ?: 0.0
    val tenureMonths: Int get() = tenureInput.toIntOrNull() ?: 0

    val taxRatePercent: Double
        get() = if (hasTin) DepositCalculator.TAX_RATE_WITH_TIN else DepositCalculator.TAX_RATE_WITHOUT_TIN

    val projection: DepositProjection
        get() = DepositCalculator.project(
            type = type,
            amount = amount,
            annualRatePercent = annualRate,
            tenureMonths = tenureMonths,
            compoundingsPerYear = compoundingsPerYear,
            taxRatePercent = taxRatePercent
        )

    /** What the user would walk away with by breaking the deposit halfway through. */
    val prematureProjection: DepositProjection
        get() = DepositCalculator.projectPrematureEncashment(
            type = type,
            amount = amount,
            tenureMonths = tenureMonths,
            atMonth = tenureMonths / 2,
            compoundingsPerYear = compoundingsPerYear,
            taxRatePercent = taxRatePercent
        )

    val hasProjection: Boolean get() = amount > 0.0 && tenureMonths > 0
    val canSave: Boolean get() = hasProjection && bankName.isNotBlank()

    // Maturity is derived from the date rather than trusting the stored isMatured flag,
    // so a deposit becomes "matured" on its own without a background job to flip it.
    val activeDeposits: List<DepositEntity>
        get() = deposits.filter { !it.isMatured && it.maturityDate > System.currentTimeMillis() }

    val maturedDeposits: List<DepositEntity>
        get() = deposits.filter { it.isMatured || it.maturityDate <= System.currentTimeMillis() }

    val totalInvested: Double get() = activeDeposits.sumOf { it.principalAmount }
    val totalProjectedValue: Double get() = activeDeposits.sumOf { it.maturityAmount }
}

@HiltViewModel
class DepositViewModel @Inject constructor(
    private val repository: ILoanRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepositUiState())
    val uiState: StateFlow<DepositUiState> = _uiState.asStateFlow()

    init {
        repository.getAllDeposits()
            .onEach { list -> _uiState.update { it.copy(deposits = list) } }
            .launchIn(viewModelScope)

        settingsRepository.currencySymbolFlow
            .onEach { symbol -> _uiState.update { it.copy(currencySymbol = symbol) } }
            .launchIn(viewModelScope)
    }

    fun setType(type: DepositType) = _uiState.update { it.copy(type = type) }
    fun setBankName(name: String) = _uiState.update { it.copy(bankName = name) }
    fun setAmount(input: String) = _uiState.update { it.copy(amountInput = input.filterNumeric()) }
    fun setRate(input: String) = _uiState.update { it.copy(rateInput = input.filterNumeric()) }
    fun setTenure(input: String) = _uiState.update { it.copy(tenureInput = input.filter { c -> c.isDigit() }) }
    fun setCompoundingsPerYear(n: Int) = _uiState.update { it.copy(compoundingsPerYear = n) }
    fun setHasTin(hasTin: Boolean) = _uiState.update { it.copy(hasTin = hasTin) }

    fun saveDeposit() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            val startDate = System.currentTimeMillis()
            val deposit = DepositEntity(
                type = state.type.name,
                bankName = state.bankName,
                monthlyDeposit = if (state.type == DepositType.DPS) state.amount else 0.0,
                principalAmount = state.projection.totalDeposited,
                annualRate = state.annualRate,
                tenureMonths = state.tenureMonths,
                // Store the NET figure so the tracker can never disagree with the calculator.
                maturityAmount = state.projection.netMaturityAmount,
                startDate = startDate,
                maturityDate = DepositCalculator.maturityDate(startDate, state.tenureMonths),
                associatedBankAccountId = null,
                compoundingsPerYear = state.compoundingsPerYear,
                taxRatePercent = state.taxRatePercent
            )
            repository.insertDeposit(deposit)
            _uiState.update {
                it.copy(bankName = "", amountInput = "", rateInput = "", tenureInput = "")
            }
        }
    }

    fun deleteDeposit(deposit: DepositEntity) {
        viewModelScope.launch { repository.deleteDeposit(deposit) }
    }

    /** Digits plus at most one decimal point. */
    private fun String.filterNumeric(): String {
        val cleaned = filter { it.isDigit() || it == '.' }
        val firstDot = cleaned.indexOf('.')
        if (firstDot == -1) return cleaned
        return cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).filter { it.isDigit() }
    }
}
