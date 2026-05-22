package com.example.emicalculator.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.emicalculator.pdf.PdfGenerator
import com.example.emicalculator.viewmodel.EMIViewModel

// ─────────────────────────────────────────────────────────────────────────────
//  LAYER 3 — VIEW  (the Screen Composable)
//
//  This is the "glue" between ViewModel and Components.
//  It has three jobs:
//    1. Get the ViewModel instance
//    2. Observe the state (collectAsStateWithLifecycle)
//    3. Build the layout — passing state DOWN to components,
//       and passing ViewModel event handlers DOWN as callbacks
//
//  The key rule of MVVM in Compose:
//    • Data flows DOWN  (ViewModel → Screen → Components)
//    • Events flow UP   (Components → Screen → ViewModel)
//
//  This is called "Unidirectional Data Flow" (UDF) and it makes apps
//  much easier to debug — there's only ONE place state can change.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EMIScreen(
    // `viewModel()` is a special Compose function that:
    //   • Creates EMIViewModel the first time this screen is shown
    //   • Returns the SAME instance if the screen recomposes (e.g. on rotation)
    // This is why ViewModel data survives screen rotation.
    viewModel: EMIViewModel = viewModel()
) {
    // ── Observe State ─────────────────────────────────────────────────────────
    //
    // `collectAsStateWithLifecycle` turns the StateFlow into a Compose State.
    // Whenever _uiState changes in the ViewModel, `state` here gets the new
    // value and Compose automatically redraws only the parts that changed.
    //
    // The `by` keyword (delegate) lets us write `state.amount` instead of
    // `state.value.amount`. Just a Kotlin convenience.

    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── PDF share handler ─────────────────────────────────────────────────────
    //
    // PdfGenerator.create() runs on the calling thread. For a lightweight one-page
    // report this is fast enough to do on the main thread, but we keep the lambda
    // here (close to where context lives) to avoid passing context into the ViewModel.
    val onSharePdf: () -> Unit = {
        val uri = PdfGenerator.create(context, state)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type  = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "EMI Loan Summary Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share EMI Report via"))
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()                         // respect status bar + nav bar
            .verticalScroll(rememberScrollState())       // scrollable in case of small screen
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text     = "EMI Calculator",
            style    = MaterialTheme.typography.headlineMedium,
            color    = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Amount Row ────────────────────────────────────────────────────────
        // CalcRow wraps the field box; NumberInputField goes inside as "content"
        // Notice: the UI passes the ViewModel's event handler as the callback.
        // When the user types, onValueChange fires → calls viewModel.onAmountChange()
        // → ViewModel updates state → UI recomposes with new value. Full loop.

        CalcRow(label = "Amount", isSelected = false) {
            NumberInputField(
                value         = state.amount,
                placeholder   = "0",
                onValueChange = viewModel::onAmountChange,   // passes the function reference
                modifier      = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Interest % Row ────────────────────────────────────────────────────
        CalcRow(label = "Interest %", isSelected = false) {
            NumberInputField(
                value         = state.interestRate,
                placeholder   = "0.00",
                onValueChange = viewModel::onInterestRateChange,
                modifier      = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Period Row (with Years / Months toggle) ───────────────────────────
        CalcRow(label = "Period", isSelected = false) {
            NumberInputField(
                value         = state.period,
                placeholder   = "0",
                onValueChange = viewModel::onPeriodChange,
                modifier      = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            YearsMonthsToggle(
                isYears  = state.isYears,
                onToggle = viewModel::onPeriodTypeChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── EMI Result Row ────────────────────────────────────────────────────
        // This row is read-only (isSelected = true = filled radio dot).
        // It shows the calculated EMI, or "—" if inputs are incomplete.

        CalcRow(label = "EMI", isSelected = true) {
            Text(
                text  = if (state.emi.isNotEmpty()) state.emi else "—",
                style = MaterialTheme.typography.titleMedium,
                color = if (state.emi.isNotEmpty())
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Summary Card ──────────────────────────────────────────────────────
        // AnimatedVisibility smoothly slides the card in/out as the user
        // fills or clears the inputs. No extra work — Compose handles the animation.

        AnimatedVisibility(
            visible = state.emi.isNotEmpty(),
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(28.dp))
                SummaryCard(
                    emi           = state.emi,
                    totalInterest = state.totalInterest,
                    totalAmount   = state.totalAmount
                )
            }
        }

        // ── Share as PDF Button ───────────────────────────────────────────────
        // Only visible once a valid EMI has been calculated.
        // Generates a styled A4 PDF and opens the OS share sheet.

        AnimatedVisibility(
            visible = state.emi.isNotEmpty(),
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                SharePdfButton(onClick = onSharePdf)
            }
        }

        // ── Clear Button ──────────────────────────────────────────────────────
        // Appears (with animation) as soon as the user starts typing in any field.
        // Tapping it calls viewModel.clearAll() which resets every input + result.

        val hasData = state.amount.isNotEmpty() ||
                      state.interestRate.isNotEmpty() ||
                      state.period.isNotEmpty()

        AnimatedVisibility(
            visible = hasData,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                ClearButton(onClick = viewModel::clearAll)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
