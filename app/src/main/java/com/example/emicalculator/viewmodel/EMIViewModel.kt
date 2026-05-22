package com.example.emicalculator.viewmodel

import androidx.lifecycle.ViewModel
import com.example.emicalculator.model.EMIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

// ─────────────────────────────────────────────────────────────────────────────
//  LAYER 2 — VIEWMODEL
//
//  The ViewModel is the "brain" of your screen. It:
//    1. Holds the single source of truth for the UI state (EMIState)
//    2. Exposes event-handler functions the UI can call
//    3. Runs all business logic (EMI calculation)
//    4. Survives screen rotations — your UI doesn't lose data when
//       the phone is flipped. This is the #1 reason to use ViewModel.
//
//  What the ViewModel does NOT do:
//    ✗ Import anything from androidx.compose (no Composables here)
//    ✗ Hold a reference to Context, Activity, or View
//    ✗ Touch the UI directly
// ─────────────────────────────────────────────────────────────────────────────

class EMIViewModel : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────
    //
    // StateFlow is a stream that always holds the latest value.
    // Think of it like a live variable the UI can "watch".
    //
    // WHY two variables (_uiState and uiState)?
    //
    //  _uiState  → MutableStateFlow  — PRIVATE, only ViewModel can modify it.
    //   uiState  → StateFlow         — PUBLIC, read-only. The UI collects from this.
    //
    // This pattern is called "backing property". It prevents the UI from
    // accidentally changing state directly — all changes must go through
    // the event functions below. This keeps data flow one-directional:
    //
    //   User taps → UI calls ViewModel function → ViewModel updates _uiState
    //   → uiState emits new value → UI recomposes automatically

    private val _uiState = MutableStateFlow(EMIState())   // starts with empty/default state
    val uiState: StateFlow<EMIState> = _uiState.asStateFlow()

    // ── Event Handlers ────────────────────────────────────────────────────────
    //
    // Each function below is called by the UI when the user does something.
    // They update the relevant field in state, then trigger a recalculation.
    //
    // WHY use .update { it.copy(...) } instead of just assigning?
    // StateFlow is designed to be updated atomically. `.update` ensures
    // thread-safety. `.copy()` creates a new EMIState with only the changed
    // field — all other fields stay the same.

    fun onAmountChange(newValue: String) {
        _uiState.update { currentState ->
            currentState.copy(amount = newValue)
        }
        recalculate()
    }

    fun onInterestRateChange(newValue: String) {
        _uiState.update { it.copy(interestRate = newValue) }
        recalculate()
    }

    fun onPeriodChange(newValue: String) {
        _uiState.update { it.copy(period = newValue) }
        recalculate()
    }

    fun onPeriodTypeChange(isYears: Boolean) {
        _uiState.update { it.copy(isYears = isYears) }
        recalculate()
    }

    /** Resets every input and result back to the initial empty state. */
    fun clearAll() {
        _uiState.update { EMIState() }
    }

    // ── Business Logic ────────────────────────────────────────────────────────
    //
    // This is private — the UI has no idea this function exists.
    // It runs automatically every time any input changes.

    private fun recalculate() {
        val state = _uiState.value

        // Try to parse each input. If any field is empty or invalid,
        // clear the results and stop. The `?: run { ... ; return }` pattern
        // means: "if null, execute the block and exit the function."
        val principal   = state.amount.toDoubleOrNull()       ?: run { clearResults(); return }
        val annualRate  = state.interestRate.toDoubleOrNull() ?: run { clearResults(); return }
        val periodValue = state.period.toDoubleOrNull()       ?: run { clearResults(); return }

        // Guard against zero or negative values
        if (principal <= 0 || annualRate <= 0 || periodValue <= 0) {
            clearResults()
            return
        }

        // Convert period to months (EMI formula always works in months)
        val totalMonths: Double = if (state.isYears) periodValue * 12 else periodValue

        // ── EMI Formula ───────────────────────────────────────────────────────
        //
        //         P × r × (1 + r)ⁿ
        //  EMI = ─────────────────────
        //          (1 + r)ⁿ − 1
        //
        //  Where:
        //    P = Principal loan amount
        //    r = Monthly interest rate = Annual rate / (12 × 100)
        //    n = Total number of months
        //
        //  Special case: if interest is 0%, the formula breaks (divide by zero).
        //  In that case, EMI is simply principal ÷ months.

        val monthlyRate: Double = annualRate / (12.0 * 100.0)

        val emi: Double = if (monthlyRate == 0.0) {
            principal / totalMonths
        } else {
            val compoundFactor = (1 + monthlyRate).pow(totalMonths)
            principal * monthlyRate * compoundFactor / (compoundFactor - 1)
        }

        val totalPayment  = emi * totalMonths
        val totalInterest = totalPayment - principal

        // Push the new results into state — UI will automatically recompose
        _uiState.update {
            it.copy(
                emi           = formatCurrency(emi),
                totalInterest = formatCurrency(totalInterest),
                totalAmount   = formatCurrency(totalPayment)
            )
        }
    }

    /** Resets all result fields to empty (shown as "—" in the UI). */
    private fun clearResults() {
        _uiState.update { it.copy(emi = "", totalInterest = "", totalAmount = "") }
    }

    /**
     * Formats a Double as a locale-aware integer currency string.
     * e.g.  10911.45  →  "10,911"  (in en-US locale)
     *                 →  "10.911"  (in de-DE locale)
     */
    private fun formatCurrency(value: Double): String {
        return NumberFormat.getNumberInstance(Locale.getDefault())
            .apply { maximumFractionDigits = 0 }
            .format(value.toLong())
    }
}
