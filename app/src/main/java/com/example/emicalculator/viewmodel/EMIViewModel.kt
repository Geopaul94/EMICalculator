package com.example.emicalculator.viewmodel

import androidx.lifecycle.ViewModel
import com.example.emicalculator.model.EMIState
import com.example.emicalculator.model.SolveFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

class EMIViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EMIState())
    val uiState: StateFlow<EMIState> = _uiState.asStateFlow()

    // ── Mode switch ───────────────────────────────────────────────────────────

    // Switching what to solve for resets all fields — the previously calculated
    // value in one field could confuse the user if silently reused as an input.
    fun onSolveForChange(solveFor: SolveFor) {
        _uiState.update { EMIState(solveFor = solveFor, isYears = it.isYears) }
    }

    // ── Input event handlers ──────────────────────────────────────────────────

    fun onAmountChange(v: String)       { _uiState.update { it.copy(amount = v) };       recalculate() }
    fun onInterestRateChange(v: String) { _uiState.update { it.copy(interestRate = v) }; recalculate() }
    fun onPeriodChange(v: String)       { _uiState.update { it.copy(period = v) };       recalculate() }
    fun onEmiChange(v: String)          { _uiState.update { it.copy(emi = v) };          recalculate() }

    fun onPeriodTypeChange(isYears: Boolean) {
        _uiState.update { it.copy(isYears = isYears) }
        recalculate()
    }

    fun clearAll() {
        // Preserve the current mode and period unit — those are user preferences.
        _uiState.update { EMIState(solveFor = it.solveFor, isYears = it.isYears) }
    }

    // ── Calculation dispatcher ────────────────────────────────────────────────

    private fun recalculate() {
        when (_uiState.value.solveFor) {
            SolveFor.EMI           -> solveForEmi()
            SolveFor.AMOUNT        -> solveForAmount()
            SolveFor.INTEREST_RATE -> solveForInterestRate()
            SolveFor.PERIOD        -> solveForPeriod()
        }
    }

    // ── Solve for EMI  (inputs: Amount, Interest %, Period) ───────────────────

    private fun solveForEmi() {
        val s = _uiState.value
        val p = s.amount.toDoubleOrNull()       ?: return clearResult()
        val a = s.interestRate.toDoubleOrNull() ?: return clearResult()
        val t = s.period.toDoubleOrNull()       ?: return clearResult()
        if (p <= 0 || a <= 0 || t <= 0) return clearResult()

        val n = if (s.isYears) t * 12 else t
        val r = a / (12.0 * 100.0)

        val emi = if (r == 0.0) p / n
                  else { val c = (1 + r).pow(n); p * r * c / (c - 1) }

        val total    = emi * n
        val interest = total - p
        _uiState.update { it.copy(emi = fmt(emi), totalInterest = fmt(interest), totalAmount = fmt(total)) }
    }

    // ── Solve for Amount  (inputs: EMI, Interest %, Period) ──────────────────
    //
    //  Reverse of the EMI formula:
    //    P = EMI × ((1+r)^n − 1) / (r × (1+r)^n)

    private fun solveForAmount() {
        val s = _uiState.value
        val e = s.emi.toDoubleOrNull()          ?: return clearResult()
        val a = s.interestRate.toDoubleOrNull() ?: return clearResult()
        val t = s.period.toDoubleOrNull()       ?: return clearResult()
        if (e <= 0 || a <= 0 || t <= 0) return clearResult()

        val n = if (s.isYears) t * 12 else t
        val r = a / (12.0 * 100.0)

        val p = if (r == 0.0) e * n
                else { val c = (1 + r).pow(n); e * (c - 1) / (r * c) }

        val total    = e * n
        val interest = total - p
        _uiState.update { it.copy(amount = fmt(p), totalInterest = fmt(interest), totalAmount = fmt(total)) }
    }

    // ── Solve for Interest Rate  (inputs: Amount, EMI, Period) ───────────────
    //
    //  No closed-form solution exists. We use binary search (bisection) on the
    //  monthly rate `r` in the range (0, 1], converging to 60 decimal places
    //  of precision after 200 iterations — effectively exact.

    private fun solveForInterestRate() {
        val s = _uiState.value
        val p = s.amount.toDoubleOrNull() ?: return clearResult()
        val e = s.emi.toDoubleOrNull()    ?: return clearResult()
        val t = s.period.toDoubleOrNull() ?: return clearResult()
        if (p <= 0 || e <= 0 || t <= 0) return clearResult()

        val n = if (s.isYears) t * 12 else t

        // EMI must be at least p/n (the zero-interest case)
        if (e < p / n - 0.001) return clearResult()

        // Special case: zero interest
        if (e <= p / n + 0.001) {
            val total = e * n
            _uiState.update { it.copy(interestRate = "0.00", totalInterest = fmt(total - p), totalAmount = fmt(total)) }
            return
        }

        val monthlyRate = bisect(p, e, n) ?: return clearResult()
        val annualPct   = monthlyRate * 12.0 * 100.0

        val total    = e * n
        val interest = total - p
        _uiState.update { it.copy(interestRate = fmtRate(annualPct), totalInterest = fmt(interest), totalAmount = fmt(total)) }
    }

    // ── Solve for Period  (inputs: Amount, Interest %, EMI) ──────────────────
    //
    //  From the EMI formula, solving for n (months):
    //    n = ln(EMI / (EMI − P×r)) / ln(1 + r)
    //
    //  Requires EMI > P×r (otherwise the loan can never be repaid).

    private fun solveForPeriod() {
        val s = _uiState.value
        val p = s.amount.toDoubleOrNull()       ?: return clearResult()
        val a = s.interestRate.toDoubleOrNull() ?: return clearResult()
        val e = s.emi.toDoubleOrNull()          ?: return clearResult()
        if (p <= 0 || a <= 0 || e <= 0) return clearResult()

        val r               = a / (12.0 * 100.0)
        val monthlyInterest = p * r

        // EMI must exceed monthly interest or the loan is never paid off
        if (r > 0 && e <= monthlyInterest) return clearResult()

        val n = if (r == 0.0) p / e
                else ln(e / (e - monthlyInterest)) / ln(1 + r)

        if (n <= 0 || n.isNaN() || n.isInfinite()) return clearResult()

        val total    = e * n
        val interest = total - p
        // Display period in the unit the user has selected via the toggle
        val display = if (s.isYears) fmtDecimal(n / 12) else fmtDecimal(n)

        _uiState.update { it.copy(period = display, totalInterest = fmt(interest), totalAmount = fmt(total)) }
    }

    // ── Binary search for monthly rate ────────────────────────────────────────

    private fun bisect(principal: Double, emi: Double, months: Double): Double? {
        fun emiAt(r: Double): Double {
            val c = (1 + r).pow(months)
            return principal * r * c / (c - 1)
        }

        var lo = 1e-9
        var hi = 1.0  // 100 % per month upper bound

        if (emiAt(hi) < emi) return null  // impossible — EMI exceeds any realistic rate

        repeat(200) {
            val mid = (lo + hi) / 2.0
            if (emiAt(mid) < emi) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun clearResult() {
        _uiState.update { s ->
            when (s.solveFor) {
                SolveFor.EMI           -> s.copy(emi = "", totalInterest = "", totalAmount = "")
                SolveFor.AMOUNT        -> s.copy(amount = "", totalInterest = "", totalAmount = "")
                SolveFor.INTEREST_RATE -> s.copy(interestRate = "", totalInterest = "", totalAmount = "")
                SolveFor.PERIOD        -> s.copy(period = "", totalInterest = "", totalAmount = "")
            }
        }
    }

    /** Formats a number as a rounded integer with locale thousands separators. */
    private fun fmt(value: Double): String =
        NumberFormat.getNumberInstance(Locale.getDefault())
            .apply { maximumFractionDigits = 0 }
            .format(value.toLong())

    /** Formats a period with up to 1 decimal place (e.g. "18.5"). */
    private fun fmtDecimal(value: Double): String =
        NumberFormat.getNumberInstance(Locale.getDefault())
            .apply { maximumFractionDigits = 1; minimumFractionDigits = 0 }
            .format(value)

    /** Formats an interest rate with exactly 2 decimal places (e.g. "8.75"). */
    private fun fmtRate(value: Double): String =
        NumberFormat.getNumberInstance(Locale.getDefault())
            .apply { maximumFractionDigits = 2; minimumFractionDigits = 2 }
            .format(value)
}
