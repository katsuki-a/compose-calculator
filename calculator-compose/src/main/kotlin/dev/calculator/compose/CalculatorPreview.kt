package dev.calculator.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.calculator.core.CalculatorError
import dev.calculator.core.CalculatorState

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CalculatorLightPreview() {
    MaterialTheme {
        Calculator(state = CalculatorState("(12.5+7.5)*3", "60", true), onAction = {}, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CalculatorDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Calculator(state = CalculatorState("128/4"), onAction = {}, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 1.5f, locale = "en")
@Composable
private fun CalculatorErrorPreview() {
    MaterialTheme {
        Calculator(state = CalculatorState("1/0", error = CalculatorError.DIVISION_BY_ZERO), onAction = {}, modifier = Modifier.fillMaxWidth())
    }
}
