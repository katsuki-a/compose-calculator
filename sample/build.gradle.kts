plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
android {
    namespace = "dev.calculator.sample"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.calculator.sample"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":calculator-compose"))
    implementation(libs.activity.compose)
}
