# Calculator specification

This document defines calculation, display, interaction, state, and UI behavior. For installation and API examples, see the [README](../README.md).

## Calculation semantics

- Precedence: parentheses → postfix `%` → unary signs → `* /` → `+ -`
- Operators with equal precedence are evaluated left to right: `2+3*4=14`, `24/4/2=3`
- Uses `BigDecimal` with `MathContext.DECIMAL128`: 34 significant digits and HALF_EVEN rounding. `0.1+0.2=0.3`; `1/3` is rounded to 34 significant digits
- **`%` always divides its operand by 100.** `50%=0.5`, `200*10%=20`, and `200+10%=200.1`. For a percentage increase, use `200*(1+10%)=220`
- The engine also accepts `× ÷ −`. Implicit multiplication such as `2(3)` is rejected by the expression API; the keypad inserts `*` where needed
- Failures are returned as `INVALID_EXPRESSION`, `DIVISION_BY_ZERO`, or `LIMIT_EXCEEDED`
- Expressions are limited to 512 characters. Parentheses and unary signs also have a recursive depth limit to reject excessive input

## Result display precision

Results display **at most 12 significant digits**, including both the integer and fractional parts. Leading zeros do not count. This keeps non-terminating decimals readable without using a fixed number of decimal places that could erase very small values.

- Round with **HALF_EVEN** (ties to the nearest even final digit), matching the engine's rounding mode.
- Remove trailing fractional zeros and an unnecessary decimal point. Zero displays as `0`.
- Use plain notation for decimal exponents from -6 through 11 (absolute values from `0.000001` up to, but excluding, `1e+12`). Use scientific notation outside that range. Choose notation after rounding, including any carry to a new exponent.
- Prefix the result with **`≈`** when display rounding changes the engine's numeric value. The indicator describes display rounding; the engine itself still has finite, 34-digit precision.
- Apply this policy consistently to all results, including terminating decimals or integers exceeding 12 significant digits. Input expressions keep their entered digits and can scroll.

| Calculation | Display |
| --- | --- |
| `1/3` | `≈ 0.333333333333` |
| `2/3` | `≈ 0.666666666667` |
| `1/8` | `0.125` |
| `123456789/7` | `≈ 17636684.1429` |
| `1/10000000` | `1e-7` |
| `1000000000000` | `1e+12` |

**Display rounding never changes the stored calculation result.** `CalculatorState.result`, `CalculationResult.Success.value`, `Success.formatted`, and `onResult` retain the engine's precision. Continued operations and state restoration use the unrounded engine value. For example, `1/3`, `=`, `×3`, `=` retains `0.9999999999999999999999999999999999` and displays `≈ 1`, instead of multiplying the shortened display text.

Use `CalculatorResultFormatter.format(value)` to apply the same presentation policy outside Compose. `CalculatorState.display` already uses it. Display strings may contain `≈` or scientific notation and must not be passed to the expression engine. Backspace edits the full stored result rather than its shortened display.

See the [repeating-decimal result preview](previews/repeating.png).

## Key behavior

- `AC`: clear the input, result, and error
- `⌫`: delete the last character; when a result is displayed, edit the result's digits
- `±`: change the sign of the last number or parenthesized operand; after an operator, start a negative operand
- Consecutive operator presses replace the pending operator
- Duplicate decimal points and invalid closing parentheses are ignored
- `=`: evaluate the expression; incomplete expressions and parentheses produce an error instead of automatic completion
- After a result: a digit, decimal point, or opening parenthesis starts a new expression; an operator continues from the result
- After an error: a digit, decimal point, or opening parenthesis starts over; `⌫` edits the original expression
- Repeated `=` preserves the result

## State and callback contract

`Calculator()` uses `rememberCalculatorController()` internally. It saves the input, result, and error through `rememberSaveable` and a custom Saver, restoring them after activity recreation. Implement persistent calculation history in the host app if needed. `onResult` is called only after a successful `=` action, with no duplicate notification on repeated `=`, restoration, or recomposition.

Dispatch controller actions on the UI thread. `initialState` is read only when the controller is first created; restored state takes precedence.

The stateless overload emits `onAction`; the host handles calculation, persistence, and result notifications. A state held with `remember` alone lasts only within the Composition; use a host persistence mechanism such as `SavedStateHandle` when activity recreation must be supported.

## UI and localization

The calculator follows `MaterialTheme` colors and typography. Its maximum width is 420dp, and keys have a minimum height of 56dp. The UI scrolls vertically when space is limited, and long results scroll horizontally. Apply suitable window insets and a bounded height in the host. When nesting the calculator inside another vertical scrolling container, constrain its height with a modifier such as `Modifier.height(...)`.

All bundled labels and error messages are in English. Override `CalculatorLabels` to customize or localize them. Digits are ASCII, and the decimal separator is `.`. Physical keyboard input, scientific-notation entry, digit grouping, trigonometric functions, memory keys, and calculation history are not included.
