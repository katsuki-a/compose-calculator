# 実行済みの検証

検証日: 2026-10-15（日本時間）。検証環境: macOS / arm64、Android Studio Rabbit 1、Gradle 9.6.0、Gradle実行JDKはAndroid Studio付属JBR。Kotlin/JVMのtoolchainは17。

| 検証 | 結果 |
|---|---|
| `:calculator-core:test` | 49件成功、失敗・スキップ0件（Engine 23件 / Reducer 26件） |
| `:calculator-compose:connectedDebugAndroidTest` | 6件成功、失敗・スキップ0件。Resizable / Android 16・API 36 / arm64エミュレーター |
| `:calculator-compose:assembleRelease` | AAR生成成功 |
| `:sample:assembleDebug` | APK生成成功。エミュレーターにインストール・起動し、画面表示を確認 |
| `:calculator-compose:lintRelease` | 警告・エラー0件 |
| `:sample:lintDebug` | 警告・エラー0件 |
| `publishAllPublicationsToDistributionRepository` | AAR / JAR / sources / POM / Gradle module metadataを生成 |
| 独立したMaven利用プロジェクト | ソースモジュールを参照せず`dev.calculator:calculator-compose:0.1.0`からAPK生成成功 |

## 検証対象

計算テストでは演算優先順位、括弧、左結合、十進小数、循環小数の丸め、符号、パーセントの仕様、不正な式、ゼロ除算、長さ・深さの制限を確認しました。入力テストでは結果からの継続、新規入力、符号切り替え後の数字入力、削除、エラー修正などを確認しました。

UIテストではタップによる計算、結果通知、`=`連打時の重複通知防止、ゼロ除算と復帰、入力・結果・エラーの保存状態復元、ステートレスAPIのイベント通知を確認しました。保存状態復元はComposeの`StateRestorationTester`で検証しています。OSによる実際のプロセス終了を起こす試験は行っていません。

## Previewの目視確認

Android Studioで各`@Preview`を個別に新規描画し、描画エラーがなくPNGが生成されたこと、および文字やキーが欠けていないことを確認しました。

- [ライトテーマ / 360dp / 計算結果](docs/previews/light.png)
- [ダークテーマ / 360dp / 入力中](docs/previews/dark.png)
- [日本語 / 320dp / fontScale 1.5 / ゼロ除算エラー](docs/previews/error.png)
- [最終サンプルAPKの実行画面](docs/previews/sample.png)

Previewは静的な外観の検証です。操作・復元は上記のUIテストで検証しています。API 23やAPI 37の実行環境、実機、TalkBackによる読み上げ、異なるComposeバージョンを採用したホストアプリでの実行は今回の検証範囲に含めていません。

## 配布

ソースZIPには生成途中のbuildディレクトリ、IDE設定、ローカルSDK設定を含めず、ソース・Gradle Wrapper・ドキュメント・配布用Mavenリポジトリを含めています。サンプルAPKはdebug署名の動作確認用です。GitHubへのソース公開とは別に、Maven Centralなどへの公開は行っていません。
