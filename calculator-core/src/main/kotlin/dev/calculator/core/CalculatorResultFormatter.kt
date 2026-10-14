package dev.calculator.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Presentation only: never feed this string back into the calculation engine. */
public object CalculatorResultFormatter {
    public const val MAX_SIGNIFICANT_DIGITS: Int = 12
    private val displayContext = MathContext(MAX_SIGNIFICANT_DIGITS, RoundingMode.HALF_EVEN)

    /**
     * Uses plain notation for decimal exponents -6 through 11, scientific notation otherwise.
     * Prefixes the result with ≈ when display rounding changes the engine's numeric value.
     */
    public fun format(value: BigDecimal): String {
        val rounded = value.round(displayContext).stripTrailingZeros()
        if (rounded.signum() == 0) return "0"
        val exponent = rounded.precision() - rounded.scale() - 1
        val number = if (exponent in -6 until MAX_SIGNIFICANT_DIGITS) {
            rounded.toPlainString()
        } else {
            val mantissa = BigDecimal(rounded.unscaledValue(), rounded.precision() - 1)
                .stripTrailingZeros().toPlainString()
            val signedExponent = if (exponent >= 0) "+$exponent" else exponent.toString()
            "${mantissa}e$signedExponent"
        }
        return if (rounded.compareTo(value) == 0) number else "≈ $number"
    }
}
