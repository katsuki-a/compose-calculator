package dev.calculator.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {
    private fun expect(expression: String, expected: String) {
        val actual = CalculatorEngine.evaluate(expression)
        assertTrue("$expression: $actual", actual is CalculationResult.Success)
        assertEquals(expected, (actual as CalculationResult.Success).formatted)
    }
    private fun fail(expression: String, error: CalculatorError = CalculatorError.INVALID_EXPRESSION) {
        assertEquals(CalculationResult.Failure(error), CalculatorEngine.evaluate(expression))
    }
    @Test fun precedence() = expect("2+3*4", "14")
    @Test fun parentheses() = expect("(2+3)*4", "20")
    @Test fun leftAssociativeDivision() = expect("24/4/2", "3")
    @Test fun leftAssociativeSubtraction() = expect("10-3-2", "5")
    @Test fun exactDecimalSum() = expect("0.1+0.2", "0.3")
    @Test fun exactDecimalProduct() = expect("1.2*0.3", "0.36")
    @Test fun repeatingDecimal() = expect("1/3", "0.3333333333333333333333333333333333")
    @Test fun signedOperand() = expect("2*-3", "-6")
    @Test fun signedGroup() = expect("-(2+3)*4", "-20")
    @Test fun unaryPlus() = expect("+2--3", "5")
    @Test fun percent() = expect("50%", "0.5")
    @Test fun percentIsRatio() = expect("200+10%", "200.1")
    @Test fun percentOfGroup() = expect("(20+30)%*200", "100")
    @Test fun unicodeSymbols() = expect("6×3÷2−1", "8")
    @Test fun whitespace() = expect(" 2 + ( 3 * 4 ) ", "14")
    @Test fun leadingAndTrailingDecimal() = expect(".5+2.", "2.5")
    @Test fun negativeZeroNormalized() = expect("-0.0", "0")
    @Test fun divisionByZero() = fail("1/(2-2)", CalculatorError.DIVISION_BY_ZERO)
    @Test fun malformedExpressions() {
        listOf("", ".", "1+", "()", "(1+2", "1+2)", "1..2", "2(3)", "abc", "1e3", "1 2", "NaN", "%1").forEach { fail(it) }
    }
    @Test fun tooLong() = fail("1".repeat(513), CalculatorError.LIMIT_EXCEEDED)
    @Test fun excessiveNesting() = fail("(".repeat(65) + "1" + ")".repeat(65), CalculatorError.LIMIT_EXCEEDED)
    @Test fun excessiveUnarySigns() = fail("-".repeat(65) + "1", CalculatorError.LIMIT_EXCEEDED)
    @Test fun maximumLiteralLengthDoesNotCrash() {
        assertTrue(CalculatorEngine.evaluate("9".repeat(512)) is CalculationResult.Success)
    }
}
