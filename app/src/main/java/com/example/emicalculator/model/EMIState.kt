package com.example.emicalculator.model

// ─────────────────────────────────────────────────────────────────────────────
//  LAYER 1 — MODEL
//
//  The Model is the "what" of your app — what data exists.
//  It has NO logic, NO calculations, NO UI. Just a data container.
//
//  Rule to remember:
//    "If it holds data → put it in the Model."
//    "If it calculates → put it in the ViewModel."
//    "If it draws pixels → put it in the View (Composable)."
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents the complete state of the EMI Calculator screen at any moment.
 *
 * WHY a data class?
 * Kotlin `data class` automatically generates:
 *   • equals()  — compare two states for equality
 *   • copy()    — create a modified copy (used in ViewModel to update state)
 *   • toString() — readable debug output
 *
 * WHY default values?
 * So we can create an initial empty state: EMIState()
 * The ViewModel starts with this and fills it in as the user types.
 */
data class EMIState(

    // ── User Inputs ──────────────────────────────────────────────────────────
    // Stored as String because text fields always give us strings.
    // We convert to Double only when we calculate (in ViewModel).

    val amount: String = "",          // Loan principal amount (e.g. "350000")
    val interestRate: String = "",    // Annual interest rate  (e.g. "7.65")
    val period: String = "",          // Loan duration         (e.g. "3")
    val isYears: Boolean = true,      // true = period is in years, false = months

    // ── Calculated Results ───────────────────────────────────────────────────
    // Empty string = "not yet calculated" (shown as "—" in the UI)

    val emi: String = "",             // Monthly EMI amount
    val totalInterest: String = "",   // Total interest paid over the full period
    val totalAmount: String = ""      // Total amount paid  = Principal + Interest
)
