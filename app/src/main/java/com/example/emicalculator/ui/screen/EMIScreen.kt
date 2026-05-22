package com.example.emicalculator.ui.screen

import android.content.Intent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.emicalculator.model.SolveFor
import com.example.emicalculator.pdf.PdfGenerator
import com.example.emicalculator.viewmodel.EMIViewModel

@Composable
fun EMIScreen(viewModel: EMIViewModel = viewModel()) {

    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text  = "EMI Calculator",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hint so the user discovers the tap-to-select behaviour
        Text(
            text  = "Tap the dot to choose what to calculate",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Amount row ────────────────────────────────────────────────────────
        CalcRow(
            label        = "Amount",
            isSelected   = state.solveFor == SolveFor.AMOUNT,
            onRadioClick = { viewModel.onSolveForChange(SolveFor.AMOUNT) }
        ) {
            if (state.solveFor == SolveFor.AMOUNT) {
                ResultText(value = state.amount, modifier = Modifier.weight(1f))
            } else {
                NumberInputField(
                    value         = state.amount,
                    placeholder   = "0",
                    onValueChange = viewModel::onAmountChange,
                    modifier      = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Interest % row ────────────────────────────────────────────────────
        CalcRow(
            label        = "Interest %",
            isSelected   = state.solveFor == SolveFor.INTEREST_RATE,
            onRadioClick = { viewModel.onSolveForChange(SolveFor.INTEREST_RATE) }
        ) {
            if (state.solveFor == SolveFor.INTEREST_RATE) {
                // Append the % sign to make the result readable at a glance
                val display = if (state.interestRate.isNotEmpty()) "${state.interestRate} %" else ""
                ResultText(value = display, modifier = Modifier.weight(1f))
            } else {
                NumberInputField(
                    value         = state.interestRate,
                    placeholder   = "0.00",
                    onValueChange = viewModel::onInterestRateChange,
                    modifier      = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Period row ────────────────────────────────────────────────────────
        // The Years/Months toggle is always visible here:
        //   • When Period is INPUT  → toggle controls how the value is interpreted
        //   • When Period is RESULT → toggle controls the display unit of the answer
        CalcRow(
            label        = "Period",
            isSelected   = state.solveFor == SolveFor.PERIOD,
            onRadioClick = { viewModel.onSolveForChange(SolveFor.PERIOD) }
        ) {
            if (state.solveFor == SolveFor.PERIOD) {
                val unit    = if (state.isYears) "Yrs" else "Mo"
                val display = if (state.period.isNotEmpty()) "${state.period} $unit" else ""
                ResultText(value = display, modifier = Modifier.weight(1f))
            } else {
                NumberInputField(
                    value         = state.period,
                    placeholder   = "0",
                    onValueChange = viewModel::onPeriodChange,
                    modifier      = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            YearsMonthsToggle(
                isYears  = state.isYears,
                onToggle = viewModel::onPeriodTypeChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── EMI row ───────────────────────────────────────────────────────────
        CalcRow(
            label        = "EMI",
            isSelected   = state.solveFor == SolveFor.EMI,
            onRadioClick = { viewModel.onSolveForChange(SolveFor.EMI) }
        ) {
            if (state.solveFor == SolveFor.EMI) {
                ResultText(value = state.emi, modifier = Modifier.weight(1f))
            } else {
                NumberInputField(
                    value         = state.emi,
                    placeholder   = "0",
                    onValueChange = viewModel::onEmiChange,
                    modifier      = Modifier.weight(1f)
                )
            }
        }

        // ── Summary card — visible once a full calculation succeeds ───────────
        AnimatedVisibility(
            visible = state.totalAmount.isNotEmpty(),
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

        // ── Share as PDF ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.totalAmount.isNotEmpty(),
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                SharePdfButton(onClick = onSharePdf)
            }
        }

        // ── Clear ─────────────────────────────────────────────────────────────
        val hasData = state.amount.isNotEmpty()       ||
                      state.interestRate.isNotEmpty() ||
                      state.period.isNotEmpty()       ||
                      state.emi.isNotEmpty()

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

// ── Result display ────────────────────────────────────────────────────────────

/** Read-only text shown in a CalcRow when that field is the one being solved for. */
@Composable
private fun ResultText(value: String, modifier: Modifier = Modifier) {
    Text(
        text     = value.ifEmpty { "—" },
        style    = MaterialTheme.typography.titleMedium,
        color    = if (value.isNotEmpty()) MaterialTheme.colorScheme.onBackground
                   else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
