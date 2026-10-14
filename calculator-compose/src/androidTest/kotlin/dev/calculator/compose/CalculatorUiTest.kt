package dev.calculator.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.calculator.core.CalculatorAction
import dev.calculator.core.CalculatorError
import dev.calculator.core.CalculatorState
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CalculatorUiTest {
    @get:Rule val compose = createComposeRule()
    private fun tap(vararg keys: String) {
        keys.forEach { compose.onNodeWithTag("calculator_key_$it").performScrollTo().performClick() }
    }
    @Test fun arithmeticAndCallback() {
        val results = mutableListOf<BigDecimal>()
        compose.setContent { MaterialTheme { Calculator(modifier = Modifier.fillMaxWidth(), onResult = { results.add(it) }) } }
        tap("2", "+", "3", "×", "4", "=", "=")
        compose.onNodeWithTag("calculator_display").assertTextEquals("14")
        compose.runOnIdle { assertEquals(listOf(BigDecimal("14")), results) }
        tap("+", "1", "=")
        compose.onNodeWithTag("calculator_display").assertTextEquals("15")
    }
    @Test fun divisionByZeroAndRecovery() {
        compose.setContent { MaterialTheme { Calculator(modifier = Modifier.fillMaxWidth()) } }
        tap("1", "÷", "0", "=")
        compose.onNodeWithTag("calculator_error").assertIsDisplayed()
            .assertTextEquals("Cannot divide by zero")
        tap("7", "=")
        compose.onNodeWithTag("calculator_display").assertTextEquals("7")
    }
    @Test fun restoresInput() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { Calculator(modifier = Modifier.fillMaxWidth()) } }
        tap("1", "2", "+", "3")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("calculator_display").assertTextEquals("12+3")
        tap("=")
        compose.onNodeWithTag("calculator_display").assertTextEquals("15")
    }
    @Test fun restoresResultAndContinues() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { Calculator(modifier = Modifier.fillMaxWidth()) } }
        tap("8", "×", "2", "=")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("calculator_display").assertTextEquals("16")
        tap("+", "1", "=")
        compose.onNodeWithTag("calculator_display").assertTextEquals("17")
    }
    @Test fun restoresError() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { Calculator(modifier = Modifier.fillMaxWidth()) } }
        tap("1", "÷", "0", "=")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("calculator_error").assertIsDisplayed()
        tap("AC")
        compose.onNodeWithTag("calculator_display").assertTextEquals("0")
    }
    @Test fun statelessApiEmitsAction() {
        val actions = mutableListOf<CalculatorAction>()
        compose.setContent {
            MaterialTheme {
                Calculator(state = CalculatorState("1/0", error = CalculatorError.DIVISION_BY_ZERO),
                    onAction = { actions.add(it) }, modifier = Modifier.fillMaxWidth())
            }
        }
        tap("AC")
        compose.runOnIdle { assertEquals(listOf(CalculatorAction.Clear), actions) }
    }
    @Test fun roundedDisplayRestoresFullPrecisionAndCallback() {
        val results = mutableListOf<BigDecimal>()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme { Calculator(modifier = Modifier.fillMaxWidth(), onResult = { results.add(it) }) }
        }
        tap("1", "÷", "3", "=")
        compose.onNodeWithTag("calculator_display").assertTextEquals("≈ 0.333333333333")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("calculator_display").assertTextEquals("≈ 0.333333333333")
        compose.runOnIdle {
            assertEquals(listOf(BigDecimal("0.3333333333333333333333333333333333")), results)
        }
        tap("×", "3", "=")
        compose.onNodeWithTag("calculator_display").assertTextEquals("≈ 1")
        compose.runOnIdle {
            assertEquals(BigDecimal("0.9999999999999999999999999999999999"), results.last())
            assertEquals(2, results.size)
        }
    }

}
