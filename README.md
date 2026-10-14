# Compose Calculator

[GitHub: katsuki-a/compose-calculator](https://github.com/katsuki-a/compose-calculator)

Android / Jetpack Composeに組み込める電卓ライブラリです。計算エンジン、状態遷移、UIを分離しています。

- `calculator-core`: Android非依存のKotlin/JVM計算エンジンと純粋なReducer
- `calculator-compose`: Material 3電卓UI、保存・復元可能なController、日本語・英語の読み上げラベル
- `sample`: ライブラリを利用するAndroidアプリ

## すぐに使う

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dev.calculator.compose.Calculator

@Composable
fun CalculatorScreen() {
    MaterialTheme {
        Calculator(onResult = { value ->
            // value: java.math.BigDecimal
        })
    }
}
```

`Calculator()`は内部で`rememberCalculatorController()`を使用します。入力式・結果・エラーを`rememberSaveable`とSaverで保存し、Activityの再生成などで復元します。永続的な計算履歴の保存はホストアプリで実装してください。成功した`=`操作でだけ`onResult`が呼ばれます。連続した`=`、復元、再コンポーズでは再通知しません。

## 導入

このライブラリは公開Mavenリポジトリにはアップロードしていません。ローカル配布用のMavenリポジトリを`distribution/maven/`に同梱しています。依存するComposeなどのライブラリはGoogle Maven / Maven Centralから取得します。

### ローカルMavenリポジトリを利用する

ホストプロジェクトの`settings.gradle.kts`のrepositoriesに、展開した配布物へのパスを追加してください。

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("/path/to/compose-calculator/distribution/maven") }
    }
}
```

アプリモジュールに追加します。

```kotlin
dependencies {
    implementation("dev.calculator:calculator-compose:0.1.0")
}
```

`calculator-core`は推移的に取得されます。UIが不要なら`dev.calculator:calculator-core:0.1.0`だけを利用できます。AARのみの直接配置では依存関係が自動解決されないため、Mavenリポジトリまたはソースモジュールの導入を推奨します。

### ソースモジュールを利用する

`calculator-core/`と`calculator-compose/`を、同梱のルートGradle設定とversion catalogも含めて利用します。同梱の`sample`は`implementation(project(":calculator-compose"))`で参照しています。既存プロジェクトへコピーする場合は、`libs`の依存定義・プラグイン定義もホストのversion catalogに統合してください。

### ビルド環境

- Android minSdk 23、プロジェクトのcompileSdk 37、サンプルのtargetSdk 37
- Gradle 9.6.0 / Android Gradle Plugin 9.4.1 / Kotlin・Compose Compiler Plugin 2.3.20
- Compose BOM 2026.03.01 / Material 3
- Gradle実行JDK 17以上、Kotlin/JVM toolchain 17
- Android SDKの場所はAndroid Studioで設定するか、`local.properties`に`sdk.dir=...`を指定

ホストアプリはComposeを有効にし、Kotlinと同じバージョンのCompose Compiler Pluginを適用してください。ここで使用するバージョンは動作検証のため固定しています。Compose依存のバージョンを変更したホストとの組み合わせは別途検証してください。

## 状態を操作する

```kotlin
val controller = rememberCalculatorController()
Calculator(controller = controller)
Button(onClick = { controller.dispatch(CalculatorAction.Clear) }) {
    Text("クリア")
}
val currentState = controller.state
```

Controllerの操作はUIスレッドで行ってください。`initialState`は初回作成時のみ参照し、保存状態がある場合は復元が優先されます。

状態をViewModelなどで管理する場合は、ステートレス版を使用できます。

```kotlin
@Composable
fun HostedCalculator() {
    // サンプルのrememberはComposition内だけの保持です。
    // Activity再生成をまたぐ保存はホスト側のSavedStateHandleなどで実装します。
    var state by remember { mutableStateOf(CalculatorState()) }
    Calculator(
        state = state,
        onAction = { action -> state = CalculatorReducer.reduce(state, action) },
    )
}
```

上のコードでは`androidx.compose.runtime.*`、`dev.calculator.core.*`、`dev.calculator.compose.Calculator`をimportします。ステートレス版は`onAction`だけを発行し、計算・保存・結果通知はホストが担当します。

## UIのカスタマイズ

```kotlin
Calculator(
    modifier = Modifier.fillMaxWidth(),
    colors = CalculatorDefaults.colors().copy(
        equalsContainer = Color(0xFF006C4C),
        equalsContent = Color.White,
    ),
    labels = CalculatorDefaults.labels().copy(clear = "入力をすべて消去"),
)
```

`MaterialTheme`の色・タイポグラフィに従います。最大幅は420dp、ボタンの最小高さは56dpです。高さが足りない場合は縦にスクロールでき、長い結果は横にスクロールできます。ホストで適切な画面Insetsと有限の高さを指定してください。別の縦スクロール領域内に置く場合は、`Modifier.height(...)`などで高さを制約してください。

英語がデフォルト、日本語リソースも同梱しています。ボタンの読み上げラベルとエラーメッセージは`CalculatorLabels`で置き換えられます。数字はASCII、小数点は`.`です。物理キーボード入力・指数表記・桁区切り・三角関数・メモリーキー・計算履歴は含みません。

## 計算エンジンのみ使う

```kotlin
when (val result = CalculatorEngine.evaluate("(12.5+7.5)*3")) {
    is CalculationResult.Success -> println(result.formatted) // "60"
    is CalculationResult.Failure -> println(result.error)
}
```

- 演算順序: 括弧 → 後置`%` → 単項符号 → `* /` → `+ -`
- 同じ優先順位は左から計算。`2+3*4=14`、`24/4/2=3`
- `BigDecimal`と`MathContext.DECIMAL128`を使用。有効数字34桁、HALF_EVEN丸め。`0.1+0.2=0.3`、`1/3`は34桁の近似値
- **`%`は常に100で割る演算**。`50%=0.5`、`200*10%=20`、`200+10%=200.1`。割合加算なら`200*(1+10%)=220`
- APIは`× ÷ −`も受け付けます。`2(3)`のような暗黙の乗算は式APIではエラー、UI入力では必要な`*`を挿入します
- エラーは`INVALID_EXPRESSION`、`DIVISION_BY_ZERO`、`LIMIT_EXCEEDED`で返します
- 入力は最大512文字。括弧・単項符号の再帰深さも制限し、過大な入力を拒否します

## キー操作

- `AC`: 入力・結果・エラーをクリア
- `⌫`: 最後の文字を削除。結果表示中は結果の数字から編集
- `±`: 最後の数値または括弧付きの項の符号を反転。演算子の後なら負の項の入力を開始
- 演算子を続けて押すと最後の演算子を置き換え
- 小数点の重複・不正な閉じ括弧は無視
- `=`: 不完全な括弧・式を自動補完せずエラーを表示
- 結果の後: 数字・小数点・開き括弧で新しい式、演算子で結果から継続
- エラーの後: 数字・小数点・開き括弧で入力をやり直し、`⌫`で元の式を修正
- `=`の連打は同じ結果を保つ

## 検証・配布物の再生成

```sh
./gradlew :calculator-core:test :calculator-compose:assembleRelease :sample:assembleDebug
./gradlew :calculator-compose:lintRelease :sample:lintDebug
# エミュレーターまたは実機を接続して実行
./gradlew :calculator-compose:connectedDebugAndroidTest
# 配布用Mavenリポジトリを再生成
./gradlew publishAllPublicationsToDistributionRepository
# 必要なら開発環境のmavenLocalへ公開
./gradlew publishToMavenLocal
```

Android Studioの`CalculatorPreview.kt`から、ライト・ダーク・日本語/大きい文字/エラーのPreviewを確認できます。実行済みの検証結果と画像は`VALIDATION.md`を参照してください。

設計の参考: [Composeの状態保存](https://developer.android.com/develop/ui/compose/state-saving)、[状態のホイスティング](https://developer.android.com/develop/ui/compose/state-hoisting)、[Compose Compiler設定](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)、[Compose BOM](https://developer.android.com/develop/ui/compose/bom)。

## ライセンス

MIT。`LICENSE`を参照してください。
