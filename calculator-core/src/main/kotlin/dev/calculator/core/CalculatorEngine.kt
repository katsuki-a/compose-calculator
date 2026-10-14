package dev.calculator.core

import java.math.BigDecimal
import java.math.MathContext

/** Failures are values, so consumers do not need exception-based control flow. */
public enum class CalculatorError { INVALID_EXPRESSION, DIVISION_BY_ZERO, LIMIT_EXCEEDED }

public sealed interface CalculationResult {
    public data class Success(val value: BigDecimal) : CalculationResult {
        /** Full engine precision in plain notation; use CalculatorResultFormatter for UI output. */
        public val formatted: String get() = value.stripTrailingZeros().toPlainString()
    }
    public data class Failure(val error: CalculatorError) : CalculationResult
}

/** Decimal arithmetic with standard precedence. Percent is always a postfix division by 100. */
public object CalculatorEngine {
    public const val MAX_EXPRESSION_LENGTH: Int = 512
    public const val MAX_NESTING_DEPTH: Int = 64
    public val mathContext: MathContext = MathContext.DECIMAL128

    public fun evaluate(expression: String): CalculationResult {
        if (expression.length > MAX_EXPRESSION_LENGTH) {
            return CalculationResult.Failure(CalculatorError.LIMIT_EXCEEDED)
        }
        return try {
            val normalized = expression.replace('×', '*').replace('÷', '/').replace('−', '-')
            CalculationResult.Success(Parser(normalized).parse())
        } catch (failure: ParseFailure) {
            CalculationResult.Failure(failure.error)
        }
    }

    private class ParseFailure(val error: CalculatorError) : RuntimeException()

    private class Parser(private val source: String) {
        private var position = 0
        private var depth = 0

        fun parse(): BigDecimal {
            val result = sum()
            skipWhitespace()
            if (position != source.length) fail()
            return result
        }

        private fun sum(): BigDecimal {
            var value = product()
            while (true) {
                value = when {
                    consume('+') -> value.add(product(), mathContext)
                    consume('-') -> value.subtract(product(), mathContext)
                    else -> return value
                }
            }
        }

        private fun product(): BigDecimal {
            var value = unary()
            while (true) {
                value = when {
                    consume('*') -> value.multiply(unary(), mathContext)
                    consume('/') -> {
                        val divisor = unary()
                        if (divisor.signum() == 0) fail(CalculatorError.DIVISION_BY_ZERO)
                        value.divide(divisor, mathContext)
                    }
                    else -> return value
                }
            }
        }

        private fun unary(): BigDecimal {
            depth++
            if (depth > MAX_NESTING_DEPTH) fail(CalculatorError.LIMIT_EXCEEDED)
            val value = when {
                consume('+') -> unary()
                consume('-') -> unary().negate(mathContext)
                else -> postfix()
            }
            depth--
            return value
        }

        private fun postfix(): BigDecimal {
            var value = if (consume('(')) {
                val inner = sum()
                if (!consume(')')) fail()
                inner
            } else {
                number()
            }
            while (consume('%')) value = value.divide(BigDecimal(100), mathContext)
            return value
        }

        private fun number(): BigDecimal {
            skipWhitespace()
            val start = position
            var digits = 0
            while (position < source.length && source[position] in '0'..'9') {
                position++
                digits++
            }
            if (position < source.length && source[position] == '.') {
                position++
                while (position < source.length && source[position] in '0'..'9') {
                    position++
                    digits++
                }
            }
            if (digits == 0) fail()
            return BigDecimal(source.substring(start, position), mathContext)
        }

        private fun consume(character: Char): Boolean {
            skipWhitespace()
            if (position >= source.length || source[position] != character) return false
            position++
            return true
        }

        private fun skipWhitespace() {
            while (position < source.length && source[position].isWhitespace()) position++
        }

        private fun fail(error: CalculatorError = CalculatorError.INVALID_EXPRESSION): Nothing {
            throw ParseFailure(error)
        }
    }
}
