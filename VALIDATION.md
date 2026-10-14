# Completed verification

Verification date: October 15, 2026 (Japan Standard Time). Environment: macOS / arm64, Android Studio Rabbit 1, Gradle 9.6.0, and Android Studio's bundled JBR to run Gradle. The Kotlin/JVM toolchain is 17.

| Check | Result |
| --- | --- |
| `:calculator-core:test` | 49 passed, 0 failed or skipped: 23 engine tests and 26 reducer tests |
| `:calculator-compose:connectedDebugAndroidTest` | 6 passed, 0 failed or skipped on the Resizable Android 16 / API 36 / arm64 emulator |
| `:calculator-compose:assembleRelease` | AAR generated successfully |
| `:sample:assembleDebug` | APK generated, installed, and launched on the emulator; its screen was inspected |
| `:calculator-compose:lintRelease` | 0 warnings and 0 errors |
| `:sample:lintDebug` | 0 warnings and 0 errors |
| `publishAllPublicationsToDistributionRepository` | AAR, JAR, sources, POM, and Gradle module metadata generated |
| Independent Maven consumer | APK built from `dev.calculator:calculator-compose:0.1.0` without referencing the source modules |

## Test coverage

Engine tests cover precedence, parentheses, left associativity, decimal arithmetic, repeating-decimal rounding, signs, percentage behavior, invalid expressions, division by zero, and expression length and depth limits. Reducer tests cover continuation from results, new input, entering digits after a sign change, deletion, and error recovery.

UI tests cover calculation by tapping keys, result callbacks, duplicate-notification prevention for repeated `=`, division by zero and recovery, restoration of input, results and errors, and stateless action callbacks. State restoration is tested with Compose's `StateRestorationTester`; actual OS-initiated process termination was not exercised.

## Visual verification

Each `@Preview` was rendered individually in Android Studio. Fresh PNG files were generated without render errors and inspected for clipped text or keys.

- [Light theme / 360dp / calculated result](docs/previews/light.png)
- [Dark theme / 360dp / expression entry](docs/previews/dark.png)
- [English / 320dp / fontScale 1.5 / division-by-zero error](docs/previews/error.png)
- [Running sample APK](docs/previews/sample.png)

The error preview was regenerated after removing the Japanese resources. The division-by-zero UI test also checks the English error text.

Previews verify static appearance; UI tests verify interaction and restoration. Execution on API 23 or API 37, physical devices, TalkBack speech output, and host apps using different Compose versions are outside the completed verification scope.

## Distribution

The source ZIP includes source files, the Gradle Wrapper, documentation, and the distribution Maven repository. Build intermediates, IDE settings, and local SDK configuration are excluded. The sample APK uses a debug signature for evaluation. Source publication on GitHub is separate from artifact publication; the library has not been uploaded to Maven Central or another public Maven registry.
