package com.example.emicalculator.model

// Which of the four loan variables the user wants to calculate.
// The filled radio dot on screen always shows the active SolveFor.
enum class SolveFor { AMOUNT, INTEREST_RATE, PERIOD, EMI }

data class EMIState(

    // ── Mode ─────────────────────────────────────────────────────────────────
    val solveFor: SolveFor = SolveFor.EMI,   // what we're calculating

    // ── The four loan variables ───────────────────────────────────────────────
    // Any one of these is the *result* (set by ViewModel after calculation).
    // The other three are *inputs* (set by the user via text fields).
    // Which is which is determined by `solveFor`.

    val amount: String = "",          // Loan principal
    val interestRate: String = "",    // Annual interest rate (%)
    val period: String = "",          // Loan duration (years or months)
    val isYears: Boolean = true,      // Unit for period input/display
    val emi: String = "",             // Monthly payment

    // ── Summary (always derived, never user-entered) ───────────────────────
    val totalInterest: String = "",
    val totalAmount: String = ""
)
