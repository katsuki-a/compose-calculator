package dev.calculator.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.calculator.core.CalculatorAction
import dev.calculator.core.CalculatorError
import dev.calculator.core.CalculatorReducer
import dev.calculator.core.CalculatorState

/** Call dispatch on the UI thread, just like other Compose state holders. */
@Stable
public class CalculatorController(initialState: CalculatorState = CalculatorState()) {
    public var state: CalculatorState by mutableStateOf(initialState)
        private set

    public fun dispatch(action: CalculatorAction) {
        state = CalculatorReducer.reduce(state, action)
    }

    public companion object {
        public val Saver = listSaver<CalculatorController, Any>(
            save = { controller ->
                with(controller.state) {
                    listOf(expression, result.orEmpty(), isResult, error?.name.orEmpty())
                }
            },
            restore = { saved ->
                CalculatorController(
                    CalculatorState(
                        expression = saved[0] as String,
                        result = (saved[1] as String).ifEmpty { null },
                        isResult = saved[2] as Boolean,
                        error = (saved[3] as String).ifEmpty { null }?.let(CalculatorError::valueOf),
                    ),
                )
            },
        )
    }
}

/** initialState is read only on first creation; restored state takes precedence. */
@Composable
public fun rememberCalculatorController(
    initialState: CalculatorState = CalculatorState(),
): CalculatorController = rememberSaveable(saver = CalculatorController.Saver) {
    CalculatorController(initialState)
}
