package dev.calculator.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.calculator.core.CalculatorAction
import dev.calculator.core.CalculatorError
import dev.calculator.core.CalculatorOperator
import dev.calculator.core.CalculatorState
import java.math.BigDecimal

@Immutable
public data class CalculatorColors(
    val background: Color,
    val display: Color,
    val numberContainer: Color,
    val numberContent: Color,
    val operatorContainer: Color,
    val operatorContent: Color,
    val equalsContainer: Color,
    val equalsContent: Color,
    val error: Color,
)

@Immutable
public data class CalculatorLabels(
    val clear: String,
    val backspace: String,
    val openParenthesis: String,
    val closeParenthesis: String,
    val toggleSign: String,
    val decimalPoint: String,
    val percent: String,
    val add: String,
    val subtract: String,
    val multiply: String,
    val divide: String,
    val equals: String,
    val invalidExpression: String,
    val divisionByZero: String,
    val limitExceeded: String,
)

public object CalculatorDefaults {
    @Composable
    public fun colors(): CalculatorColors = with(MaterialTheme.colorScheme) {
        CalculatorColors(
            surface, onSurface, surfaceContainerHighest, onSurface,
            secondaryContainer, onSecondaryContainer, primary, onPrimary, error,
        )
    }

    @Composable
    public fun labels(): CalculatorLabels = CalculatorLabels(
        stringResource(R.string.calculator_clear),
        stringResource(R.string.calculator_backspace),
        stringResource(R.string.calculator_open_parenthesis),
        stringResource(R.string.calculator_close_parenthesis),
        stringResource(R.string.calculator_toggle_sign),
        stringResource(R.string.calculator_decimal_point),
        stringResource(R.string.calculator_percent),
        stringResource(R.string.calculator_add),
        stringResource(R.string.calculator_subtract),
        stringResource(R.string.calculator_multiply),
        stringResource(R.string.calculator_divide),
        stringResource(R.string.calculator_equals),
        stringResource(R.string.calculator_invalid_expression),
        stringResource(R.string.calculator_division_by_zero),
        stringResource(R.string.calculator_limit_exceeded),
    )
}

/** Ready-to-use calculator. onResult is called once for each successful Equals action. */
@Composable
public fun Calculator(
    modifier: Modifier = Modifier,
    controller: CalculatorController = rememberCalculatorController(),
    colors: CalculatorColors = CalculatorDefaults.colors(),
    labels: CalculatorLabels = CalculatorDefaults.labels(),
    onResult: (BigDecimal) -> Unit = {},
) {
    Calculator(
        state = controller.state,
        onAction = { action ->
            val wasResult = controller.state.isResult
            controller.dispatch(action)
            val next = controller.state
            if (action == CalculatorAction.Equals && !wasResult && next.isResult && next.error == null) {
                next.result?.let { onResult(BigDecimal(it)) }
            }
        },
        modifier = modifier,
        colors = colors,
        labels = labels,
    )
}

/** Stateless UI for host-owned state, for example a ViewModel using CalculatorReducer. */
@Composable
public fun Calculator(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    colors: CalculatorColors = CalculatorDefaults.colors(),
    labels: CalculatorLabels = CalculatorDefaults.labels(),
) {
    val displayScroll = rememberScrollState()
    LaunchedEffect(state.display) { displayScroll.scrollTo(displayScroll.maxValue) }
    Surface(
        modifier = modifier.widthIn(max = 420.dp),
        shape = RoundedCornerShape(28.dp),
        color = colors.background,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp).padding(8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                if (state.isResult) {
                    Text(
                        text = state.expression.displaySymbols() + " =",
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        color = colors.display.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }
                Text(
                    text = state.display.displaySymbols(),
                    modifier = Modifier.horizontalScroll(displayScroll)
                        .testTag("calculator_display")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    color = colors.display,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                state.error?.let { error ->
                    Text(
                        text = when (error) {
                            CalculatorError.INVALID_EXPRESSION -> labels.invalidExpression
                            CalculatorError.DIVISION_BY_ZERO -> labels.divisionByZero
                            CalculatorError.LIMIT_EXCEEDED -> labels.limitExceeded
                        },
                        modifier = Modifier.testTag("calculator_error")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        color = colors.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            keyRows(labels).forEach { keys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    keys.forEach { key ->
                        val container = when (key.kind) {
                            KeyKind.NUMBER -> colors.numberContainer
                            KeyKind.FUNCTION -> colors.operatorContainer
                            KeyKind.EQUALS -> colors.equalsContainer
                        }
                        val content = when (key.kind) {
                            KeyKind.NUMBER -> colors.numberContent
                            KeyKind.FUNCTION -> colors.operatorContent
                            KeyKind.EQUALS -> colors.equalsContent
                        }
                        Button(
                            onClick = { onAction(key.action) },
                            modifier = Modifier.weight(key.weight).heightIn(min = 56.dp)
                                .testTag("calculator_key_${key.text}")
                                .semantics { contentDescription = key.description },
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
                        ) {
                            Text(key.text, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private enum class KeyKind { NUMBER, FUNCTION, EQUALS }
private data class Key(
    val text: String,
    val action: CalculatorAction,
    val description: String = text,
    val kind: KeyKind = KeyKind.FUNCTION,
    val weight: Float = 1f,
)

private fun keyRows(labels: CalculatorLabels): List<List<Key>> {
    fun digit(value: Int) = Key(value.toString(), CalculatorAction.Digit(value), kind = KeyKind.NUMBER)
    fun op(text: String, operator: CalculatorOperator, description: String) =
        Key(text, CalculatorAction.Operator(operator), description)
    return listOf(
        listOf(
            Key("AC", CalculatorAction.Clear, labels.clear),
            Key("(", CalculatorAction.OpenParenthesis, labels.openParenthesis),
            Key(")", CalculatorAction.CloseParenthesis, labels.closeParenthesis),
            Key("⌫", CalculatorAction.Backspace, labels.backspace),
        ),
        listOf(digit(7), digit(8), digit(9), op("÷", CalculatorOperator.DIVIDE, labels.divide)),
        listOf(digit(4), digit(5), digit(6), op("×", CalculatorOperator.MULTIPLY, labels.multiply)),
        listOf(digit(1), digit(2), digit(3), op("−", CalculatorOperator.SUBTRACT, labels.subtract)),
        listOf(
            Key("±", CalculatorAction.ToggleSign, labels.toggleSign), digit(0),
            Key(".", CalculatorAction.DecimalPoint, labels.decimalPoint, KeyKind.NUMBER),
            op("+", CalculatorOperator.ADD, labels.add),
        ),
        listOf(
            Key("%", CalculatorAction.Percent, labels.percent),
            Key("=", CalculatorAction.Equals, labels.equals, KeyKind.EQUALS, weight = 3f),
        ),
    )
}

private fun String.displaySymbols(): String = replace('*', '×').replace('/', '÷').replace('-', '−')
