package com.example.emicalculator.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.emicalculator.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  Reusable UI Components
//
//  WHY a separate file for components?
//  EMIScreen.kt would become very long if every piece of UI was defined
//  inline. Breaking components out here makes each piece:
//    • Reusable in other screens
//    • Easier to read in isolation
//    • Easier to test individually
//
//  These are "dumb" composables — they receive data via parameters and
//  fire callbacks (lambdas) when the user interacts. They don't know
//  about ViewModels or business logic.
// ─────────────────────────────────────────────────────────────────────────────

// ── CalcRow ──────────────────────────────────────────────────────────────────

/**
 * A single row in the calculator: [radio] [label] [field content]
 *
 * @param label      The row label shown on the left (e.g. "Amount")
 * @param isSelected Whether the radio circle is filled (true = this is the result row)
 * @param content    The field content — defined by the caller using a "slot" (lambda)
 *
 * WHY use a content lambda (slot pattern)?
 * It lets each row decide what goes inside the field box:
 *   • Input rows put a BasicTextField inside
 *   • The Period row puts a TextField + Years/Months toggle inside
 *   • The EMI row puts a plain Text inside
 * One component, four different uses.
 */
@Composable
fun CalcRow(
    label: String,
    isSelected: Boolean,
    content: @Composable RowScope.() -> Unit   // "slot" — caller fills this in
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {

        // ── Radio Button (decorative circle) ──────────────────────────────────
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(width = 2.dp, color = AccentBlue, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // If this is the "result" row, fill the centre dot
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = AccentBlue, shape = CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // ── Row Label ─────────────────────────────────────────────────────────
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(92.dp)   // fixed width keeps all fields aligned
        )

        Spacer(modifier = Modifier.width(8.dp))

        // ── Field Box ─────────────────────────────────────────────────────────
        // The `isSelected` flag slightly changes the background color to
        // visually distinguish the result from the input fields.
        Row(
            modifier = Modifier
                .weight(1f)                            // take all remaining width
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                    else            MaterialTheme.colorScheme.surface
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()   // caller fills this area
        }
    }
}

// ── NumberInputField ──────────────────────────────────────────────────────────

/**
 * A styled numeric text input field.
 *
 * WHY BasicTextField instead of OutlinedTextField?
 * BasicTextField gives us full control over appearance. The built-in
 * OutlinedTextField comes with its own padding and border that would
 * conflict with the CalcRow field box styling.
 *
 * @param value         Current text value (from state)
 * @param placeholder   Shown when the field is empty (e.g. "0.00")
 * @param onValueChange Called every time the user types — we pass the new value
 *                      up to the ViewModel via this callback
 */
@Composable
fun NumberInputField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = { rawInput ->
            // Sanitise: only allow digits and a single decimal point
            val cleaned = rawInput.filter { it.isDigit() || it == '.' }
            val dotCount = cleaned.count { it == '.' }
            if (dotCount <= 1) onValueChange(cleaned)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal    // shows numeric keyboard with decimal
        ),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onBackground
        ),
        cursorBrush = SolidColor(AccentBlue),      // blue cursor to match the theme
        singleLine = true,
        modifier = modifier,
        decorationBox = { innerTextField ->
            // Show placeholder text when the field is empty
            if (value.isEmpty()) {
                Text(
                    text  = placeholder,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            innerTextField()   // always draw the actual text field on top
        }
    )
}

// ── YearsMonthsToggle ─────────────────────────────────────────────────────────

/**
 * Inline "Years | Months" switcher shown inside the Period field.
 *
 * @param isYears  true = "Years" is active, false = "Months" is active
 * @param onToggle called when the user taps either option
 */
@Composable
fun YearsMonthsToggle(
    isYears: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {

        // "Years" label — highlighted when isYears == true
        Text(
            text     = "Years",
            style    = MaterialTheme.typography.bodyMedium,
            color    = if (isYears) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onToggle(true) }
        )

        Text(
            text  = "  |  ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        // "Months" label — highlighted when isYears == false
        Text(
            text     = "Months",
            style    = MaterialTheme.typography.bodyMedium,
            color    = if (!isYears) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onToggle(false) }
        )
    }
}

// ── SummaryCard ───────────────────────────────────────────────────────────────

/**
 * The results card shown below the calculator rows once all inputs are valid.
 * Displays EMI, total interest, and total amount.
 */
@Composable
fun SummaryCard(
    emi: String,
    totalInterest: String,
    totalAmount: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryRow(label = "Monthly EMI",    value = emi,           valueColor = AccentBlue)
        SummaryDivider()
        SummaryRow(label = "Total Interest", value = totalInterest, valueColor = AccentAmber)
        SummaryRow(label = "Total Amount",   value = totalAmount,   valueColor = AccentGreen)
    }
}

/**
 * A single label + value row inside the SummaryCard.
 */
@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor
        )
    }
}

/** Thin horizontal line between summary rows. */
@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

// ── SharePdfButton ────────────────────────────────────────────────────────────

/**
 * Full-width button that generates an EMI PDF report and opens the system share sheet.
 * Uses AccentBlue border + text to signal it's the primary action.
 *
 * @param onClick Called when the user taps — the caller handles PDF creation + share Intent.
 */
@Composable
fun SharePdfButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.5.dp,
                color = AccentBlue,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = "Share as PDF",
            style = MaterialTheme.typography.labelLarge,
            color = AccentBlue
        )
    }
}

// ── ClearButton ───────────────────────────────────────────────────────────────

/**
 * Full-width outlined button that clears all calculator inputs.
 * Styled to match the app's rounded-card language without being alarming.
 *
 * @param onClick Called when the user taps the button.
 */
@Composable
fun ClearButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = "Clear",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
