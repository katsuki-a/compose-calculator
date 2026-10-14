package dev.calculator.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorReducerTest {
    private fun sequence(vararg actions: CalculatorAction): CalculatorState =
        actions.fold(CalculatorState(), CalculatorReducer::reduce)
    private fun digit(value: Int) = CalculatorAction.Digit(value)
    private fun op(value: CalculatorOperator) = CalculatorAction.Operator(value)
    private fun press(state: CalculatorState, action: CalculatorAction) = CalculatorReducer.reduce(state, action)

    @Test fun calculateFromKeys() {
        val state = sequence(digit(2), op(CalculatorOperator.ADD), digit(3), op(CalculatorOperator.MULTIPLY), digit(4), CalculatorAction.Equals)
        assertEquals("14", state.display)
        assertEquals("2+3*4", state.expression)
    }
    @Test fun numberStartsFreshAfterResult() {
        val state = press(CalculatorState("2+3", "5", true), digit(7))
        assertEquals("7", state.display)
        assertFalse(state.isResult)
    }
    @Test fun operatorContinuesFromResult() {
        val state = press(CalculatorState("2+3", "5", true), op(CalculatorOperator.MULTIPLY))
        assertEquals("5*", state.expression)
    }
    @Test fun replaceTrailingOperator() {
        assertEquals("2*", sequence(digit(2), op(CalculatorOperator.ADD), op(CalculatorOperator.MULTIPLY)).expression)
    }
    @Test fun replaceOperatorAndPendingSign() {
        assertEquals("2+", press(CalculatorState("2*-"), op(CalculatorOperator.ADD)).expression)
    }
    @Test fun leadingMinus() {
        assertEquals("-2", sequence(op(CalculatorOperator.SUBTRACT), digit(2)).expression)
    }
    @Test fun ignoreInvalidLeadingOperator() {
        assertEquals("", sequence(op(CalculatorOperator.MULTIPLY)).expression)
    }
    @Test fun decimalStartsAtZero() {
        assertEquals("0.5", sequence(CalculatorAction.DecimalPoint, digit(5)).expression)
    }
    @Test fun duplicateDecimalIgnored() {
        assertEquals("1.2", sequence(digit(1), CalculatorAction.DecimalPoint, digit(2), CalculatorAction.DecimalPoint).expression)
    }
    @Test fun signOnOperandAndFurtherDigits() {
        val state = sequence(digit(2), op(CalculatorOperator.ADD), digit(3), CalculatorAction.ToggleSign, digit(4), CalculatorAction.Equals)
        assertEquals("-32", state.display)
    }
    @Test fun toggleSignTwice() {
        assertEquals("2+3", press(press(CalculatorState("2+3"), CalculatorAction.ToggleSign), CalculatorAction.ToggleSign).expression)
    }
    @Test fun signOnGroup() {
        val state = press(CalculatorState("2*(3+4)"), CalculatorAction.ToggleSign)
        assertEquals("2*-((3+4))", state.expression)
        assertEquals("-14", press(state, CalculatorAction.Equals).display)
        assertEquals("2*(3+4)", press(state, CalculatorAction.ToggleSign).expression)
    }
    @Test fun signAfterOperator() {
        assertEquals("-6", sequence(digit(2), op(CalculatorOperator.MULTIPLY), CalculatorAction.ToggleSign, digit(3), CalculatorAction.Equals).display)
    }
    @Test fun percentThenContinue() {
        assertEquals("100", sequence(digit(5), digit(0), CalculatorAction.Percent, op(CalculatorOperator.MULTIPLY), digit(2), digit(0), digit(0), CalculatorAction.Equals).display)
    }
    @Test fun repeatedPercentKeyIgnored() {
        assertEquals("5%", sequence(digit(5), CalculatorAction.Percent, CalculatorAction.Percent).expression)
    }
    @Test fun implicitMultiplicationInKeypad() {
        val state = sequence(digit(2), CalculatorAction.OpenParenthesis, digit(3), CalculatorAction.CloseParenthesis, digit(4), CalculatorAction.Equals)
        assertEquals("24", state.display)
    }
    @Test fun unexpectedClosingParenthesisIgnored() {
        assertEquals("2", sequence(digit(2), CalculatorAction.CloseParenthesis).expression)
    }
    @Test fun closingIncompleteGroupIgnored() {
        assertEquals("(", sequence(CalculatorAction.OpenParenthesis, CalculatorAction.CloseParenthesis).expression)
    }
    @Test fun backspaceEditsResult() {
        assertEquals("1", press(CalculatorState("6*2", "12", true), CalculatorAction.Backspace).expression)
    }
    @Test fun failedCalculationKeepsInput() {
        val state = press(CalculatorState("1/0"), CalculatorAction.Equals)
        assertEquals(CalculatorError.DIVISION_BY_ZERO, state.error)
        assertEquals("1/0", state.expression)
    }
    @Test fun newDigitRecoversAfterError() {
        val state = press(CalculatorState("1/0", error = CalculatorError.DIVISION_BY_ZERO), digit(7))
        assertEquals("7", state.expression)
        assertNull(state.error)
    }
    @Test fun backspaceRecoversByEditingError() {
        val state = press(CalculatorState("1/0", error = CalculatorError.DIVISION_BY_ZERO), CalculatorAction.Backspace)
        assertEquals("1/", state.expression)
        assertNull(state.error)
    }
    @Test fun clearResetsAllState() {
        assertEquals(CalculatorState(), press(CalculatorState("1/0", error = CalculatorError.DIVISION_BY_ZERO), CalculatorAction.Clear))
    }
    @Test fun repeatedEqualsIsIdempotent() {
        val state = CalculatorState("2+3", "5", true)
        assertEquals(state, press(state, CalculatorAction.Equals))
    }
    @Test fun inputLengthLimit() {
        val state = press(CalculatorState("1".repeat(512)), digit(2))
        assertEquals(512, state.expression.length)
        assertEquals(CalculatorError.LIMIT_EXCEEDED, state.error)
    }
    @Test(expected = IllegalArgumentException::class) fun invalidDigitRejected() { digit(10) }
    @Test fun resultDisplayDoesNotReplaceCalculationValue() {
        val state = press(CalculatorState("1/3"), CalculatorAction.Equals)
        assertEquals("≈ 0.333333333333", state.display)
        assertEquals("0.3333333333333333333333333333333333", state.result)
    }
    @Test fun continuedCalculationUsesUnroundedResult() {
        val result = press(CalculatorState("1/3"), CalculatorAction.Equals)
        val continued = press(result, op(CalculatorOperator.MULTIPLY))
        val completed = press(press(continued, digit(3)), CalculatorAction.Equals)
        assertEquals("0.3333333333333333333333333333333333*", continued.expression)
        assertEquals("0.9999999999999999999999999999999999", completed.result)
        assertEquals("≈ 1", completed.display)
    }
    @Test fun scientificDisplayContinuesUsingPlainEngineValue() {
        val result = press(CalculatorState("1/10000000"), CalculatorAction.Equals)
        assertEquals("1e-7", result.display)
        assertEquals("0.0000001*", press(result, op(CalculatorOperator.MULTIPLY)).expression)
    }
    @Test fun backspaceEditsFullPrecisionValue() {
        val result = press(CalculatorState("1/3"), CalculatorAction.Equals)
        assertEquals(result.result!!.dropLast(1), press(result, CalculatorAction.Backspace).expression)
    }

}
