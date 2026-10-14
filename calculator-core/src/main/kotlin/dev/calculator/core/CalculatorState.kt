package dev.calculator.core

public enum class CalculatorOperator(public val symbol: Char) {
    ADD('+'), SUBTRACT('-'), MULTIPLY('*'), DIVIDE('/')
}

public sealed interface CalculatorAction {
    public data class Digit(val value: Int) : CalculatorAction {
        init { require(value in 0..9) { "Digit must be between 0 and 9" } }
    }
    public data class Operator(val value: CalculatorOperator) : CalculatorAction
    public data object DecimalPoint : CalculatorAction
    public data object OpenParenthesis : CalculatorAction
    public data object CloseParenthesis : CalculatorAction
    public data object Percent : CalculatorAction
    public data object ToggleSign : CalculatorAction
    public data object Backspace : CalculatorAction
    public data object Clear : CalculatorAction
    public data object Equals : CalculatorAction
}

/** Immutable state suitable for a ViewModel, StateFlow, or the provided Compose controller. */
public data class CalculatorState(
    val expression: String = "",
    /** Canonical engine value, retained at calculation precision for callbacks and continuation. */
    val result: String? = null,
    val isResult: Boolean = false,
    val error: CalculatorError? = null,
) {
    public val display: String
        get() = if (isResult) {
            result?.toBigDecimalOrNull()?.let(CalculatorResultFormatter::format) ?: result ?: "0"
        } else expression.ifEmpty { "0" }
}

/** Pure state transition; it never mutates the supplied state. */
public object CalculatorReducer {
    public fun reduce(state: CalculatorState, action: CalculatorAction): CalculatorState {
        if (action == CalculatorAction.Clear) return CalculatorState()
        if (action == CalculatorAction.Equals) {
            if (state.isResult) return state
            return when (val calculation = CalculatorEngine.evaluate(state.expression.ifEmpty { "0" })) {
                is CalculationResult.Success -> state.copy(
                    result = calculation.formatted, isResult = true, error = null,
                )
                is CalculationResult.Failure -> state.copy(error = calculation.error, isResult = false)
            }
        }
        val startsFresh = action is CalculatorAction.Digit ||
            action == CalculatorAction.DecimalPoint || action == CalculatorAction.OpenParenthesis
        val source = when {
            startsFresh && (state.isResult || state.error != null) -> ""
            state.isResult -> state.result.orEmpty()
            else -> state.expression
        }
        val next = when (action) {
            is CalculatorAction.Digit -> {
                val prefix = if (source.lastOrNull() in listOf(')', '%')) "$source*" else source
                prefix + action.value
            }
            is CalculatorAction.Operator -> operator(source, action.value.symbol)
            CalculatorAction.DecimalPoint -> decimal(source)
            CalculatorAction.OpenParenthesis -> {
                if (complete(source)) "$source*(" else "$source("
            }
            CalculatorAction.CloseParenthesis -> {
                if (complete(source) && source.count { it == '(' } > source.count { it == ')' }) {
                    "$source)"
                } else source
            }
            CalculatorAction.Percent -> if (complete(source) && !source.endsWith('%')) "$source%" else source
            CalculatorAction.ToggleSign -> toggleSign(source)
            CalculatorAction.Backspace -> source.dropLast(1)
        }
        if (next.length > CalculatorEngine.MAX_EXPRESSION_LENGTH) {
            return state.copy(error = CalculatorError.LIMIT_EXCEEDED)
        }
        return CalculatorState(expression = next)
    }

    private fun complete(source: String): Boolean =
        source.lastOrNull()?.let { it in '0'..'9' || it == '.' || it == ')' || it == '%' } ?: false

    private fun operator(source: String, symbol: Char): String = when {
        source.isEmpty() || source.endsWith('(') -> if (symbol == '-') "$source-" else source
        source.last() in "+-*/" -> {
            val base = source.dropLastWhile { it in "+-*/" }
            if (base.isEmpty() || base.endsWith('(')) {
                if (symbol == '-') "$base-" else source
            } else base + symbol
        }
        else -> source + symbol
    }

    private fun decimal(source: String): String {
        if (source.lastOrNull() in listOf(')', '%')) return "$source*0."
        val number = source.takeLastWhile { it in '0'..'9' || it == '.' }
        return when {
            '.' in number -> source
            number.isEmpty() -> source + "0."
            else -> "$source."
        }
    }

    private fun toggleSign(source: String): String {
        if (!complete(source)) {
            return if (source.endsWith('-') &&
                (source.length == 1 || source[source.length - 2] in "(+-*/")) {
                source.dropLast(1)
            } else "$source-"
        }
        var end = source.length
        while (end > 0 && source[end - 1] == '%') end--
        var start = end
        if (end > 0 && source[end - 1] == ')') {
            var balance = 0
            do {
                start--
                if (source[start] == ')') balance++
                if (source[start] == '(') balance--
            } while (start > 0 && balance > 0)
        } else {
            while (start > 0 && (source[start - 1] in '0'..'9' || source[start - 1] == '.')) start--
        }
        if (start > 0 && source[start - 1] == '-' &&
            (start == 1 || source[start - 2] in "(+-*/")) start--
        val operand = source.substring(start)
        val replacement = when {
            operand.startsWith("-(") && operand.endsWith(')') -> operand.substring(2, operand.length - 1)
            operand.startsWith('-') -> operand.drop(1)
            else -> if (operand.startsWith('(')) "-($operand)" else "-$operand"
        }
        return source.take(start) + replacement
    }
}
