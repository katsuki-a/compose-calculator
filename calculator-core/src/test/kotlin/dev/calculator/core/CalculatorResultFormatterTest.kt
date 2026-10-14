package dev.calculator.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorResultFormatterTest {
    private fun expect(value: String, expected: String) {
        assertEquals(expected, CalculatorResultFormatter.format(BigDecimal(value)))
    }
    private fun expectExpression(expression: String, expected: String) {
        val result = CalculatorEngine.evaluate(expression) as CalculationResult.Success
        assertEquals(expected, CalculatorResultFormatter.format(result.value))
    }

    @Test fun repeatingThird() = expectExpression("1/3", "≈ 0.333333333333")
    @Test fun repeatingTwoThirdsRoundsUp() = expectExpression("2/3", "≈ 0.666666666667")
    @Test fun negativeRepeatingValue() = expectExpression("-2/3", "≈ -0.666666666667")
    @Test fun terminatingFraction() = expectExpression("1/8", "0.125")
    @Test fun significantDigitsIncludeIntegerPart() = expectExpression("123456789/7", "≈ 17636684.1429")
    @Test fun halfEvenKeepsEvenLastDigit() = expect("1.234567890125", "≈ 1.23456789012")
    @Test fun halfEvenRoundsOddLastDigitUp() = expect("1.234567890135", "≈ 1.23456789014")
    @Test fun negativeHalfEven() = expect("-1.234567890125", "≈ -1.23456789012")
    @Test fun trailingZerosDoNotIndicateRounding() = expect("1.23000000000000", "1.23")
    @Test fun zero() = expect("0.00000000000000", "0")
    @Test fun negativeZero() = expect("-0.00000000000000", "0")
    @Test fun smallestPlainExponent() = expect("0.000001", "0.000001")
    @Test fun belowPlainRange() = expect("0.0000001", "1e-7")
    @Test fun largestPlainExponent() = expect("999999999999", "999999999999")
    @Test fun abovePlainRange() = expect("1000000000000", "1e+12")
    @Test fun smallValuesNeverRoundToZero() = expectExpression("1/30000000000000000000", "≈ 3.33333333333e-20")
    @Test fun negativeScientificNotation() = expect("-1000000000000", "-1e+12")
    @Test fun roundingCarryChangesNotation() = expect("999999999999.5", "≈ 1e+12")
    @Test fun roundingCarryEntersPlainRange() = expect("0.0000009999999999995", "≈ 0.000001")
    @Test fun largeExactValuesAlsoFollowDisplayLimit() = expect("12345678901234567890", "≈ 1.23456789012e+19")
}
